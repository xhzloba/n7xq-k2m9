"""Локальное окно: свой HTTP на 127.0.0.1 и Chrome в режиме приложения."""

from __future__ import annotations

import json
import subprocess
import threading
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from kp_parser.scrape import CATEGORIES, FIELDS, category_by_id, scrape

ROOT = Path(__file__).resolve().parent.parent
UI = Path(__file__).resolve().parent / "ui.html"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

_lock = threading.Lock()
_job = {
    "running": False,
    "done": False,
    "error": None,
    "log": [],
    "items": [],
    "path": None,
    "target": 50,
    "t0": None,
    "elapsed": None,
    "step": 0,
    "steps": 1,
    "active": "",
    "active_id": "",
    "lists": {},
    "restored": False,
    "revision": 0,
    "details": False,
    "cancel": False,
}
_last_beat = time.monotonic()


def _snapshot(view: str | None = None, since: int | None = None, rev: int | None = None) -> dict:
    with _lock:
        lists: dict = _job["lists"]
        active_id = _job["active_id"]
        explicit = view if view and view in lists else None
        if explicit:
            chosen = explicit
        elif active_id in lists:
            chosen = active_id
        elif lists:
            chosen = next(reversed(lists))
        else:
            chosen = ""
        bucket = lists.get(chosen)
        full = bucket["items"] if bucket else []
        unchanged = (
            explicit is not None
            and since is not None
            and since == len(full)
            and rev == _job["revision"]
        )
        return {
            "running": _job["running"],
            "done": _job["done"],
            "error": _job["error"],
            "log": list(_job["log"]),
            "items": None if unchanged else list(full),
            "count": len(full),
            "path": bucket.get("path") if bucket else _job["path"],
            "target": bucket["target"] if bucket else _job["target"],
            "elapsed": _elapsed_now(),
            "step": _job["step"],
            "steps": _job["steps"],
            "active": _job["active"],
            "active_id": active_id,
            "view": chosen,
            "restored": _job["restored"],
            "revision": _job["revision"],
            "views": [
                {
                    "id": item["id"],
                    "title": item["title"],
                    "count": len(item["items"]),
                    "target": item["target"],
                }
                for item in lists.values()
            ],
        }


def _elapsed_now() -> float | None:
    if _job["running"] and _job["t0"] is not None:
        return time.monotonic() - _job["t0"]
    return _job["elapsed"]


def _fmt_elapsed(seconds: float) -> str:
    if seconds < 60:
        return f"{seconds:.1f} с"
    minutes, rest = divmod(int(round(seconds)), 60)
    return f"{minutes}:{rest:02d}"


def _stopped() -> bool:
    with _lock:
        return bool(_job["cancel"])


def _log(message: str) -> None:
    print(message, flush=True)
    with _lock:
        _job["log"].append(message)


def _set_items(items: list[dict]) -> None:
    with _lock:
        _job["items"] = items
        _job["revision"] += 1
        bucket = _job["lists"].get(_job["active_id"])
        if bucket is not None:
            bucket["items"] = items


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        return

    def do_GET(self) -> None:
        path = self.path.split("?", 1)[0]
        if path in ("/", "/index.html"):
            body = UI.read_bytes()
            self._send(200, "text/html; charset=utf-8", body)
            return
        if path == "/api/meta":
            payload = {
                "categories": [category.as_dict() for category in CATEGORIES],
                "fields": FIELDS,
            }
            self._send(200, "application/json", _dump(payload))
            return
        if path == "/api/status":
            view, since, rev = _status_query(self.path)
            self._send(200, "application/json", _dump(_snapshot(view, since, rev)))
            return
        self._send(404, "text/plain", b"not found")

    def do_POST(self) -> None:
        global _last_beat
        if self.path == "/api/heartbeat":
            _last_beat = time.monotonic()
            self._send(204, "text/plain", b"")
            return
        if self.path == "/api/stop":
            with _lock:
                if _job["running"]:
                    _job["cancel"] = True
            self._send(204, "text/plain", b"")
            return
        if self.path == "/api/reveal":
            spec = _read_json(self) if int(self.headers.get("Content-Length", "0") or 0) else {}
            view = str(spec.get("id") or "")
            with _lock:
                bucket = _job["lists"].get(view)
                path = (bucket or {}).get("path") or _job["path"]
            if path:
                subprocess.Popen(["open", "-R", path])
            self._send(204, "text/plain", b"")
            return
        if self.path != "/api/run":
            self._send(404, "text/plain", b"not found")
            return
        try:
            spec = _read_json(self)
            _start(spec)
        except Exception as error:
            self._send(400, "application/json", _dump({"error": str(error)}))
            return
        self._send(200, "application/json", _dump({"ok": True}))

    def _send(self, code: int, content_type: str, body: bytes) -> None:
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if body:
            self.wfile.write(body)


def _read_json(handler: BaseHTTPRequestHandler) -> dict:
    length = int(handler.headers.get("Content-Length", "0"))
    if length > 100_000:
        raise ValueError("Слишком большой запрос")
    raw = handler.rfile.read(length) if length else b"{}"
    data = json.loads(raw.decode())
    if not isinstance(data, dict):
        raise ValueError("Жду объект")
    return data


def _categories_from(spec: dict) -> list:
    raw = spec.get("categories")
    if not raw:
        single = spec.get("category")
        raw = [single] if single else []
    if isinstance(raw, str):
        raw = [raw]
    if not isinstance(raw, list) or not raw:
        raise ValueError("Выбери хотя бы один список")
    picked = []
    seen: set[str] = set()
    for item in raw:
        try:
            category = category_by_id(str(item))
        except KeyError as error:
            raise ValueError("Нет такой категории") from error
        if not category.enabled:
            raise RuntimeError(f"Категория «{category.title}» ещё не подключена")
        if category.id in seen:
            continue
        seen.add(category.id)
        picked.append(category)
    return picked


def _start(spec: dict) -> None:
    categories = _categories_from(spec)
    if any(category.fixed is None for category in categories):
        raw_limit = spec.get("limit", 50)
        if raw_limit in (None, ""):
            raw_limit = 50
        try:
            limit = int(raw_limit)
        except (TypeError, ValueError) as error:
            raise ValueError("Количество от 1 до 1000") from error
        if limit < 1 or limit > 1000:
            raise ValueError("Количество от 1 до 1000")
    else:
        limit = categories[0].fixed or 50
    fields = spec.get("fields") or [key for key, _label in FIELDS]
    if not isinstance(fields, list) or not all(isinstance(item, str) for item in fields):
        raise ValueError("Поля — список")
    known = {key for key, _label in FIELDS}
    fields = [item for item in fields if item not in {"on", "kp_id"}]
    unknown = [item for item in fields if item not in known]
    if unknown:
        raise ValueError("Неизвестные поля: " + ", ".join(unknown))
    details = bool(spec.get("details"))
    custom = str(spec.get("out") or "").strip()
    outs = {
        category.id: _output_path(custom or f"output/{category.id}.json")
        if len(categories) == 1
        else _output_path(f"output/{category.id}.json")
        for category in categories
    }
    with _lock:
        if _job["running"]:
            raise RuntimeError("Парс уже идёт")
        lists = {
            category.id: {
                "id": category.id,
                "title": category.title,
                "items": [],
                "target": category.fixed or limit,
                "path": None,
            }
            for category in categories
        }
        _job.update(
            running=True,
            done=False,
            error=None,
            log=[],
            items=[],
            path=None,
            target=lists[categories[0].id]["target"],
            t0=time.monotonic(),
            elapsed=None,
            step=0,
            steps=len(categories),
            active=categories[0].title,
            active_id=categories[0].id,
            lists=lists,
            restored=False,
            revision=0,
            details=details,
            cancel=False,
        )
    threading.Thread(
        target=_run,
        args=(categories, limit, list(fields), outs, details),
        daemon=True,
    ).start()


def _run(categories, limit: int, fields: list[str], outs: dict[str, Path], details: bool) -> None:
    try:
        last_items: list[dict] = []
        last_path: Path | None = None
        stopped = False
        for index, category in enumerate(categories):
            if _stopped():
                stopped = True
                break
            out = outs[category.id]
            take = category.fixed or limit
            with _lock:
                _job["step"] = index
                _job["active"] = category.title
                _job["active_id"] = category.id
                _job["target"] = take
                _job["items"] = []
                bucket = _job["lists"].get(category.id)
                if bucket is not None:
                    bucket["items"] = []
            _log(f"Список · {category.title} · {take}")
            items = scrape(category, take, _log, _set_items, details=details, stop=_stopped)
            write_result(out, category, items, fields, details)
            if _stopped():
                stopped = True
            last_items = items
            last_path = out
            with _lock:
                _job["path"] = str(out)
                _job["items"] = items
                bucket = _job["lists"].get(category.id)
                if bucket is not None:
                    bucket["items"] = items
                    bucket["path"] = str(out)
            _log(f"{category.title} → {out} · {len(items)}")
            if stopped:
                break
        with _lock:
            took = time.monotonic() - _job["t0"]
            _job["items"] = last_items
            _job["path"] = str(last_path) if last_path else None
            _job["done"] = True
            _job["elapsed"] = took
        if stopped:
            _log("Остановлено")
        else:
            _log(f"Готово за {_fmt_elapsed(took)}")
    except Exception as error:
        with _lock:
            took = None if _job["t0"] is None else time.monotonic() - _job["t0"]
            _job["error"] = str(error)
            _job["done"] = True
            _job["elapsed"] = took
        _log(f"Ошибка: {error}")
    finally:
        with _lock:
            _job["running"] = False


def write_result(out: Path, category, items: list[dict], fields: list[str], details: bool = False) -> None:
    payload = {
        "category": category.id,
        "category_title": category.title,
        "source": category.url,
        "parsed_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "count": len(items),
        "details": details,
        "items": [_project(item, fields, details) for item in items],
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    tmp = out.with_suffix(".json.tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(out)


def _project(item: dict, fields: list[str], details: bool = False) -> dict:
    picked = {"kp_id": item["kp_id"]}
    for key in fields:
        if key == "diff":
            picked["diff"] = item["diff"]
            picked["diff_kind"] = item["diff_kind"]
            continue
        if key in item:
            picked[key] = item[key]
    if details:
        picked["page_rating"] = item.get("page_rating")
        picked["page_votes"] = item.get("page_votes")
    return picked


def _output_path(raw: str) -> Path:
    path = Path(raw).expanduser()
    if not path.is_absolute():
        path = ROOT / path
    path = path.resolve()
    if path.suffix.lower() != ".json":
        raise ValueError("Файл должен заканчиваться на .json")
    home = Path.home().resolve()
    if path != home and home not in path.parents:
        raise ValueError("Путь вне домашней папки")
    return path


def _dump(payload: dict) -> bytes:
    return json.dumps(payload, ensure_ascii=False).encode()


def _status_query(raw: str) -> tuple[str | None, int | None, int | None]:
    query = parse_qs(urlsplit(raw).query)
    view = (query.get("view") or [None])[0]
    raw_since = (query.get("since") or [None])[0]
    raw_rev = (query.get("rev") or [None])[0]
    since = int(raw_since) if isinstance(raw_since, str) and raw_since.isdigit() else None
    rev = int(raw_rev) if isinstance(raw_rev, str) and raw_rev.isdigit() else None
    return (str(view) if view else None), since, rev


def _load_saved() -> None:
    """Поднимает уже записанные output/<id>.json, чтобы листать их без нового парса."""
    lists: dict = {}
    last_path = None
    for category in CATEGORIES:
        path = ROOT / "output" / f"{category.id}.json"
        if not path.is_file():
            continue
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        items = payload.get("items")
        if not isinstance(items, list):
            continue
        lists[category.id] = {
            "id": category.id,
            "title": payload.get("category_title") or category.title,
            "items": items,
            "target": len(items) or (category.fixed or 0),
            "path": str(path.resolve()),
        }
        last_path = str(path.resolve())
    if not lists:
        return
    with _lock:
        if _job["running"]:
            return
        active_id = next(reversed(lists))
        _job["lists"] = lists
        _job["active_id"] = active_id
        _job["active"] = lists[active_id]["title"]
        _job["items"] = lists[active_id]["items"]
        _job["target"] = lists[active_id]["target"]
        _job["path"] = last_path
        _job["steps"] = len(lists)
        _job["step"] = len(lists) - 1
        _job["restored"] = True


def serve() -> None:
    _load_saved()
    if not Path(CHROME).exists():
        raise SystemExit(f"Нет Chrome: {CHROME}")
    ThreadingHTTPServer.allow_reuse_address = True
    try:
        httpd = ThreadingHTTPServer(("127.0.0.1", 8765), Handler)
    except OSError as error:
        raise SystemExit(f"Порт 8765 занят, закрой старое окно парсера. {error}") from error
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    url = "http://127.0.0.1:8765/"
    profile = ROOT / ".chrome-profile"
    profile.mkdir(exist_ok=True)
    _drop_stale_session(profile)
    proc = subprocess.Popen(
        [
            CHROME,
            f"--app={url}",
            f"--user-data-dir={profile}",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-session-crashed-bubble",
            "--disable-extensions",
            "--window-size=1320,900",
        ],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    print(f"Окно парсера: {url}", flush=True)
    try:
        while True:
            time.sleep(0.5)
            if proc.poll() is not None and not _chrome_with_profile(profile):
                break
    except KeyboardInterrupt:
        proc.terminate()
    finally:
        httpd.shutdown()


def _drop_stale_session(profile: Path) -> None:
    """Профиль помнит прошлый порт. Без этого Chrome открывает мёртвый адрес."""
    default = profile / "Default"
    for name in ("Last Session", "Current Session", "Last Tabs", "Current Tabs"):
        file = default / name
        if file.exists():
            file.unlink()
    sessions = default / "Sessions"
    if sessions.is_dir():
        for child in sessions.iterdir():
            if child.is_file():
                child.unlink()


def _chrome_with_profile(profile: Path) -> bool:
    needle = f"--user-data-dir={profile}"
    try:
        listing = subprocess.run(
            ["ps", "-ax", "-o", "command="],
            capture_output=True,
            text=True,
            check=False,
        ).stdout
    except OSError:
        return False
    return any(
        needle in line and "Google Chrome.app" in line and "Helper" not in line
        for line in listing.splitlines()
    )
