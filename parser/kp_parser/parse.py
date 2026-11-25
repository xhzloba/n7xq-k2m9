"""Разбор HTML списка Кинопоиска.

Хеши в CSS-модулях (`styles_position__nfMVF`) меняются между выкладками.
Держимся за стабильный кусок имени: `styles_position__`, `styles_diff__`,
`styles_boxOfficesLeft__`, `styles_kinopoiskValue__`, `styles_kinopoiskCount__`,
`data-test-id`.
"""

from __future__ import annotations

import json
import re
from decimal import Decimal, ROUND_HALF_UP

_ITEM = "data-test-id=\"movie-list-item\""
_POS = re.compile(r'class="[^"]*styles_position__[^"]*"[^>]*>(\d+)<')
_DIFF = re.compile(r'class="([^"]*styles_diff__[^"]*)"[^>]*>([^<]*)<')
_BOX = re.compile(r'class="[^"]*styles_boxOfficesLeft__[^"]*"[^>]*>([^<]*)<')
_TITLE = re.compile(r'class="[^"]*styles_mainTitle__[^"]*"[^>]*>([^<]+)<')
_ORIG = re.compile(
    r'class="[^"]*desktop-list-main-info_secondaryTitle__[^"]*"[^>]*>([^<]*)<'
)
_SEC = re.compile(
    r'class="[^"]*desktop-list-main-info_secondaryText__[^"]*"[^>]*>(.*?)</span>',
    re.S,
)
_RATING = re.compile(r'class="[^"]*styles_kinopoiskValue__[^"]*"[^>]*>([^<]*)<')
_VOTES = re.compile(r'class="[^"]*styles_kinopoiskCount__[^"]*"[^>]*>([^<]*)<')
_ID = re.compile(r'href="(?:https://www\.kinopoisk\.ru)?/(film|series)/(\d+)/"')
_IMG = re.compile(r"<img\b[^>]*>", re.I)
_SRC = re.compile(r'\bsrc="([^"]+)"')
_SRCSET = re.compile(r'\bsrcset="([^"]+)"')
_YEAR = re.compile(r"\b(?:19|20)\d{2}\b")
_COMMENT = re.compile(r"<!--.*?-->", re.S)
_TAG = re.compile(r"<[^>]+>")
_SIZE = re.compile(r"/\d+x\d+(?=$|\?)")
_LD_JSON = re.compile(
    r'<script[^>]*type="application/ld\+json"[^>]*>(.*?)</script>',
    re.S,
)
_PAGE_RATING = re.compile(
    r'class="[^"]*styles_ratingKpTop__[^"]*"[^>]*>\s*([^<]+?)\s*<'
)
_PAGE_VOTES = re.compile(
    r'class="[^"]*styles_count__[^"]*"[^>]*>\s*([^<]*оценк[^<]*)<'
)


def parse_list_html(html: str) -> list[dict]:
    starts = [m.start() for m in re.finditer(re.escape(_ITEM), html)]
    items: list[dict] = []
    seen: set[int] = set()
    for i, start in enumerate(starts):
        end = starts[i + 1] if i + 1 < len(starts) else start + 12000
        item = _parse_item(html[start:end])
        if item is None or item["kp_id"] in seen:
            continue
        seen.add(item["kp_id"])
        items.append(item)
    return items


def _parse_item(chunk: str) -> dict | None:
    id_match = _ID.search(chunk)
    if id_match is None:
        return None
    kind = id_match.group(1)
    kp_id = int(id_match.group(2))
    title = _clean(_TITLE.search(chunk).group(1)) if _TITLE.search(chunk) else None
    orig_match = _ORIG.search(chunk)
    original = _clean(orig_match.group(1)) if orig_match else None
    if original == "":
        original = None
    sec_match = _SEC.search(chunk)
    secondary = _clean_html(sec_match.group(1)) if sec_match else ""
    year_match = _YEAR.search(secondary)
    year = int(year_match.group(0)) if year_match else None
    pos_match = _POS.search(chunk)
    diff_match = _DIFF.search(chunk)
    position_diff, direction = (None, None)
    if diff_match:
        position_diff, direction = _parse_diff(diff_match.group(1), diff_match.group(2))
    box_match = _BOX.search(chunk)
    box_office = _clean(box_match.group(1)) if box_match else ""
    rating_match = _RATING.search(chunk)
    votes_match = _VOTES.search(chunk)
    return {
        "kp_id": kp_id,
        "position": int(pos_match.group(1)) if pos_match else None,
        "diff": position_diff,
        "diff_kind": direction,
        "box_office": box_office or None,
        "title": title or None,
        "original_title": original,
        "year": year,
        "rating": _parse_rating(rating_match.group(1) if rating_match else None),
        "votes": _parse_votes(votes_match.group(1) if votes_match else None),
        "poster": _poster(chunk),
        "url": f"https://www.kinopoisk.ru/{kind}/{kp_id}/",
    }


def _parse_diff(classes: str, raw: str) -> tuple[int | None, str | None]:
    """Число в плашке всегда без знака. Знак — второй класс на том же span."""
    digits = re.sub(r"\D", "", raw)
    if not digits:
        return None, None
    number = int(digits)
    if "styles_negative__" in classes:
        return number, "negative"
    if "styles_positive__" in classes:
        return number, "positive"
    return number, None


def _parse_rating(raw: str | None) -> float | None:
    if not raw:
        return None
    text = raw.strip().replace(",", ".")
    if not re.fullmatch(r"\d+(?:\.\d+)?", text):
        return None
    return float(text)


def _parse_votes(raw: str | None) -> int | None:
    if not raw:
        return None
    digits = re.sub(r"\D", "", raw)
    return int(digits) if digits else None


def _poster(chunk: str) -> str | None:
    img = _IMG.search(chunk)
    if img is None:
        return None
    tag = img.group(0)
    src = _SRC.search(tag)
    srcset = _SRCSET.search(tag)
    best_scale = -1.0
    best_url = src.group(1) if src else None
    if srcset:
        for part in srcset.group(1).split(","):
            bits = part.strip().split()
            if not bits:
                continue
            scale = 1.0
            if len(bits) > 1 and bits[1].endswith("x"):
                try:
                    scale = float(bits[1][:-1])
                except ValueError:
                    scale = 1.0
            if scale >= best_scale:
                best_scale = scale
                best_url = bits[0]
    if not best_url:
        return None
    if best_url.startswith("//"):
        best_url = "https:" + best_url
    return _SIZE.sub("/600x900", best_url)


def parse_film_page(html: str) -> tuple[float | None, int | None]:
    """Общий рейтинг и все оценки со страницы фильма или сериала.

    Это не цифры Топ-250: ratingValue / ratingCount из разметки страницы.
    """
    rating, votes = _rating_from_ld(html)
    if rating is None:
        rating = _one_decimal(_PAGE_RATING.search(html).group(1) if _PAGE_RATING.search(html) else None)
    if votes is None:
        anchor = html.find("styles_ratingKpTop__")
        window = html[anchor:anchor + 2500] if anchor >= 0 else html
        votes_match = _PAGE_VOTES.search(window) or _PAGE_VOTES.search(html)
        votes = _parse_votes(votes_match.group(1) if votes_match else None)
    return rating, votes


def _rating_from_ld(html: str) -> tuple[float | None, int | None]:
    for match in _LD_JSON.finditer(html):
        try:
            data = json.loads(match.group(1))
        except json.JSONDecodeError:
            continue
        nodes = data if isinstance(data, list) else [data]
        for node in nodes:
            if not isinstance(node, dict):
                continue
            aggregate = node.get("aggregateRating")
            if not isinstance(aggregate, dict):
                continue
            rating = _one_decimal(aggregate.get("ratingValue"))
            votes = _parse_votes(str(aggregate.get("ratingCount"))) if aggregate.get("ratingCount") is not None else None
            if rating is not None or votes is not None:
                return rating, votes
    return None, None


def _one_decimal(value) -> float | None:
    if value is None:
        return None
    text = str(value).strip().replace(",", ".")
    if not text:
        return None
    try:
        number = Decimal(text)
    except Exception:
        return None
    return float(number.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP))


def _clean(text: str) -> str:
    return text.replace("\xa0", " ").replace("&nbsp;", " ").strip()


def _clean_html(text: str) -> str:
    text = _COMMENT.sub("", text)
    text = _TAG.sub("", text)
    return _clean(re.sub(r"\s+", " ", text))
