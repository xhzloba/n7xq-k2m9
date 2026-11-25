# Парсер Кинопоиска

Окно, в котором выбираешь категорию, сколько карточек снять и какие поля писать в JSON.

Категории:

- **Популярные фильмы** — до 1000, число задаёшь сам
- **Популярные сериалы** — до 1000, число задаёшь сам
- **Топ 500** — всегда 500, `https://www.kinopoisk.ru/lists/movies/top500/`
- **Топ 250 сериалов** — всегда 250, `https://www.kinopoisk.ru/lists/movies/series-top250/`
- **США: самые кассовые** — всегда 881, это весь список, `https://www.kinopoisk.ru/lists/movies/box-usa-all-time/`. Позиции и сдвига нет. Вместо них сборы США, как на карточке: `$959.1 млн` в поле `box_office`. Это касса, не бюджет производства.

## Почему так

Страница списка — обычный HTML, 50 фильмов на страницу. Перед ним Яндекс SSO на JavaScript: `requests` / `curl` получают 302 и пустое тело. Поэтому страницу открывает headless Chrome (Playwright), а карточки разбираются локально, без браузера.

Хеши в классах (`styles_position__nfMVF`) Кинопоиск меняет. Парсер держится за стабильные куски: `data-test-id="movie-list-item"`, `styles_position__`, `styles_diff__`, `styles_positive__` / `styles_negative__`, `styles_kinopoiskValue__`, `styles_kinopoiskCount__`, ссылку `/film/<kp_id>/` или `/series/<kp_id>/`.

Нужен установленный Google Chrome.

## Запуск

Из папки `parser`:

```bash
./run.sh
```

Откроется отдельное окно (Chrome без вкладок). Первый запуск сам создаст `.venv` и поставит Playwright.

В окне:

1. Списки — можно отметить оба. Идут по очереди: сначала фильмы, потом сериалы. У каждого свой JSON.
2. Сколько — по умолчанию 50. На странице ровно 50, больше догрузит `?page=2` и дальше, потолок 1000.
3. Галочки — что попадёт в JSON. `kp_id` пишется всегда.
4. Файл — куда сохранить. По умолчанию `output/popular-films.json`.
5. «Парсить». Таблица заполняется по ходу. «Показать файл» открывает JSON в Finder.

Окно закрыл — процесс завершается сам.

Без окна, сразу в файл:

```bash
./run.sh --cli --limit 50
./run.sh --cli --limit 100 --out output/popular-films.json
```

## JSON

```json
{
  "category": "popular-films",
  "category_title": "Популярные фильмы",
  "source": "https://www.kinopoisk.ru/lists/movies/popular-films/",
  "parsed_at": "2026-11-25T01:50:00+04:00",
  "count": 50,
  "items": [
    {
      "kp_id": 5927239,
      "title": "Голодные игры: Рассвет Жатвы",
      "original_title": "The Hunger Games: Sunrise on the Reaping",
      "year": 2026,
      "rating": 7.2,
      "votes": 1646,
      "poster": "https://cdn.../600x900",
      "url": "https://www.kinopoisk.ru/film/5927239/",
      "position": 1,
      "diff": 1,
      "diff_kind": "positive"
    }
  ]
}
```

`position` — место в списке. Число в плашке сдвига всегда без знака, направление задаёт класс на том же элементе: `styles_positive__` → `diff_kind: "positive"` (в таблице «позитивная»), `styles_negative__` → `diff_kind: "negative"` («негативная»). `diff` — само число. Если плашки нет, оба поля `null`. На кассовом списке места и сдвига нет, вместо них `box_office` — строка сборов слева на карточке (`styles_boxOfficesLeft__`), например `$959.1 млн`. Если рейтинга ещё нет (вместо него «добавили в Буду смотреть»), `rating` и `votes` тоже `null`. Постер — тот же кадр, что на списке, размер в URL подменён на `600x900` (`360x540` этот CDN не отдаёт).
