package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Полки «Моё». Все шесть всегда на экране, меняется только порядок. */
object LibraryShelves {
    const val Favorites = "favorites"
    const val Continue = "continue"
    const val History = "history"
    const val Watched = "watched"
    const val Downloads = "downloads"
    const val Folders = "folders"
}

val DefaultLibraryOrder: List<String> = listOf(
    LibraryShelves.Favorites,
    LibraryShelves.Continue,
    LibraryShelves.History,
    LibraryShelves.Watched,
    LibraryShelves.Downloads,
    LibraryShelves.Folders,
)

@Serializable
data class UserSettings(
    val extractPlex: Boolean = true,
    val plexBackdrop: Boolean = true,
    /** Кнопка правки углов у Chromecast. Свои цвета остаются, даже когда выключено. */
    val plexEdit: Boolean = false,
    /** UltraBlur на главной с обложки слайдера. По дефолту вкл (полки скрыты — только слайдер). */
    val heroPosterColors: Boolean = true,
    /** true = UltraBlur на всю главную; false = только зона слайдера. */
    val heroPosterPage: Boolean = true,
    /** 1 — дальность цветов как у алгоритма. Меньше — больше чёрного между углами. */
    val plexIntensity: Float = 1f,
    /** Сдвиг яркости уже добытых цветов. 0 — не трогать. */
    val plexLight: Float = 0f,
    /** Насколько самый край угла уходит в чёрный. 0 — цвет угла как есть. */
    val plexCorner: Float = 0f,
    /** Белый поверх цвета. Больше не рисуется, поле осталось чтобы старые бэкапы читались. */
    val plexWhite: Float = 0f,
    /** Сила каждого угла: левый верх, правый верх, правый низ, левый низ. 0 — как достал алгоритм. */
    val plexPunch: List<Float> = listOf(0f, 0f, 0f, 0f),
    /** Свой цвет угла, ARGB. 0 — цвет алгоритма. Порядок тот же, что у plexPunch. */
    val plexPick: List<Int> = listOf(0, 0, 0, 0),
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    /** original — сразу оригинал, low — только w1280, auto — w1280 и потом оригинал. */
    val backdropQuality: String = BackdropQualityAuto,
    val watchGlow: Boolean = false,
    val haptic: Boolean = true,
    /** Наклон телефона. Без него параллакс в карточке не работает. */
    val gyro: Boolean = false,
    /** Сдвиг первого экрана карточки относительно кадра. */
    val detailParallax: Boolean = false,
    val showPosterTitles: Boolean = true,
    /** Под названием день и месяц. Выключено — только год. */
    val showReleaseDate: Boolean = false,
    val largePosters: Boolean = false,
    /** full — полный постер, light — под ячейку, low — маленький файл. */
    val posterQuality: String = PosterQualityFull,
    /** Сколько обложек качать сразу с одного адреса. */
    val imageParallel: Int = DefaultImageParallel,
    /** auto — ролик сам один раз за заход. manual — Play на постере с видео. */
    val posterMotion: String = PosterMotionAuto,
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
    /** Переход в деталку: полосы цвета темы заезжают одна за другой. */
    val detailShutter: Boolean = false,
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
    /** Подсказка на главной уже была показана в этом включении. */
    val homeHintShown: Boolean = false,
    /** Подсказка про цвет полки в каталоге уже была показана в этом включении. */
    val catalogHintSeen: Boolean = false,
    /** Порядок полок «Моё». Выключить нельзя. */
    val libraryOrder: List<String> = DefaultLibraryOrder,
    /** Заливка полки по id. 0 не хранится. 1 — UltraBlur с первого постера. */
    val shelfFills: Map<String, Int> = emptyMap(),
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

/** Порядок, который уже мог сохраниться: жанры до семейного, потом свои. */
val PreviousHomeRailOrder: List<String> = listOf(
    GenreRails.Action,
    GenreRails.Scifi,
    GenreRails.Horror,
    GenreRails.Drama,
    GenreRails.Thriller,
    GenreRails.Comedy,
    GenreRails.Western,
    GenreRails.War,
    GenreRails.Fantasy,
    GenreRails.Melodrama,
    GenreRails.Crime,
    GenreRails.Detective,
) + listOf(
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

const val FolderRailPrefix = "folder:"

fun String.isFolderRail(): Boolean = startsWith(FolderRailPrefix) && length > FolderRailPrefix.length

fun String.folderRailId(): String = removePrefix(FolderRailPrefix)

fun folderRailKey(id: String): String = "$FolderRailPrefix$id"

fun List<String>.normalizedHomeRailOrder(): List<String> {
    val kept = map { it.trim() }.filter { it in DefaultHomeRailOrder || it.isFolderRail() }.distinct()
    return kept + DefaultHomeRailOrder.filter { it !in kept }
}

/** Папки всегда выше остальных полок. Между собой порядок сохраняется, новые встают первыми. */
fun List<String>.syncFolderRails(folderIds: List<String>): List<String> {
    val keys = folderIds.map { folderRailKey(it) }
    val alive = keys.toSet()
    val normalized = normalizedHomeRailOrder()
    val kept = normalized.filter { it.isFolderRail() && it in alive }
    val missing = keys.filter { it !in kept }
    val rest = normalized.filter { !it.isFolderRail() }
    return missing + kept + rest
}

fun List<String>.placedAfter(anchor: String, ids: List<String>): List<String> {
    val order = toMutableList()
    ids.forEach { order.remove(it) }
    val at = order.indexOf(anchor).let { if (it >= 0) it + 1 else order.size }
    order.addAll(at, ids)
    return order
}

/** Жанровые полки в своих местах, но по алфавиту. Остальные полки не двигаются. */
fun List<String>.genresAlphabetical(): List<String> {
    val rank = GenreRails.All.mapIndexed { index, rail -> rail.id to index }.toMap()
    val sorted = filter { it in rank }.sortedBy { rank.getValue(it) }
    var cursor = 0
    return map { id -> if (id in rank) sorted[cursor++] else id }
}

fun List<String>.canMoveHomeRail(id: String, up: Boolean): Boolean {
    val from = indexOf(id)
    if (from < 0) return false
    val to = if (up) from - 1 else from + 1
    if (to !in indices) return false
    return if (id.isFolderRail()) this[to].isFolderRail() else !this[to].isFolderRail()
}

fun List<String>.moveHomeRail(id: String, up: Boolean): List<String> {
    val order = normalizedHomeRailOrder().toMutableList()
    if (!order.canMoveHomeRail(id, up)) return order
    val from = order.indexOf(id)
    val to = if (up) from - 1 else from + 1
    order.add(to, order.removeAt(from))
    return order
}

fun List<String>.normalizedLibraryOrder(): List<String> {
    val known = DefaultLibraryOrder.toSet()
    val kept = filter { it in known }.distinct()
    return kept + DefaultLibraryOrder.filter { it !in kept }
}

fun List<String>.canMoveLibraryShelf(id: String, up: Boolean): Boolean {
    val order = normalizedLibraryOrder()
    val from = order.indexOf(id)
    if (from < 0) return false
    val to = if (up) from - 1 else from + 1
    return to in order.indices
}

fun List<String>.moveLibraryShelf(id: String, up: Boolean): List<String> {
    val order = normalizedLibraryOrder().toMutableList()
    if (!order.canMoveLibraryShelf(id, up)) return order
    val from = order.indexOf(id)
    val to = if (up) from - 1 else from + 1
    order.add(to, order.removeAt(from))
    return order
}

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractPlex(enabled: Boolean)

    fun setPlexBackdrop(enabled: Boolean)

    fun setPlexEdit(enabled: Boolean)

    fun setHeroPosterColors(enabled: Boolean)

    fun setHeroPosterPage(enabled: Boolean)

    fun setPlexIntensity(value: Float)

    fun setPlexLight(value: Float)

    fun setPlexCorner(value: Float)

    fun setPlexWhite(value: Float)

    fun setPlexPunch(index: Int, value: Float)

    fun setPlexPick(index: Int, color: Int)

    fun resetPlexTone()

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setBackdropQuality(quality: String)

    fun setWatchGlow(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setGyro(enabled: Boolean)

    fun setDetailParallax(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)

    fun setShowReleaseDate(enabled: Boolean)

    fun setLargePosters(enabled: Boolean)

    fun setPosterQuality(quality: String)

    fun setImageParallel(count: Int)

    fun setPosterMotion(mode: String)

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

    fun setDetailShutter(enabled: Boolean)

    fun setBackdropDissolve(enabled: Boolean)

    fun setHomeRailVisible(id: String, visible: Boolean)

    fun setHomeRailsVisible(ids: Collection<String>, visible: Boolean)

    fun setHomeRailOrder(order: List<String>)

    fun setLibraryOrder(order: List<String>)

    fun setShelfFill(id: String, color: Int)

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

    fun setHomeHintShown(shown: Boolean)

    fun setCatalogHintSeen(seen: Boolean)

    fun replace(settings: UserSettings)
}

const val DefaultNetworkTimeoutSec = 4
const val MinNetworkTimeoutSec = 4
const val MaxNetworkTimeoutSec = 20

fun Int.normalizedNetworkTimeout(): Int = coerceIn(MinNetworkTimeoutSec, MaxNetworkTimeoutSec)

const val BackdropQualityOriginal = "original"
const val BackdropQualityLow = "low"
const val BackdropQualityAuto = "auto"

fun String.normalizedBackdropQuality(): String = when (this) {
    BackdropQualityLow, BackdropQualityAuto -> this
    else -> BackdropQualityOriginal
}

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

const val PosterMotionAuto = "auto"
const val PosterMotionManual = "manual"

fun String.normalizedPosterMotion(): String =
    if (this == PosterMotionManual) PosterMotionManual else PosterMotionAuto

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
