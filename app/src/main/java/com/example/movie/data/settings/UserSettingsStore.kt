package com.example.movie.data.settings

import android.content.Context
import com.example.movie.domain.model.DefaultHiddenHomeRails
import com.example.movie.domain.model.DefaultHomeRailOrder
import com.example.movie.domain.model.PreviousHomeRailOrder
import com.example.movie.domain.model.genresAlphabetical
import com.example.movie.domain.model.normalizedHomeRailOrder
import com.example.movie.domain.model.normalizedLibraryOrder
import com.example.movie.domain.model.placedAfter
import com.example.movie.domain.model.GenreRails
import com.example.movie.domain.model.DefaultWatchedPercent
import com.example.movie.domain.model.DefaultHapticProfile
import com.example.movie.domain.model.DefaultNetworkTimeoutSec
import com.example.movie.domain.model.HapticFeel
import com.example.movie.domain.model.HapticSlot
import com.example.movie.domain.model.withHapticFeel
import com.example.movie.domain.model.UserSettings
import com.example.movie.domain.model.UserSettingsRepository
import com.example.movie.domain.model.BarScrollAuto
import com.example.movie.domain.model.BarScrollOff
import com.example.movie.domain.model.DefaultImageParallel
import com.example.movie.domain.model.PosterQualityFull
import com.example.movie.domain.model.normalizedBarScroll
import com.example.movie.domain.model.PosterQualityLight
import com.example.movie.domain.model.normalizedImageParallel
import com.example.movie.domain.model.normalizedNetworkTimeout
import com.example.movie.domain.model.normalizedPlayerCodec
import com.example.movie.domain.model.normalizedPosterQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSettingsStore(context: Context) : UserSettingsRepository {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override fun setExtractPlex(enabled: Boolean) = write { it.copy(extractPlex = enabled) }

    override fun setPlexBackdrop(enabled: Boolean) = write { it.copy(plexBackdrop = enabled) }

    override fun setPlexEdit(enabled: Boolean) = write { it.copy(plexEdit = enabled) }

    override fun setHeroPosterColors(enabled: Boolean) = write { it.copy(heroPosterColors = enabled) }

    override fun setHeroPosterPage(enabled: Boolean) = write { it.copy(heroPosterPage = enabled) }

    override fun setPlexIntensity(value: Float) = tune { it.copy(plexIntensity = value.coerceIn(0.45f, 1.55f)) }

    override fun setPlexLight(value: Float) = tune { it.copy(plexLight = value.coerceIn(-1f, 1f)) }

    override fun setPlexCorner(value: Float) = tune { it.copy(plexCorner = value.coerceIn(0f, 1f)) }

    override fun setPlexWhite(value: Float) = tune { it.copy(plexWhite = value.coerceIn(0f, 0.72f)) }

    override fun setPlexPunch(index: Int, value: Float) = tune { current ->
        val next = current.plexPunch.punch4().toMutableList()
        next[index.coerceIn(0, 3)] = value.coerceIn(-1f, 1f)
        current.copy(plexPunch = next)
    }

    override fun setPlexPick(index: Int, color: Int) = tune { current ->
        val next = current.plexPick.pick4().toMutableList()
        next[index.coerceIn(0, 3)] = color
        current.copy(plexPick = next)
    }

    override fun resetPlexTone() = tune {
        it.copy(
            plexIntensity = 1f,
            plexLight = 0f,
            plexCorner = 0f,
            plexWhite = 0f,
            plexPunch = listOf(0f, 0f, 0f, 0f),
            plexPick = listOf(0, 0, 0, 0),
        )
    }

    override fun setShowLogos(enabled: Boolean) = write { it.copy(showLogos = enabled) }

    override fun setAutoBackdrop(enabled: Boolean) = write { it.copy(autoBackdrop = enabled) }

    override fun setWatchGlow(enabled: Boolean) = write { it.copy(watchGlow = enabled) }

    override fun setHaptic(enabled: Boolean) = write { it.copy(haptic = enabled) }

    override fun setGyro(enabled: Boolean) = write { it.copy(gyro = enabled) }

    override fun setDetailParallax(enabled: Boolean) = write { it.copy(detailParallax = enabled) }

    override fun setShowPosterTitles(enabled: Boolean) = write { it.copy(showPosterTitles = enabled) }

    override fun setShowReleaseDate(enabled: Boolean) = write { it.copy(showReleaseDate = enabled) }

    override fun setLargePosters(enabled: Boolean) = write { it.copy(largePosters = enabled) }

    override fun setPosterQuality(quality: String) = write {
        it.copy(posterQuality = quality.normalizedPosterQuality())
    }

    override fun setImageParallel(count: Int) = write {
        it.copy(imageParallel = count.normalizedImageParallel())
    }

    override fun setPlayerLogo(enabled: Boolean) = write { it.copy(playerLogo = enabled) }

    override fun setPlayerSpeed(speed: Float) = write { it.copy(playerSpeed = speed) }

    override fun setPlayerPicture(mode: String) = write { it.copy(playerPicture = mode) }

    override fun setPlayerDelayUs(delayUs: Long) = write { it.copy(playerDelayUs = delayUs) }

    override fun setPlayerBrightness(brightness: Float?) = write { it.copy(playerBrightness = brightness) }

    override fun setPlayerUnderCamera(enabled: Boolean) = write { it.copy(playerUnderCamera = enabled) }

    override fun setPlayerAutoNext(enabled: Boolean) = write { it.copy(playerAutoNext = enabled) }

    override fun setPlayerHdr(enabled: Boolean) = write { it.copy(playerHdr = enabled) }

    override fun setPlayerCodec(codec: String) = write { it.copy(playerCodec = codec.normalizedPlayerCodec()) }

    override fun setWatchedBadge(enabled: Boolean) = write { it.copy(watchedBadge = enabled) }

    override fun setWhiteTitles(enabled: Boolean) = write { it.copy(whiteTitles = enabled) }

    override fun setBarScroll(mode: String) = write { it.copy(barScroll = mode.normalizedBarScroll()) }

    override fun setNavLabels(enabled: Boolean) = write { it.copy(navLabels = enabled) }

    override fun setSliderShade(enabled: Boolean) = write { it.copy(sliderShade = enabled) }

    override fun setDetailIris(enabled: Boolean) = write {
        it.copy(detailIris = enabled, detailShutter = if (enabled) false else it.detailShutter)
    }

    override fun setDetailShutter(enabled: Boolean) = write {
        it.copy(detailShutter = enabled, detailIris = if (enabled) false else it.detailIris)
    }

    override fun setHints(enabled: Boolean) = write { current ->
        current.copy(
            hints = enabled,
            homeHintSeen = if (enabled) false else current.homeHintSeen,
        )
    }

    override fun setHomeHintSeen(seen: Boolean) = write { it.copy(homeHintSeen = seen) }

    override fun replace(settings: UserSettings) = write {
        settings.copy(
            playerCodec = settings.playerCodec.normalizedPlayerCodec(),
            posterQuality = settings.posterQuality.normalizedPosterQuality(),
            barScroll = settings.barScroll.normalizedBarScroll(),
            imageParallel = settings.imageParallel.normalizedImageParallel(),
            networkTimeoutSec = settings.networkTimeoutSec.normalizedNetworkTimeout(),
            watchedPercent = settings.watchedPercent.coerceIn(50, 100),
            homeRailOrder = settings.homeRailOrder.normalizedHomeRailOrder(),
            libraryOrder = settings.libraryOrder.normalizedLibraryOrder(),
            appTheme = when (settings.appTheme) {
                "graphite" -> "graphite"
                "black" -> "black"
                else -> "cinema"
            },
        )
    }

    override fun setBackdropDissolve(enabled: Boolean) = write { it.copy(backdropDissolve = enabled) }

    override fun setHideRailsLookHint(hidden: Boolean) = write { it.copy(hideRailsLookHint = hidden) }

    override fun setHomeRailOrder(order: List<String>) = write {
        it.copy(homeRailOrder = order.normalizedHomeRailOrder())
    }

    override fun setLibraryOrder(order: List<String>) = write {
        it.copy(libraryOrder = order.normalizedLibraryOrder())
    }

    override fun setHomeRailVisible(id: String, visible: Boolean) = write { current ->
        val next = if (visible) {
            current.hiddenHomeRails - id
        } else {
            current.hiddenHomeRails + id
        }
        current.copy(hiddenHomeRails = next)
    }

    override fun setHomeRailsVisible(ids: Collection<String>, visible: Boolean) = write { current ->
        val next = if (visible) current.hiddenHomeRails - ids.toSet() else current.hiddenHomeRails + ids
        current.copy(hiddenHomeRails = next)
    }

    override fun setAppTheme(theme: String) = write {
        it.copy(
            appTheme = when (theme) {
                "graphite" -> "graphite"
                "black" -> "black"
                else -> "cinema"
            },
        )
    }

    override fun setWatchedPercent(percent: Int) = write {
        it.copy(watchedPercent = percent.coerceIn(50, 100))
    }

    override fun setCountFromStart(enabled: Boolean) = write {
        it.copy(countFromStart = enabled)
    }

    override fun setQualityHintSeen(seen: Boolean) = write {
        it.copy(qualityHintSeen = seen)
    }

    override fun setNetworkTimeoutSec(seconds: Int) = write {
        it.copy(networkTimeoutSec = seconds.normalizedNetworkTimeout())
    }

    override fun setHeaderFill(enabled: Boolean) = write { it.copy(headerFill = enabled) }

    override fun setHapticFeel(slot: String, feel: String) = write { current ->
        val known = HapticSlot.entries.firstOrNull { it.name.equals(slot, ignoreCase = true) }
        val parsed = HapticFeel.entries.firstOrNull { it.name.equals(feel, ignoreCase = true) }
        if (known == null || parsed == null) {
            current
        } else {
            current.copy(hapticProfile = current.hapticProfile.withHapticFeel(known, parsed))
        }
    }

    private fun write(transform: (UserSettings) -> UserSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putBoolean(KEY_COLORS, false)
            .putBoolean(KEY_PLEX, next.extractPlex)
            .putBoolean(KEY_PLEX_BACKDROP, next.plexBackdrop)
            .putBoolean(KEY_PLEX_EDIT, next.plexEdit)
            .putBoolean(KEY_HERO_COLORS, next.heroPosterColors)
            .putBoolean(KEY_HERO_PAGE, next.heroPosterPage)
            .putFloat(KEY_PLEX_INTENSITY, next.plexIntensity)
            .putFloat(KEY_PLEX_LIGHT, next.plexLight)
            .putFloat(KEY_PLEX_CORNER, next.plexCorner)
            .putFloat(KEY_PLEX_WHITE, next.plexWhite)
            .putString(KEY_PLEX_PUNCH, next.plexPunch.punch4().joinToString(","))
            .putString(KEY_PLEX_PICK, next.plexPick.pick4().joinToString(","))
            .putBoolean(KEY_LOGOS, next.showLogos)
            .putBoolean(KEY_BACKDROP, next.autoBackdrop)
            .putBoolean(KEY_WATCH_GLOW, next.watchGlow)
            .putBoolean(KEY_HAPTIC, next.haptic)
            .putBoolean(KEY_GYRO, next.gyro)
            .putBoolean(KEY_DETAIL_PARALLAX, next.detailParallax)
            .putBoolean(KEY_POSTER_TITLES, next.showPosterTitles)
            .putBoolean(KEY_SHOW_RELEASE_DATE, next.showReleaseDate)
            .putBoolean(KEY_LARGE_POSTERS, next.largePosters)
            .putString(KEY_POSTER_QUALITY, next.posterQuality)
            .putInt(KEY_IMAGE_PARALLEL, next.imageParallel)
            .putBoolean(KEY_PLAYER_LOGO, next.playerLogo)
            .putFloat(KEY_PLAYER_SPEED, next.playerSpeed)
            .putString(KEY_PLAYER_PICTURE, next.playerPicture)
            .putLong(KEY_PLAYER_DELAY, next.playerDelayUs)
            .putFloat(KEY_PLAYER_BRIGHTNESS, next.playerBrightness ?: -1f)
            .putBoolean(KEY_PLAYER_UNDER_CAMERA, next.playerUnderCamera)
            .putBoolean(KEY_PLAYER_AUTO_NEXT, next.playerAutoNext)
            .putBoolean(KEY_PLAYER_HDR, next.playerHdr)
            .putString(KEY_PLAYER_CODEC, next.playerCodec)
            .putBoolean(KEY_WATCHED_BADGE, next.watchedBadge)
            .putBoolean(KEY_WHITE_TITLES, next.whiteTitles)
            .putString(KEY_BAR_SCROLL, next.barScroll)
            .putBoolean(KEY_HIDE_BAR, next.barScroll != BarScrollOff)
            .putBoolean(KEY_NAV_LABELS, next.navLabels)
            .putBoolean(KEY_SLIDER_SHADE, next.sliderShade)
            .putBoolean(KEY_DETAIL_IRIS, next.detailIris)
            .putBoolean(KEY_DETAIL_SHUTTER, next.detailShutter)
            .putBoolean(KEY_BACKDROP_DISSOLVE, next.backdropDissolve)
            .putStringSet(KEY_HIDDEN_HOME_RAILS, HashSet(next.hiddenHomeRails))
            .putString(KEY_HOME_RAIL_ORDER, next.homeRailOrder.joinToString(","))
            .putString(KEY_LIBRARY_ORDER, next.libraryOrder.joinToString(","))
            .putBoolean(KEY_HIDE_RAILS_LOOK_HINT, next.hideRailsLookHint)
            .putString(KEY_THEME, next.appTheme)
            .putInt(KEY_WATCHED_PERCENT, next.watchedPercent)
            .putBoolean(KEY_COUNT_FROM_START, next.countFromStart)
            .putBoolean(KEY_QUALITY_HINT, next.qualityHintSeen)
            .putInt(KEY_NETWORK_TIMEOUT, next.networkTimeoutSec)
            .putString(KEY_HAPTIC_PROFILE, next.hapticProfile)
            .putBoolean(KEY_HEADER_FILL, next.headerFill)
            .putBoolean(KEY_HINTS, next.hints)
            .putBoolean(KEY_HOME_HINT, next.homeHintSeen)
            .commit()
        state.value = next
    }

    private fun tune(transform: (UserSettings) -> UserSettings) {
        val next = transform(state.value)
        state.value = next
        prefs.edit()
            .putFloat(KEY_PLEX_INTENSITY, next.plexIntensity)
            .putFloat(KEY_PLEX_LIGHT, next.plexLight)
            .putFloat(KEY_PLEX_CORNER, next.plexCorner)
            .putFloat(KEY_PLEX_WHITE, next.plexWhite)
            .putString(KEY_PLEX_PUNCH, next.plexPunch.punch4().joinToString(","))
            .putString(KEY_PLEX_PICK, next.plexPick.pick4().joinToString(","))
            .apply()
    }

    private fun read(): UserSettings {
        if (!prefs.contains(KEY_FRAME_DEFAULT)) {
            val oldPosterDefault = prefs.getBoolean(KEY_COLORS, true) &&
                !prefs.getBoolean(KEY_BACKDROP_COLORS, false)
            prefs.edit()
                .putBoolean(KEY_FRAME_DEFAULT, true)
                .apply {
                    if (oldPosterDefault) {
                        putBoolean(KEY_COLORS, false)
                        putBoolean(KEY_BACKDROP_COLORS, true)
                    }
                }
                .commit()
        }
        if (!prefs.contains(KEY_PLEX_DEFAULT)) {
            prefs.edit()
                .putBoolean(KEY_PLEX_DEFAULT, true)
                .putBoolean(KEY_PLEX, true)
                .putBoolean(KEY_COLORS, false)
                .putBoolean(KEY_BACKDROP_COLORS, false)
                .commit()
        }
        if (!prefs.contains(KEY_DEFAULTS_230)) {
            prefs.edit()
                .putBoolean(KEY_DEFAULTS_230, true)
                .putBoolean(KEY_WHITE_TITLES, true)
                .putBoolean(KEY_POSTER_TITLES, true)
                .putBoolean(KEY_HERO_COLORS, true)
                .putBoolean(KEY_HERO_PAGE, true)
                .putBoolean(KEY_PLEX, true)
                .putBoolean(KEY_PLEX_BACKDROP, true)
                .putBoolean(KEY_BACKDROP_COLORS, false)
                .commit()
        }
        if (prefs.getBoolean(KEY_COLORS, false)) {
            prefs.edit().putBoolean(KEY_COLORS, false).commit()
        }
        if (prefs.getBoolean(KEY_BACKDROP_COLORS, false)) {
            prefs.edit()
                .putBoolean(KEY_BACKDROP_COLORS, false)
                .putBoolean(KEY_PLEX, true)
                .putBoolean(KEY_PLEX_BACKDROP, true)
                .commit()
        }
        val armHints = !prefs.contains(KEY_HINTS_ARMED)
        if (armHints) {
            prefs.edit()
                .putBoolean(KEY_HINTS, true)
                .putBoolean(KEY_HOME_HINT, false)
                .putBoolean(KEY_HINTS_ARMED, true)
                .apply()
        }
        return UserSettings(
        extractPlex = prefs.getBoolean(KEY_PLEX, true),
        plexBackdrop = prefs.getBoolean(KEY_PLEX_BACKDROP, true),
        plexEdit = prefs.getBoolean(KEY_PLEX_EDIT, false),
        heroPosterColors = prefs.getBoolean(KEY_HERO_COLORS, true),
        heroPosterPage = prefs.getBoolean(KEY_HERO_PAGE, true),
        plexIntensity = prefs.getFloat(KEY_PLEX_INTENSITY, 1f).coerceIn(0.45f, 1.55f),
        plexLight = prefs.getFloat(KEY_PLEX_LIGHT, 0f).coerceIn(-1f, 1f),
        plexCorner = prefs.getFloat(KEY_PLEX_CORNER, 0f).coerceIn(0f, 1f),
        plexWhite = prefs.getFloat(KEY_PLEX_WHITE, 0f).coerceIn(0f, 0.72f),
        plexPunch = prefs.getString(KEY_PLEX_PUNCH, null).toPunch(),
        plexPick = prefs.getString(KEY_PLEX_PICK, null).toPick(),
        showLogos = prefs.getBoolean(KEY_LOGOS, true),
        autoBackdrop = prefs.getBoolean(KEY_BACKDROP, true),
        watchGlow = prefs.getBoolean(KEY_WATCH_GLOW, false),
        haptic = prefs.getBoolean(KEY_HAPTIC, true),
        gyro = prefs.getBoolean(KEY_GYRO, false),
        detailParallax = prefs.getBoolean(KEY_DETAIL_PARALLAX, false),
        showPosterTitles = prefs.getBoolean(KEY_POSTER_TITLES, true),
        showReleaseDate = prefs.getBoolean(KEY_SHOW_RELEASE_DATE, false),
        largePosters = prefs.getBoolean(KEY_LARGE_POSTERS, false),
        posterQuality = when {
            prefs.contains(KEY_POSTER_QUALITY) ->
                prefs.getString(KEY_POSTER_QUALITY, PosterQualityFull).orEmpty().normalizedPosterQuality()
            prefs.getBoolean(KEY_COMPACT_POSTERS, false) -> PosterQualityLight
            else -> PosterQualityFull
        },
        imageParallel = prefs.getInt(KEY_IMAGE_PARALLEL, DefaultImageParallel).normalizedImageParallel(),
        playerLogo = prefs.getBoolean(KEY_PLAYER_LOGO, true),
        playerSpeed = prefs.getFloat(KEY_PLAYER_SPEED, 1f),
        playerPicture = prefs.getString(KEY_PLAYER_PICTURE, "fit") ?: "fit",
        playerDelayUs = prefs.getLong(KEY_PLAYER_DELAY, 0L),
        playerBrightness = prefs.getFloat(KEY_PLAYER_BRIGHTNESS, -1f).takeIf { it in 0f..1f },
        playerUnderCamera = prefs.getBoolean(KEY_PLAYER_UNDER_CAMERA, false),
        playerAutoNext = prefs.getBoolean(KEY_PLAYER_AUTO_NEXT, true),
        playerHdr = prefs.getBoolean(KEY_PLAYER_HDR, true),
        playerCodec = prefs.getString(KEY_PLAYER_CODEC, "auto").orEmpty().normalizedPlayerCodec(),
        watchedBadge = prefs.getBoolean(KEY_WATCHED_BADGE, true),
        whiteTitles = prefs.getBoolean(KEY_WHITE_TITLES, true),
        barScroll = when {
            prefs.contains(KEY_BAR_SCROLL) ->
                prefs.getString(KEY_BAR_SCROLL, BarScrollAuto).orEmpty().normalizedBarScroll()
            prefs.contains(KEY_HIDE_BAR) && !prefs.getBoolean(KEY_HIDE_BAR, true) -> BarScrollOff
            else -> BarScrollAuto
        },
        navLabels = prefs.getBoolean(KEY_NAV_LABELS, false),
        sliderShade = prefs.getBoolean(KEY_SLIDER_SHADE, false),
        detailIris = prefs.getBoolean(KEY_DETAIL_IRIS, false),
        detailShutter = prefs.getBoolean(KEY_DETAIL_SHUTTER, false),
        backdropDissolve = prefs.getBoolean(KEY_BACKDROP_DISSOLVE, false),
        hiddenHomeRails = run {
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_ACTION_RAIL_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Action)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_ACTION_RAIL_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_MELO_CRIME_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Melodrama)
                hidden.add(GenreRails.Crime)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_MELO_CRIME_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_DETECTIVE_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Detective)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_DETECTIVE_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_HISTORICAL_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Historical)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_HISTORICAL_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_MUSIC_GENRES_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Music)
                hidden.add(GenreRails.Musical)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_MUSIC_GENRES_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_LIFE_GENRES_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Family)
                hidden.add(GenreRails.Adventure)
                hidden.add(GenreRails.Biography)
                hidden.add(GenreRails.Sport)
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_LIFE_GENRES_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_OFF_RAILS)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                hidden.add(GenreRails.Comedy)
                hidden.add(GenreRails.War)
                hidden.add("watching")
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_OFF_RAILS, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS) && !prefs.contains(KEY_GENRE_RAILS_HIDDEN)) {
                val hidden = prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet()).orEmpty().toMutableSet()
                GenreRails.All.forEach { rail ->
                    if (rail.id != GenreRails.Action) hidden.add(rail.id)
                }
                prefs.edit()
                    .putStringSet(KEY_HIDDEN_HOME_RAILS, hidden)
                    .putBoolean(KEY_GENRE_RAILS_HIDDEN, true)
                    .commit()
            }
            if (prefs.contains(KEY_HIDDEN_HOME_RAILS)) {
                prefs.getStringSet(KEY_HIDDEN_HOME_RAILS, emptySet())?.toSet().orEmpty()
            } else {
                DefaultHiddenHomeRails
            }
        },
        homeRailOrder = run {
            val hadOrder = prefs.contains(KEY_HOME_RAIL_ORDER)
            var order = prefs.getString(KEY_HOME_RAIL_ORDER, null)
                ?.split(',')
                .orEmpty()
                .normalizedHomeRailOrder()
            if (hadOrder && !prefs.contains(KEY_HOME_RAIL_ORDER_TOP)) {
                val withoutNew = order.filter {
                    it != GenreRails.Melodrama && it != GenreRails.Crime && it != GenreRails.Detective
                }
                if (withoutNew == PreviousHomeRailOrder) order = DefaultHomeRailOrder
                prefs.edit().putBoolean(KEY_HOME_RAIL_ORDER_TOP, true).commit()
            }
            if (!prefs.contains(KEY_MELO_CRIME_ORDER)) {
                if (hadOrder && order != DefaultHomeRailOrder) {
                    order = order.placedAfter(
                        GenreRails.Fantasy,
                        listOf(GenreRails.Melodrama, GenreRails.Crime),
                    )
                }
                prefs.edit()
                    .putBoolean(KEY_MELO_CRIME_ORDER, true)
                    .apply { if (hadOrder) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            if (!prefs.contains(KEY_DETECTIVE_ORDER)) {
                if (hadOrder && order != DefaultHomeRailOrder) {
                    order = order.placedAfter(GenreRails.Crime, listOf(GenreRails.Detective))
                }
                prefs.edit()
                    .putBoolean(KEY_DETECTIVE_ORDER, true)
                    .apply { if (hadOrder) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            if (!prefs.contains(KEY_LIFE_GENRES_ORDER)) {
                if (hadOrder && order != DefaultHomeRailOrder) {
                    order = order.placedAfter(
                        GenreRails.Detective,
                        listOf(
                            GenreRails.Family,
                            GenreRails.Adventure,
                            GenreRails.Biography,
                            GenreRails.Sport,
                        ),
                    )
                }
                prefs.edit()
                    .putBoolean(KEY_LIFE_GENRES_ORDER, true)
                    .apply { if (hadOrder) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            if (!prefs.contains(KEY_GENRE_ALPHA_ORDER)) {
                val sorted = order.genresAlphabetical()
                val changed = hadOrder && sorted != order
                if (changed) order = sorted
                prefs.edit()
                    .putBoolean(KEY_GENRE_ALPHA_ORDER, true)
                    .apply { if (changed) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            if (!prefs.contains(KEY_MUSIC_GENRES_ORDER)) {
                val sorted = order.genresAlphabetical()
                val changed = hadOrder && sorted != order
                if (changed) order = sorted
                prefs.edit()
                    .putBoolean(KEY_MUSIC_GENRES_ORDER, true)
                    .apply { if (changed) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            if (!prefs.contains(KEY_HISTORICAL_ORDER)) {
                val sorted = order.genresAlphabetical()
                val changed = hadOrder && sorted != order
                if (changed) order = sorted
                prefs.edit()
                    .putBoolean(KEY_HISTORICAL_ORDER, true)
                    .apply { if (changed) putString(KEY_HOME_RAIL_ORDER, order.joinToString(",")) }
                    .commit()
            }
            order
        },
        hideRailsLookHint = prefs.getBoolean(KEY_HIDE_RAILS_LOOK_HINT, false),
        appTheme = prefs.getString(KEY_THEME, "cinema") ?: "cinema",
        watchedPercent = prefs.getInt(KEY_WATCHED_PERCENT, DefaultWatchedPercent).coerceIn(50, 100),
        countFromStart = prefs.getBoolean(KEY_COUNT_FROM_START, false),
        qualityHintSeen = prefs.getBoolean(KEY_QUALITY_HINT, false),
        networkTimeoutSec = prefs.getInt(KEY_NETWORK_TIMEOUT, DefaultNetworkTimeoutSec).normalizedNetworkTimeout(),
        hapticProfile = prefs.getString(KEY_HAPTIC_PROFILE, DefaultHapticProfile).orEmpty().ifBlank { DefaultHapticProfile },
        headerFill = prefs.getBoolean(KEY_HEADER_FILL, false),
        hints = if (armHints) true else prefs.getBoolean(KEY_HINTS, true),
        homeHintSeen = if (armHints) false else prefs.getBoolean(KEY_HOME_HINT, false),
        libraryOrder = prefs.getString(KEY_LIBRARY_ORDER, null)
            ?.split(',')
            .orEmpty()
            .normalizedLibraryOrder(),
        )
    }

    private companion object {
        const val FILE = "user_settings"
        const val KEY_COLORS = "extract_colors"
        const val KEY_BACKDROP_COLORS = "extract_backdrop"
        const val KEY_PLEX = "extract_plex"
        const val KEY_PLEX_BACKDROP = "plex_backdrop"
        const val KEY_PLEX_EDIT = "plex_edit"
        const val KEY_PLEX_DEFAULT = "plex_color_default"
        const val KEY_FRAME_DEFAULT = "frame_color_default"
        const val KEY_DEFAULTS_230 = "defaults_2_30"
        const val KEY_HERO_COLORS = "hero_poster_colors"
        const val KEY_HERO_PAGE = "hero_poster_page"
        const val KEY_PLEX_INTENSITY = "plex_intensity"
        const val KEY_PLEX_LIGHT = "plex_light"
        const val KEY_PLEX_CORNER = "plex_corner"
        const val KEY_PLEX_WHITE = "plex_white"
        const val KEY_PLEX_PUNCH = "plex_punch"
        const val KEY_PLEX_PICK = "plex_pick"
        const val KEY_LOGOS = "show_logos"
        const val KEY_BACKDROP = "auto_backdrop"
        const val KEY_WATCH_GLOW = "watch_glow"
        const val KEY_HAPTIC = "haptic"
        const val KEY_GYRO = "gyro"
        const val KEY_DETAIL_PARALLAX = "detail_parallax"
        const val KEY_POSTER_TITLES = "poster_titles"
        const val KEY_SHOW_RELEASE_DATE = "show_release_date"
        const val KEY_LARGE_POSTERS = "large_posters"
        const val KEY_COMPACT_POSTERS = "compact_posters"
        const val KEY_POSTER_QUALITY = "poster_quality"
        const val KEY_IMAGE_PARALLEL = "image_parallel"
        const val KEY_PLAYER_LOGO = "player_logo"
        const val KEY_PLAYER_SPEED = "player_speed"
        const val KEY_PLAYER_PICTURE = "player_picture"
        const val KEY_PLAYER_DELAY = "player_delay_us"
        const val KEY_PLAYER_BRIGHTNESS = "player_brightness"
        const val KEY_PLAYER_UNDER_CAMERA = "player_under_camera"
        const val KEY_PLAYER_AUTO_NEXT = "player_auto_next"
        const val KEY_PLAYER_HDR = "player_hdr"
        const val KEY_PLAYER_CODEC = "player_codec"
        const val KEY_WATCHED_BADGE = "watched_badge"
        const val KEY_WHITE_TITLES = "white_titles"
        const val KEY_HIDE_BAR = "hide_bottom_bar_scroll"
        const val KEY_BAR_SCROLL = "bar_scroll"
        const val KEY_NAV_LABELS = "nav_labels"
        const val KEY_SLIDER_SHADE = "slider_shade"
        const val KEY_DETAIL_IRIS = "detail_iris"
        const val KEY_DETAIL_SHUTTER = "detail_shutter"
        const val KEY_BACKDROP_DISSOLVE = "backdrop_dissolve"
        const val KEY_HIDDEN_HOME_RAILS = "hidden_home_rails"
        const val KEY_HOME_RAIL_ORDER = "home_rail_order"
        const val KEY_LIBRARY_ORDER = "library_order"
        const val KEY_HOME_RAIL_ORDER_TOP = "home_rail_order_top"
        const val KEY_ACTION_RAIL_HIDDEN = "action_rail_hidden_default"
        const val KEY_GENRE_RAILS_HIDDEN = "genre_rails_hidden_default"
        const val KEY_MELO_CRIME_HIDDEN = "melo_crime_hidden_default"
        const val KEY_MELO_CRIME_ORDER = "melo_crime_order_default"
        const val KEY_DETECTIVE_HIDDEN = "detective_hidden_default"
        const val KEY_DETECTIVE_ORDER = "detective_order_default"
        const val KEY_LIFE_GENRES_HIDDEN = "life_genres_hidden_default"
        const val KEY_LIFE_GENRES_ORDER = "life_genres_order_default"
        const val KEY_GENRE_ALPHA_ORDER = "genre_alpha_order"
        const val KEY_MUSIC_GENRES_HIDDEN = "music_genres_hidden_default"
        const val KEY_MUSIC_GENRES_ORDER = "music_genres_order_default"
        const val KEY_HISTORICAL_HIDDEN = "historical_genre_hidden_default"
        const val KEY_HISTORICAL_ORDER = "historical_genre_order_default"
        const val KEY_OFF_RAILS = "off_comedy_war_watching"
        const val KEY_HIDE_RAILS_LOOK_HINT = "hide_rails_look_hint"
        const val KEY_THEME = "app_theme"
        const val KEY_WATCHED_PERCENT = "watched_percent"
        const val KEY_COUNT_FROM_START = "count_from_start"
        const val KEY_QUALITY_HINT = "quality_hint_seen"
        const val KEY_NETWORK_TIMEOUT = "network_timeout_sec"
        const val KEY_HAPTIC_PROFILE = "haptic_profile"
        const val KEY_HEADER_FILL = "header_fill"
        const val KEY_HINTS = "hints"
        const val KEY_HOME_HINT = "home_hint_seen"
        const val KEY_HINTS_ARMED = "hints_armed"
    }
}

private fun List<Float>.punch4(): List<Float> = List(4) { getOrElse(it) { 0f }.coerceIn(-1f, 1f) }

private fun List<Int>.pick4(): List<Int> = List(4) { getOrElse(it) { 0 } }

private fun String?.toPunch(): List<Float> {
    val parts = this?.split(',')?.mapNotNull { it.toFloatOrNull() }.orEmpty()
    return parts.punch4()
}

private fun String?.toPick(): List<Int> {
    val parts = this?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
    return parts.pick4()
}
