"""Окно парсера. `python -m kp_parser --cli` пишет JSON без окна."""

from __future__ import annotations

import argparse

from kp_parser.scrape import FIELDS, category_by_id, scrape
from kp_parser.server import serve, write_result, _output_path


def main() -> None:
    parser = argparse.ArgumentParser(description="Парсер списков Кинопоиска")
    parser.add_argument("--cli", action="store_true", help="без окна, сразу в JSON")
    parser.add_argument("--category", default="popular-films")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--out", default="")
    parser.add_argument("--details", action="store_true", help="общий рейтинг и все оценки со страницы")
    args = parser.parse_args()
    if not args.cli:
        serve()
        return
    category = category_by_id(args.category)
    if args.limit < 1 or args.limit > 1000:
        raise SystemExit("Количество от 1 до 1000")
    out = _output_path(args.out or f"output/{category.id}.json")
    items = scrape(category, category.fixed or args.limit, print, details=args.details)
    write_result(out, category, items, [key for key, _label in FIELDS], args.details)
    print(f"Записал {len(items)} в {out}")


if __name__ == "__main__":
    main()
