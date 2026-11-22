package com.example.movie.domain.model

import java.time.LocalDate

private val posterMonths = arrayOf(
    "янв.",
    "февр.",
    "мар.",
    "апр.",
    "мая",
    "июн.",
    "июл.",
    "авг.",
    "сен.",
    "окт.",
    "нояб.",
    "дек.",
)

/** По умолчанию год. Полная дата только когда её включили и каталог прислал день. */
internal fun posterDateLabel(year: Int?, releaseDate: String?, full: Boolean): String {
    val plain = year?.takeIf { it > 0 }?.toString().orEmpty()
    if (!full) return plain
    val date = releaseDate?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() } ?: return plain
    return "${date.dayOfMonth} ${posterMonths[date.monthValue - 1]} ${date.year}"
}
