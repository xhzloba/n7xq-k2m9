package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val extractBackdrop: Boolean = false,
    val extractPlex: Boolean = true,
    val plexBackdrop: Boolean = true,
    /** UltraBlur на главной с обложки слайдера. По дефолту вкл (полки скрыты — только слайдер). */
    val heroPosterColors: Boolean = true,
    /** true = UltraBlur на всю главную; false = только зона слайдера. */
    val heroPosterPage: Boolean = true,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val watchGlow: Boolean = false,
    val haptic: Boolean = true,
    val showPosterTitles: Boolean = true,
    val largePosters: Boolean = false,
    /** full — полный постер, light — под ячейку, low — маленький файл. */
    val posterQuality: String = PosterQualityFull,
    /** Сколько обложек качать сразу с одного адреса. */
    val imageParallel: Int = DefaultImageParallel,
    val playerLogo: Boolean = true,
    val playerSpeed: Float = 1f,
    val playerPicture: String = "fit",
    val playerDelayUs: Long = 0L,
    val playerBrightness: Float? = null,
    val playerUnderCamera: Boolean = false,
    val playerAutoNext: Boolean = true,
    val playerHdr: Boolean = true,
    /** Потолок кодека: auto, av1, vp9, h264. По умолчанию авто, как у Lift. */
    val playerCodec: String = "auto",
    val watchedBadge: Boolean = true,
    val whiteTitles: Boolean = true,
    /**
     * Нижнее меню при скролле: off — всегда видно, scroll — прячется везде,
     * auto — только полный список жанра и список со слайдера. По умолчанию auto.
     */
    val barScroll: String = BarScrollAuto,
    /** Подписи под иконками нижнего меню. */
    val navLabels: Boolean = false,
    /** Ретушь по бокам полок (слайдеров) на главной и в каталоге. */
    val sliderShade: Boolean = false,
    /** Переход в деталку: экран закрывается цветом темы из углов. */
    val detailIris: Boolean = false,
    /** Смена кадров в деталке: растворение вместо сдвига. По умолчанию сдвиг. */
    val backdropDissolve: Boolean = false,
    /** Скрытые полки главной (id из HomeRails). Главный слайдер всегда. По умолчанию все скрыты. */
    val hiddenHomeRails: Set<String> = DefaultHiddenHomeRails,
    /** Порядок полок сверху вниз. Первая — сразу под слайдером. */
    val homeRailOrder: List<String> = DefaultHomeRailOrder,
    /** Ссылка «Зачем это?» под плашкой «Вид полок». */
    val hideRailsLookHint: Boolean = false,
    val appTheme: String = "cinema",
    /** Доля длины, после которой фильм или серия уходит в «Просмотренное». */
    val watchedPercent: Int = DefaultWatchedPercent,
    /** true — прогресс с первой секунды, false — с [DefaultProgressStartMs]. */
    val countFromStart: Boolean = false,
    /** Подсказка про колокольчик на фильме в TS уже показана. */
    val qualityHintSeen: Boolean = false,
    /** Потолок ожидания карточки, секунды. Быстрый ответ не ждёт этот срок. */
    val networkTimeoutSec: Int = DefaultNetworkTimeoutSec,
    /** Отклик по действиям: poster=tick,watch=click,… */
    val hapticProfile: String = DefaultHapticProfile,
    /** Шапка при прокрутке: сплошная заливка темы вместо растушёвки. */
    val headerFill: Boolean = false,
    /** Подсказки на экранах. По умолчанию включены и гаснут после первого показа. */
    val hints: Boolean = true,
    /** Подсказка про долгое нажатие на «Главную» уже пройдена. */
    val homeHintSeen: Boolean = false,
) {
    val progressStartMs: Long
        get() = if (countFromStart) 0L else DefaultProgressStartMs

    fun playerCodecCeiling(): String = playerCodec.normalizedPlayerCodec()

    fun homeRailVisible(id: String): Boolean = id !in hiddenHomeRails
}

/** Совпадает с HomeRails.All — по дефолту на главной только слайдер. */
fun String.normalizedPlayerCodec(): String = when (this) {
    "av1", "vp9", "h264" -> this
    else -> "auto"
}

/** Порядок, который уже мог сохраниться: жанры, потом свои. */
val PreviousHomeRailOrder: List<String> = GenreRails.All.map { it.id } + listOf(
    "recommendations",
    "history",
    "continue",
    "favorites",
    "watched",
    "watching",
    "best",
)

val DefaultHomeRailOrder: List<String> = listOf(
    "history",
    "favorites",
    "watched",
    "continue",
    "watching",
    "best",
    "recommendations",
) + GenreRails.All.map { it.id }

val DefaultHiddenHomeRails: Set<String> = DefaultHomeRailOrder.toSet()

fun List<String>.normalizedHomeRailOrder(): List<String> {
    val kept = map { it.trim() }.filter { it in DefaultHomeRailOrder }.distinct()
    return kept + DefaultHomeRailOrder.filter { it !in kept }
}

fun List<String>.placedAfter(anchor: String, ids: List<String>): List<String> {
    val order = toMutableList()
    ids.forEach { order.remove(it) }
    val at = order.indexOf(anchor).let { if (it >= 0) it + 1 else order.size }
    order.addAll(at, ids)
    return order
}

fun List<String>.moveHomeRail(id: String, up: Boolean): List<String> {
    val order = normalizedHomeRailOrder().toMutableList()
    val from = order.indexOf(id)
    if (from < 0) return order
    val to = if (up) from - 1 else from + 1
    if (to !in order.indices) return order
    order.add(to, order.removeAt(from))
    return order
}

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractBackdrop(enabled: Boolean)

    fun setExtractPlex(enabled: Boolean)

    fun setPlexBackdrop(enabled: Boolean)

    fun setHeroPosterColors(enabled: Boolean)

    fun setHeroPosterPage(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setWatchGlow(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)

    fun setLargePosters(enabled: Boolean)

    fun setPosterQuality(quality: String)

    fun setImageParallel(count: Int)

    fun setPlayerLogo(enabled: Boolean)

    fun setPlayerSpeed(speed: Float)

    fun setPlayerPicture(mode: String)

    fun setPlayerDelayUs(delayUs: Long)

    fun setPlayerBrightness(brightness: Float?)

    fun setPlayerUnderCamera(enabled: Boolean)

    fun setPlayerAutoNext(enabled: Boolean)

    fun setPlayerHdr(enabled: Boolean)

    fun setPlayerCodec(codec: String)

    fun setWatchedBadge(enabled: Boolean)

    fun setWhiteTitles(enabled: Boolean)

    fun setBarScroll(mode: String)

    fun setNavLabels(enabled: Boolean)

    fun setSliderShade(enabled: Boolean)

    fun setDetailIris(enabled: Boolean)

    fun setBackdropDissolve(enabled: Boolean)

    fun setHomeRailVisible(id: String, visible: Boolean)

    fun setHomeRailOrder(order: List<String>)

    fun setHideRailsLookHint(hidden: Boolean)

    fun setAppTheme(theme: String)

    fun setWatchedPercent(percent: Int)

    fun setCountFromStart(enabled: Boolean)

    fun setQualityHintSeen(seen: Boolean)

    fun setNetworkTimeoutSec(seconds: Int)

    fun setHapticFeel(slot: String, feel: String)

    fun setHeaderFill(enabled: Boolean)

    fun setHints(enabled: Boolean)

    fun setHomeHintSeen(seen: Boolean)

    fun replace(settings: UserSettings)
}

const val DefaultNetworkTimeoutSec = 4
const val MinNetworkTimeoutSec = 4
const val MaxNetworkTimeoutSec = 20

fun Int.normalizedNetworkTimeout(): Int = coerceIn(MinNetworkTimeoutSec, MaxNetworkTimeoutSec)

const val PosterQualityFull = "full"
const val PosterQualityLight = "light"
const val PosterQualityLow = "low"

fun String.normalizedPosterQuality(): String = when (this) {
    PosterQualityLight, PosterQualityLow -> this
    else -> PosterQualityFull
}

const val DefaultImageParallel = 16

val ImageParallelChoices: List<Int> = listOf(8, 16, 24, 32, 48)

fun Int.normalizedImageParallel(): Int = if (this in ImageParallelChoices) this else DefaultImageParallel

const val BarScrollOff = "off"
const val BarScrollScroll = "scroll"
const val BarScrollAuto = "auto"

fun String.normalizedBarScroll(): String = when (this) {
    BarScrollOff, BarScrollScroll, BarScrollAuto -> this
    else -> BarScrollAuto
}

/** Скролл прячет меню на главной, в каталоге и в полном списке. Авто сюда не входит. */
fun UserSettings.hidesBar(): Boolean = barScroll.normalizedBarScroll() == BarScrollScroll

/** Авто: полный список жанра или список со слайдера открывается уже без меню. */
fun UserSettings.pinsBarAway(fullList: Boolean): Boolean =
    fullList && barScroll.normalizedBarScroll() == BarScrollAuto
