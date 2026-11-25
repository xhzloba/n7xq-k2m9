"""Снятие страниц списка. Сам разбор — в parse.py."""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass

from kp_parser.parse import parse_film_page, parse_list_html

Log = Callable[[str], None]
OnItems = Callable[[list[dict]], None]

PAGE_SIZE = 50
MAX_PAGES = 20


@dataclass(frozen=True)
class Category:
    id: str
    title: str
    kind: str
    url: str | None
    enabled: bool
    note: str = ""
    fixed: int | None = None

    def as_dict(self) -> dict:
        return {
            "id": self.id,
            "title": self.title,
            "kind": self.kind,
            "url": self.url,
            "enabled": self.enabled,
            "note": self.note,
            "fixed": self.fixed,
        }


CATEGORIES: tuple[Category, ...] = (
    Category(
        id="popular-films",
        title="Популярные фильмы",
        kind="movie",
        url="https://www.kinopoisk.ru/lists/movies/popular-films/",
        enabled=True,
    ),
    Category(
        id="popular-series",
        title="Популярные сериалы",
        kind="series",
        url="https://www.kinopoisk.ru/lists/movies/popular-series/",
        enabled=True,
    ),
    Category(
        id="top500",
        title="Топ 500",
        kind="movie",
        url="https://www.kinopoisk.ru/lists/movies/top500/",
        enabled=True,
        fixed=500,
    ),
    Category(
        id="series-top250",
        title="Топ 250 сериалов",
        kind="series",
        url="https://www.kinopoisk.ru/lists/movies/series-top250/",
        enabled=True,
        fixed=250,
    ),
    Category(
        id="box-usa-all-time",
        title="США: самые кассовые",
        kind="movie",
        url="https://www.kinopoisk.ru/lists/movies/box-usa-all-time/",
        enabled=True,
        fixed=881,
    ),
)

FIELDS: tuple[tuple[str, str], ...] = (
    ("title", "Название"),
    ("original_title", "Оригинальное название"),
    ("year", "Год"),
    ("rating", "Рейтинг"),
    ("votes", "Голоса"),
    ("poster", "Постер"),
    ("url", "Ссылка"),
    ("position", "Позиция"),
    ("diff", "Сдвиг"),
    ("box_office", "Сборы"),
)


def category_by_id(category_id: str) -> Category:
    for category in CATEGORIES:
        if category.id == category_id:
            return category
    raise KeyError(category_id)


Stop = Callable[[], bool]


def scrape(
    category: Category,
    limit: int,
    log: Log,
    on_items: OnItems | None = None,
    *,
    details: bool = False,
    stop: Stop | None = None,
) -> list[dict]:
    if not category.enabled or not category.url:
        raise RuntimeError(f"Категория «{category.title}» ещё не подключена")
    limit = max(1, min(int(limit), PAGE_SIZE * MAX_PAGES))
    try:
        from playwright.sync_api import sync_playwright
    except ImportError as error:
        raise RuntimeError(
            "Нет playwright. Запусти ./run.sh — он ставит зависимости."
        ) from error

    collected: list[dict] = []
    seen: set[int] = set()
    pages = min(MAX_PAGES, (limit + PAGE_SIZE - 1) // PAGE_SIZE)

    log("Открываю список")
    with sync_playwright() as playwright:
        browser = _launch(playwright)
        try:
            context = browser.new_context(
                locale="ru-RU",
                viewport={"width": 1440, "height": 900},
            )
            page = context.new_page()
            page.route("**/*", _drop_heavy)
            for page_no in range(1, pages + 1):
                if _halted(stop):
                    break
                url = _page_url(category.url, page_no)
                log(f"Страница {page_no}")
                html = _load(page, url)
                found = parse_list_html(html)
                fresh = [item for item in found if item["kp_id"] not in seen]
                for item in fresh:
                    seen.add(item["kp_id"])
                collected.extend(fresh)
                positive = sum(1 for item in fresh if item["diff_kind"] == "positive")
                negative = sum(1 for item in fresh if item["diff_kind"] == "negative")
                boxed = sum(1 for item in fresh if item.get("box_office"))
                if boxed:
                    log(f"+{len(fresh)} · всего {len(collected)} · сборы {boxed}")
                else:
                    log(f"+{len(fresh)} · всего {len(collected)} · ↑ {positive} · ↓ {negative}")
                if on_items:
                    on_items(collected[:limit])
                if len(collected) >= limit or not fresh:
                    break
            collected = collected[:limit]
            if details and collected and not _halted(stop):
                _fill_details(page, collected, log, on_items, stop)
        finally:
            browser.close()

    if not collected and not _halted(stop):
        raise RuntimeError(
            "Список пустой. Кинопоиск отдал капчу или не прошёл SSO. Запусти ещё раз."
        )
    return collected[:limit]


def _halted(stop: Stop | None) -> bool:
    return bool(stop and stop())


def _fill_details(page, items: list[dict], log: Log, on_items: OnItems | None, stop: Stop | None) -> None:
    log(f"Деталка · {len(items)} страниц")
    misses = 0
    for index, item in enumerate(items, start=1):
        if _halted(stop):
            if on_items:
                on_items(items)
            return
        title = item.get("title") or item.get("kp_id")
        url = item.get("url")
        rating = votes = None
        if url:
            try:
                html = _load_detail(page, url)
                rating, votes = parse_film_page(html)
            except Exception as error:
                log(f"Деталка {index}/{len(items)} · {title} · {error}")
        item["page_rating"] = rating
        item["page_votes"] = votes
        if rating is None and votes is None:
            misses += 1
            if misses >= 3:
                log("Деталка остановлена: три страницы подряд без рейтинга")
                if on_items:
                    on_items(items)
                return
        else:
            misses = 0
            rate = f"{rating:.1f}" if rating is not None else "—"
            count = f"{votes:,}".replace(",", " ") if votes is not None else "—"
            log(f"Деталка {index}/{len(items)} · {title} · {rate} · {count}")
        if on_items:
            on_items(items)


def _load_detail(page, url: str) -> str:
    last: Exception | None = None
    for attempt in (1, 2):
        try:
            if attempt == 1:
                page.goto(url, wait_until="domcontentloaded", timeout=30000)
            else:
                page.reload(wait_until="domcontentloaded", timeout=30000)
            page.wait_for_selector(
                'script[type="application/ld+json"]',
                state="attached",
                timeout=15000,
            )
            return page.content()
        except Exception as error:
            last = error
    reason = "" if last is None else str(last).splitlines()[0][:160]
    raise RuntimeError("Страница не открылась" + (f": {reason}" if reason else "")) from last


def _launch(playwright):
    try:
        return playwright.chromium.launch(channel="chrome", headless=True)
    except Exception:
        return playwright.chromium.launch(headless=True)


def _drop_heavy(route) -> None:
    if route.request.resource_type in {"image", "media", "font"}:
        route.abort()
        return
    route.continue_()


def _page_url(base: str, page_no: int) -> str:
    if page_no <= 1:
        return base
    join = "&" if "?" in base else "?"
    return f"{base}{join}page={page_no}"


def _load(page, url: str) -> str:
    last: Exception | None = None
    for attempt in (1, 2):
        try:
            if attempt == 1:
                page.goto(url, wait_until="domcontentloaded", timeout=45000)
            else:
                page.reload(wait_until="domcontentloaded", timeout=45000)
            page.wait_for_selector('[data-test-id="movie-list-item"]', timeout=20000)
            return page.content()
        except Exception as error:
            last = error
    raise RuntimeError(
        "Карточки списка не появились. Похоже на капчу или обрыв SSO."
    ) from last
