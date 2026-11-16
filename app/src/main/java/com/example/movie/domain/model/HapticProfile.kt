package com.example.movie.domain.model

enum class HapticSlot {
    Poster,
    Watch,
    Favorite,
    Download,
    Menu,
    Scroll,
    Page,
    Hold,
    Card,
    Play,
}

enum class HapticFeel {
    Off,
    Tick,
    Click,
    Heavy,
    Double,
    Texture,
    Pop,
    Thud,
    Hold,
}

private val SharedFeels = listOf(
    HapticFeel.Off,
    HapticFeel.Tick,
    HapticFeel.Click,
    HapticFeel.Heavy,
    HapticFeel.Double,
    HapticFeel.Texture,
    HapticFeel.Pop,
    HapticFeel.Thud,
)

fun feelsFor(slot: HapticSlot): List<HapticFeel> = when (slot) {
    HapticSlot.Hold -> SharedFeels + HapticFeel.Hold
    else -> SharedFeels
}

fun defaultHapticFeel(slot: HapticSlot): HapticFeel = when (slot) {
    HapticSlot.Hold -> HapticFeel.Hold
    HapticSlot.Card, HapticSlot.Play -> HapticFeel.Off
    else -> HapticFeel.Tick
}

fun nextHapticFeel(slot: HapticSlot, current: HapticFeel): HapticFeel {
    val options = feelsFor(slot)
    val index = options.indexOf(current).let { if (it < 0) 0 else it }
    return options[(index + 1) % options.size]
}

fun String.hapticFeel(slot: HapticSlot): HapticFeel {
    val raw = split(',')
        .map { it.trim() }
        .firstOrNull { it.substringBefore('=') == slot.wire }
        ?.substringAfter('=')
        .orEmpty()
    val parsed = HapticFeel.entries.firstOrNull { it.wire == raw } ?: defaultHapticFeel(slot)
    return if (parsed in feelsFor(slot)) parsed else defaultHapticFeel(slot)
}

fun String.withHapticFeel(slot: HapticSlot, feel: HapticFeel): String {
    val chosen = if (feel in feelsFor(slot)) feel else defaultHapticFeel(slot)
    return HapticSlot.entries.joinToString(",") { item ->
        val value = if (item == slot) chosen else hapticFeel(item)
        "${item.wire}=${value.wire}"
    }
}

val DefaultHapticProfile: String = HapticSlot.entries.joinToString(",") { slot ->
    "${slot.wire}=${defaultHapticFeel(slot).wire}"
}

private val HapticSlot.wire: String get() = name.lowercase()

private val HapticFeel.wire: String get() = name.lowercase()
