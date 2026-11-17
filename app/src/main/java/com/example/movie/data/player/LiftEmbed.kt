package com.example.movie.data.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal enum class WatchKind { Hls, Dash }

internal enum class WatchCodec { H264, Av1, Vp9 }

internal enum class DecoderTier { None, Software, Hardware }

/** Потолок, как «Максимальный видеокодек» у Lift. auto — без потолка. */
internal enum class CodecCeiling { Auto, Av1, Vp9, H264 }

internal data class WatchSubtitle(val url: String, val name: String)

/** Кандидат манифеста: HLS / DASH (в т.ч. dash_av1 / dash_vp9). */
internal data class WatchManifest(
    val url: String,
    val kind: WatchKind,
    val codec: WatchCodec = WatchCodec.H264,
)

internal data class WatchStream(
    val url: String,
    val kind: WatchKind,
    val subtitles: List<WatchSubtitle>,
    val audioNames: List<String> = emptyList(),
    val audioOrder: List<Int> = emptyList(),
    val alternateUrl: String? = null,
    val alternateKind: WatchKind? = null,
    val manifests: List<WatchManifest> = emptyList(),
    val manifestPicked: Boolean = false,
    val crops: Map<String, List<FrameCrop>> = emptyMap(),
)

/** Активная картинка внутри кадра: полосы сверху и снизу уже вшиты в пиксели. */
internal data class FrameCrop(
    val width: Int,
    val height: Int,
    val activeX: Int,
    val activeY: Int,
    val activeWidth: Int,
    val activeHeight: Int,
)

internal fun WatchStream.frameCrop(videoHeight: Int): FrameCrop? {
    if (videoHeight <= 0 || crops.isEmpty()) return null
    val manifest = manifests.firstOrNull { it.url == url }
    val key = when {
        (manifest?.kind ?: kind) == WatchKind.Hls -> "hls"
        manifest?.codec == WatchCodec.Av1 -> "dash_av1"
        manifest?.codec == WatchCodec.Vp9 -> "dash_vp9"
        else -> "dash"
    }
    val pool = crops[key].orEmpty().ifEmpty { crops.values.flatten() }
    return pool.minByOrNull { kotlin.math.abs(it.height - videoHeight) }
}

internal data class WatchEpisode(
    val season: Int,
    val episode: Int,
    val stream: WatchStream,
)

internal data class WatchPlayback(
    val stream: WatchStream,
    val episodes: List<WatchEpisode> = emptyList(),
)

internal const val LIFT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

private val hlsUrl = Regex("""["']?hls["']?\s*:\s*"(https?:[^"]+)"""")
private val dashAv1Url = Regex("""["']?dash_av1["']?\s*:\s*"(https?:[^"]+)"""")
private val dashVp9Url = Regex("""["']?dash_vp9["']?\s*:\s*"(https?:[^"]+)"""")
private val dashUrl = Regex("""["']?dash(?:a)?["']?\s*:\s*"(https?:[^"]+)"""")
private val ccBlock = Regex("""["']?cc["']?\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
private val ccItem = Regex("""\{\s*"url"\s*:\s*"(https?:[^"]+)"\s*,\s*"name"\s*:\s*"([^"]*)"""")
private val seasonsKey = Regex("""["']?seasons["']?\s*:\s*\[""")
private val audioKey = Regex("""["']?audio["']?\s*:\s*\{""")
private val currentEpisode = Regex(
    """["']?current["']?\s*:\s*\{[^}]*?["']?season["']?\s*:\s*"?(\d+)"?[^}]*?["']?episode["']?\s*:\s*"?(\d+)""",
)

private val json = Json { ignoreUnknownKeys = true }

private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

internal fun parseLiftEmbed(
    html: String,
    season: Int? = null,
    episode: Int? = null,
): WatchPlayback? {
    val episodes = parseEpisodes(html)
    if (episodes.isNotEmpty()) {
        val current = currentEpisode.find(html)
        val wantedSeason = season ?: current?.groupValues?.get(1)?.toIntOrNull()
        val wantedEpisode = episode ?: current?.groupValues?.get(2)?.toIntOrNull()
        val picked = episodes.firstOrNull { it.season == wantedSeason && it.episode == wantedEpisode }
            ?: episodes.firstOrNull { it.season == wantedSeason }
            ?: episodes.first()
        return WatchPlayback(picked.stream, episodes)
    }
    val single = singleStream(html) ?: return null
    return WatchPlayback(single)
}

internal data class TrailerSources(val video: String, val audio: String?)

internal fun fetchTrailerSources(pageUrl: String): TrailerSources {
    val master = fetchLiftStream(pageUrl).stream.url
    if (!master.substringBefore('?').endsWith(".m3u8")) return TrailerSources(master, null)
    val text = fetchText(master, pageUrl) ?: return TrailerSources(master, null)
    return TrailerSources(
        video = mediaPlaylist(master, text) ?: master,
        audio = audioPlaylist(master, text),
    )
}

/** Просмотр: playlist Lift (AV1/VP9), иначе HTML embed. */
internal fun fetchLiftWatch(pageUrl: String): WatchPlayback {
    val page = pageUrl.toHttpUrlOrNull()
    val id = page?.pathSegments?.lastOrNull()?.toIntOrNull()
    if (page != null && id != null && page.host.endsWith("liftw.ws")) {
        val season = page.queryParameter("season")?.toIntOrNull()
        val episode = page.queryParameter("episode")?.toIntOrNull()
        runCatching { return fetchLiftPlaylist(id, season, episode) }
    }
    return fetchLiftStream(pageUrl)
}

internal fun fetchLiftStream(pageUrl: String): WatchPlayback {
    val page = pageUrl.toHttpUrlOrNull()
    val request = Request.Builder()
        .url(pageUrl)
        .header("User-Agent", LIFT_USER_AGENT)
        .build()
    client.newCall(request).execute().use { response ->
        val html = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("embed ${response.code}")
        return parseLiftEmbed(
            html,
            season = page?.queryParameter("season")?.toIntOrNull(),
            episode = page?.queryParameter("episode")?.toIntOrNull(),
        ) ?: error("no direct stream")
    }
}

/**
 * Нативный playlist Lift (`api.liftw.ws`) — тут `streams.dash_av1` / `dash_vp9` / `hls`
 * с полной лестницей (4K). Embed HTML часто отдаёт те же файлы, но с урезанным манифестом до 1080.
 */
internal fun fetchLiftPlaylist(
    id: Int,
    season: Int? = null,
    episode: Int? = null,
): WatchPlayback {
    val request = Request.Builder()
        .url("https://api.liftw.ws/playlist?id=$id")
        .header("User-Agent", LIFT_USER_AGENT)
        .build()
    client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) error("playlist ${response.code}")
        return parseLiftPlaylist(body, season, episode) ?: error("no playlist stream")
    }
}

/** Сначала нативный playlist (4K), иначе embed. */
internal fun fetchLiftPlayback(
    id: Int,
    season: Int? = null,
    episode: Int? = null,
): WatchPlayback = runCatching { fetchLiftPlaylist(id, season, episode) }
    .getOrElse { fetchLiftStream(liftPlaylistFallbackUrl(id, season, episode)) }

private fun liftPlaylistFallbackUrl(id: Int, season: Int?, episode: Int?): String {
    val base = "https://embed.liftw.ws/embed/movie/$id"
    if (season == null || episode == null) return base
    return "$base?season=$season&episode=$episode"
}

internal fun parseLiftPlaylist(
    raw: String,
    season: Int? = null,
    episode: Int? = null,
): WatchPlayback? {
    val root = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return null
    val seasons = root["seasons"] as? JsonArray
    if (seasons != null && seasons.isNotEmpty()) {
        val episodes = seasons.mapNotNull { it as? JsonObject }.flatMap { seasonObj ->
            val number = seasonObj.int("season") ?: return@flatMap emptyList()
            val rows = seasonObj["episodes"] as? JsonArray ?: return@flatMap emptyList()
            rows.mapNotNull { item ->
                val ep = item as? JsonObject ?: return@mapNotNull null
                val index = ep.int("episode") ?: return@mapNotNull null
                val stream = ep.stream() ?: return@mapNotNull null
                WatchEpisode(number, index, stream)
            }
        }.sortedWith(compareBy({ it.season }, { it.episode }))
        if (episodes.isEmpty()) return null
        val picked = episodes.firstOrNull { it.season == season && it.episode == episode }
            ?: episodes.firstOrNull { it.season == season }
            ?: episodes.first()
        return WatchPlayback(picked.stream, episodes)
    }
    val stream = root.stream() ?: return null
    return WatchPlayback(stream)
}

private fun parseEpisodes(html: String): List<WatchEpisode> {
    val key = seasonsKey.find(html) ?: return emptyList()
    val start = html.indexOf('[', key.range.first)
    val raw = extractJsonArray(html, start) ?: return emptyList()
    val seasons = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
    return seasons.mapNotNull { it as? JsonObject }.flatMap { season ->
        val number = season.int("season") ?: return@flatMap emptyList()
        val episodes = season["episodes"] as? JsonArray ?: return@flatMap emptyList()
        episodes.mapNotNull { item ->
            val episode = item as? JsonObject ?: return@mapNotNull null
            val index = episode.int("episode") ?: return@mapNotNull null
            val stream = episode.stream() ?: return@mapNotNull null
            WatchEpisode(number, index, stream)
        }
    }.sortedWith(compareBy({ it.season }, { it.episode }))
}

private fun JsonObject.stream(): WatchStream? {
    val nested = this["streams"] as? JsonObject
    val source = nested ?: this
    val subs = (this["cc"] as? JsonArray).orEmpty().mapNotNull { item ->
        val row = item as? JsonObject ?: return@mapNotNull null
        val subtitle = row.string("url") ?: return@mapNotNull null
        WatchSubtitle(subtitle, row.string("name").orEmpty())
    }
    val (names, order) = (this["audio"] as? JsonObject)?.trackNames() ?: (emptyList<String>() to emptyList())
    return buildStream(
        hls = source.string("hls"),
        dashAv1 = source.string("dash_av1") ?: source.string("dashAv1"),
        dashVp9 = source.string("dash_vp9") ?: source.string("dashVp9"),
        dash = source.string("dash") ?: source.string("dasha"),
        subs = subs,
        names = names,
        order = order,
        crops = frameCrops(),
    )
}

private fun singleStream(html: String): WatchStream? {
    val hls = hlsUrl.find(html)?.groupValues?.get(1)?.cleanUrl()
    val dashAv1 = dashAv1Url.find(html)?.groupValues?.get(1)?.cleanUrl()
    val dashVp9 = dashVp9Url.find(html)?.groupValues?.get(1)?.cleanUrl()
    // dash / dasha — не dash_av1 / dash_vp9
    val dash = dashUrl.findAll(html)
        .map { it.groupValues[1].cleanUrl() }
        .firstOrNull { candidate ->
            candidate != dashAv1 && candidate != dashVp9
        }
    val subs = ccBlock.find(html)?.groupValues?.get(1).orEmpty()
        .let { block ->
            ccItem.findAll(block).map { match ->
                WatchSubtitle(match.groupValues[1].cleanUrl(), match.groupValues[2])
            }.toList()
        }
    val (names, order) = audioFromHtml(html)
    return buildStream(hls, dashAv1, dashVp9, dash, subs, names, order)
}

private fun buildStream(
    hls: String?,
    dashAv1: String?,
    dashVp9: String?,
    dash: String?,
    subs: List<WatchSubtitle>,
    names: List<String>,
    order: List<Int>,
    crops: Map<String, List<FrameCrop>> = emptyMap(),
): WatchStream? {
    val manifests = buildList {
        if (hls != null) add(WatchManifest(hls, WatchKind.Hls, WatchCodec.H264))
        // AV1 → VP9 → обычный DASH — как у Lift
        if (dashAv1 != null) add(WatchManifest(dashAv1, WatchKind.Dash, WatchCodec.Av1))
        if (dashVp9 != null && dashVp9 != dashAv1) add(WatchManifest(dashVp9, WatchKind.Dash, WatchCodec.Vp9))
        if (dash != null && dash != dashAv1 && dash != dashVp9) add(WatchManifest(dash, WatchKind.Dash, WatchCodec.H264))
    }.distinctBy { it.url }
    if (manifests.isEmpty()) return null
    val primary = manifests.first()
    val alternate = manifests.getOrNull(1)
    return WatchStream(
        url = primary.url,
        kind = primary.kind,
        subtitles = subs,
        audioNames = names,
        audioOrder = order,
        alternateUrl = alternate?.url,
        alternateKind = alternate?.kind,
        manifests = manifests,
        crops = crops,
    )
}

private fun JsonObject.frameCrops(): Map<String, List<FrameCrop>> {
    val infos = this["infos"] as? JsonObject ?: return emptyMap()
    return buildMap {
        infos.forEach { (codec, node) ->
            val rows = node as? JsonObject ?: return@forEach
            val crops = rows.values.mapNotNull { (it as? JsonObject)?.frameCrop() }
            if (crops.isNotEmpty()) put(codec, crops)
        }
    }
}

private fun JsonObject.frameCrop(): FrameCrop? {
    val width = looseInt("width") ?: return null
    val height = looseInt("height") ?: return null
    val activeX = looseInt("active_x") ?: return null
    val activeY = looseInt("active_y") ?: return null
    val activeWidth = looseInt("active_width") ?: return null
    val activeHeight = looseInt("active_height") ?: return null
    if (width <= 0 || height <= 0 || activeWidth <= 0 || activeHeight <= 0) return null
    val flag = (this["active_has_crop"] as? JsonPrimitive)?.contentOrNull?.lowercase()
    val enabled = when (flag) {
        "1", "true", "yes" -> true
        "0", "false", "no" -> false
        else -> activeWidth < width || activeHeight < height
    }
    if (!enabled || (activeWidth >= width && activeHeight >= height)) return null
    return FrameCrop(width, height, activeX, activeY, activeWidth, activeHeight)
}

private fun JsonObject.looseInt(key: String): Int? {
    val prim = this[key] as? JsonPrimitive ?: return null
    return prim.intOrNull ?: prim.contentOrNull?.substringBefore('.')?.toIntOrNull()
}

/**
 * Берёт манифест с самым высоким max height (4K из dash_av1/vp9),
 * при равенстве — с большим числом дорожек, затем DASH.
 */
internal fun WatchStream.preferRicherManifest(): WatchStream {
    if (manifestPicked) return this
    val options = manifests.ifEmpty {
        listOfNotNull(
            WatchManifest(url, kind),
            alternateUrl?.let { alt -> alternateKind?.let { WatchManifest(alt, it) } },
        )
    }.distinctBy { it.url }
    if (options.isEmpty()) return copy(manifestPicked = true)
    if (options.size == 1) {
        val only = options.first()
        return copy(
            url = only.url,
            kind = only.kind,
            alternateUrl = null,
            alternateKind = null,
            manifests = options,
            manifestPicked = true,
        )
    }
    val scored = options.map { option -> option to fetchHeights(option.url) }
    val best = scored.maxWith(
        compareBy(
            { it.second.maxOrNull() ?: 0 },
            { it.second.size },
            { if (it.first.kind == WatchKind.Dash) 1 else 0 },
        ),
    )
    val rest = options.filterNot { it.url == best.first.url }
    return copy(
        url = best.first.url,
        kind = best.first.kind,
        alternateUrl = rest.firstOrNull()?.url,
        alternateKind = rest.firstOrNull()?.kind,
        manifests = options,
        manifestPicked = true,
    )
}

internal fun WatchPlayback.forDownload(): WatchPlayback = copy(
    stream = stream.preferRicherManifest(),
    episodes = episodes.map { episode -> episode.copy(stream = episode.stream.preferRicherManifest()) },
)

internal fun codecRank(manifest: WatchManifest): Int = when {
    manifest.codec == WatchCodec.Av1 -> 4
    manifest.codec == WatchCodec.Vp9 -> 3
    manifest.kind == WatchKind.Dash -> 2
    else -> 1
}

/** Железо важнее софта. Внутри уровня: AV1, VP9, DASH H.264, HLS. */
internal fun List<WatchManifest>.playableOrder(tier: (WatchManifest) -> DecoderTier): List<WatchManifest> {
    if (isEmpty()) return this
    val known = map { it to tier(it) }
    val hardware = known.filter { it.second == DecoderTier.Hardware }
    val software = known.filter { it.second == DecoderTier.Software }
    val pool = when {
        hardware.isNotEmpty() -> hardware
        software.isNotEmpty() -> software
        else -> known
    }
    return pool.sortedByDescending { codecRank(it.first) }.map { it.first }
}

internal data class ManifestRange(val sdr: Boolean, val hdr: Boolean)

internal fun ManifestRange.playsOn(displayHdr: Boolean): Boolean = displayHdr || sdr || !hdr

/** HLS `VIDEO-RANGE` и DASH PQ/HLG/Dolby. Пустой разбор не режем. */
internal fun manifestRange(text: String): ManifestRange = when {
    text.contains("#EXT-X-STREAM-INF") -> hlsRange(text)
    text.contains("<AdaptationSet") -> dashRange(text)
    else -> ManifestRange(sdr = true, hdr = false)
}

internal fun pickCastManifest(
    manifests: List<WatchManifest>,
    ceiling: CodecCeiling,
    height: Int?,
    heightsOf: (WatchManifest) -> Set<Int>,
    tier: (WatchManifest) -> DecoderTier,
): WatchManifest? {
    if (manifests.isEmpty()) return null
    val safe = if (ceiling == CodecCeiling.Auto) CodecCeiling.H264 else ceiling
    val preferred = manifests.underCeiling(safe, tier).ifEmpty { manifests }
    if (height == null) return preferred.firstOrNull()
    preferred.firstOrNull { height in heightsOf(it) }?.let { return it }
    if (ceiling != CodecCeiling.Auto) return preferred.firstOrNull()
    return manifests.sortedBy { codecRank(it) }.firstOrNull { height in heightsOf(it) }
        ?: preferred.firstOrNull()
}

internal fun WatchStream.forCast(ceiling: CodecCeiling, height: Int?): Pair<WatchStream, List<Int>> {
    val tier = { manifest: WatchManifest -> manifest.decoderTier() }
    val measured = manifests.distinctBy { it.url }.map { it to fetchHeights(it.url) }
    val heightsOf = { manifest: WatchManifest ->
        measured.firstOrNull { it.first.url == manifest.url }?.second ?: emptySet()
    }
    val picked = pickCastManifest(manifests, ceiling, height, heightsOf, tier)
    val allowed = if (ceiling == CodecCeiling.Auto) {
        manifests
    } else {
        manifests.underCeiling(ceiling, tier)
    }
    val heights = measured
        .filter { row -> allowed.any { it.url == row.first.url } }
        .flatMap { it.second }
        .filter { it > 0 }
        .distinct()
        .sortedDescending()
    return (if (picked == null) this else selectManifest(picked)) to heights
}

internal fun WatchStream.preferPlayableManifest(
    displayHdr: Boolean,
    ceiling: CodecCeiling = CodecCeiling.Auto,
): WatchStream {
    val best = orderedFor(displayHdr, ceiling).firstOrNull() ?: return copy(manifestPicked = true)
    return selectManifest(best)
}

internal fun WatchStream.nextPlayableManifest(
    displayHdr: Boolean,
    ceiling: CodecCeiling = CodecCeiling.Auto,
): WatchStream? {
    val order = orderedFor(displayHdr, ceiling)
    val next = order.getOrNull(order.indexOfFirst { it.url == url } + 1) ?: return null
    return selectManifest(next)
}

internal fun List<WatchManifest>.underCeiling(
    ceiling: CodecCeiling,
    tier: (WatchManifest) -> DecoderTier,
): List<WatchManifest> {
    fun present(codec: WatchCodec) = filter { it.codec == codec && tier(it) != DecoderTier.None }
    return when (ceiling) {
        CodecCeiling.Auto -> this
        CodecCeiling.Av1 -> {
            val av1 = present(WatchCodec.Av1)
            if (av1.isEmpty()) this else av1 + filter { it.codec != WatchCodec.Av1 }
        }
        CodecCeiling.Vp9 -> {
            val rest = filter { it.codec != WatchCodec.Av1 }
            val vp9 = present(WatchCodec.Vp9)
            when {
                vp9.isNotEmpty() -> vp9 + rest.filter { it.codec != WatchCodec.Vp9 }
                rest.isNotEmpty() -> rest
                else -> this
            }
        }
        CodecCeiling.H264 -> filter { it.codec == WatchCodec.H264 }.ifEmpty { this }
    }
}

private fun WatchStream.orderedFor(displayHdr: Boolean, ceiling: CodecCeiling): List<WatchManifest> {
    val tier = { manifest: WatchManifest -> manifest.decoderTier() }
    val order = if (ceiling == CodecCeiling.Auto) {
        manifests.playableOrder(tier)
    } else {
        manifests.underCeiling(ceiling, tier)
    }
    if (displayHdr) return order
    val allowed = order.filter { manifest ->
        val text = fetchManifest(manifest.url) ?: return@filter true
        manifestRange(text).playsOn(displayHdr = false)
    }
    return allowed.ifEmpty { order }
}

internal fun WatchStream.withCodec(codec: WatchCodec, kind: WatchKind): WatchStream {
    val picked = manifests.firstOrNull { it.codec == codec && it.kind == kind } ?: return this
    return selectManifest(picked)
}

internal fun WatchStream.selectManifest(picked: WatchManifest): WatchStream {
    val rest = manifests.filterNot { it.url == picked.url }
    return copy(
        url = picked.url,
        kind = picked.kind,
        alternateUrl = rest.firstOrNull()?.url,
        alternateKind = rest.firstOrNull()?.kind,
        manifestPicked = true,
    )
}

internal fun WatchStream.fallbackManifest(): WatchStream? {
    val other = alternateUrl ?: return null
    val otherKind = alternateKind ?: return null
    return copy(
        url = other,
        kind = otherKind,
        alternateUrl = null,
        alternateKind = null,
        manifestPicked = true,
    )
}

internal fun manifestHeights(manifest: String): Set<Int> {
    val heights = LinkedHashSet<Int>()
    hlsResolution.findAll(manifest).forEach { match ->
        match.groupValues[2].toIntOrNull()?.takeIf { it > 0 }?.let(heights::add)
    }
    dashHeight.findAll(manifest).forEach { match ->
        match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }?.let(heights::add)
    }
    return heights
}

private fun fetchText(url: String, pageUrl: String): String? = runCatching {
    val origin = pageUrl.toHttpUrlOrNull()
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", LIFT_USER_AGENT)
        .apply {
            if (origin != null) header("Referer", "${origin.scheme}://${origin.host}/")
        }
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        response.body?.string()
    }
}.getOrNull()

private fun mediaPlaylist(master: String, text: String): String? {
    val base = master.toHttpUrlOrNull() ?: return null
    val lines = text.lines()
    val variants = ArrayList<Pair<Int, String>>()
    for (index in lines.indices) {
        val line = lines[index].trim()
        if (!line.startsWith("#EXT-X-STREAM-INF")) continue
        val height = hlsResolution.find(line)?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val next = lines.getOrNull(index + 1)?.trim().orEmpty()
        if (next.isEmpty() || next.startsWith("#")) continue
        val absolute = base.resolve(next)?.toString() ?: continue
        variants += height to absolute
    }
    if (variants.isEmpty()) return null
    return variants.filter { it.first in 1..720 }.maxByOrNull { it.first }?.second
        ?: variants.minByOrNull { it.first }?.second
}

private fun audioPlaylist(master: String, text: String): String? {
    val base = master.toHttpUrlOrNull() ?: return null
    val line = text.lineSequence().firstOrNull { row ->
        row.startsWith("#EXT-X-MEDIA") &&
            "TYPE=AUDIO" in row &&
            "DEFAULT=YES" in row &&
            "failover" !in row
    } ?: return null
    val uri = Regex("""URI="([^"]+)"""").find(line)?.groupValues?.get(1) ?: return null
    return base.resolve(uri)?.toString()
}

internal fun playlistHeights(url: String): List<Int> =
    fetchHeights(url).filter { it > 0 }.sortedDescending()

private fun fetchManifest(url: String): String? = runCatching {
    val request = Request.Builder().url(url).header("User-Agent", LIFT_USER_AGENT).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        response.body?.string()
    }
}.getOrNull()

private fun hlsRange(text: String): ManifestRange {
    val lines = text.lines().filter { it.contains("#EXT-X-STREAM-INF") }
    if (lines.isEmpty()) return ManifestRange(sdr = true, hdr = false)
    var sdr = false
    var hdr = false
    for (line in lines) {
        val range = videoRange.find(line)?.groupValues?.get(1)?.uppercase()
        val dolby = line.split("CODECS=", limit = 2).getOrNull(1)?.let { codecsHaveDolby(it) } == true
        if (range == "PQ" || range == "HLG" || dolby) hdr = true else sdr = true
    }
    return ManifestRange(sdr, hdr)
}

private fun dashRange(text: String): ManifestRange {
    val sets = text.split("<AdaptationSet").drop(1)
    var sdr = false
    var hdr = false
    for (raw in sets) {
        val body = raw.substringBefore("</AdaptationSet")
        if (!body.isVideoSet()) continue
        if (body.isHdrSet()) hdr = true else sdr = true
    }
    if (!sdr && !hdr) return ManifestRange(sdr = true, hdr = false)
    return ManifestRange(sdr, hdr)
}

private fun String.isVideoSet(): Boolean {
    val lower = lowercase()
    if ("audio/" in lower && "video/" !in lower) return false
    if ("video/" in lower) return true
    return codecTokens(this).any { it.isVideoCodec() }
}

private fun String.isHdrSet(): Boolean {
    if (transferHdr.containsMatchIn(this) || transferHdrReversed.containsMatchIn(this)) return true
    return codecTokens(this).any { token ->
        token.startsWith("dvh1") || token.startsWith("dvhe") || token.startsWith("dav1")
    }
}

private fun codecTokens(body: String): List<String> =
    codecsAttr.findAll(body).flatMap { match ->
        match.groupValues[1].split(',').map { it.trim().lowercase() }
    }.toList()

private fun String.isVideoCodec(): Boolean =
    startsWith("avc1") || startsWith("avc3") || startsWith("hvc1") || startsWith("hev1") ||
        startsWith("dvh1") || startsWith("dvhe") || startsWith("dav1") || startsWith("av01") ||
        startsWith("vp09") || startsWith("vp9")

private fun codecsHaveDolby(raw: String): Boolean =
    raw.lowercase().split(',').any { token ->
        val codec = token.trim().trim('"')
        codec.startsWith("dvh1") || codec.startsWith("dvhe") || codec.startsWith("dav1")
    }

private fun fetchHeights(url: String): Set<Int> = runCatching {
    val request = Request.Builder().url(url).header("User-Agent", LIFT_USER_AGENT).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptySet()
        manifestHeights(response.peekBody(400_000).string())
    }
}.getOrDefault(emptySet())

private val hlsResolution = Regex("""RESOLUTION=(\d+)x(\d+)""")
private val dashHeight = Regex("""height\s*=\s*["'](\d+)["']""")
private val videoRange = Regex("""VIDEO-RANGE=(SDR|PQ|HLG)""", RegexOption.IGNORE_CASE)
private val codecsAttr = Regex("""codecs\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val transferHdr = Regex(
    """TransferCharacteristics["'][^>]*\bvalue\s*=\s*["'](16|18)["']""",
    RegexOption.IGNORE_CASE,
)
private val transferHdrReversed = Regex(
    """\bvalue\s*=\s*["'](16|18)["'][^>]*TransferCharacteristics""",
    RegexOption.IGNORE_CASE,
)

private fun audioFromHtml(html: String): Pair<List<String>, List<Int>> {
    for (match in audioKey.findAll(html)) {
        val start = html.indexOf('{', match.range.first)
        val raw = extractJsonObject(html, start) ?: continue
        val parsed = runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()?.trackNames()
        if (parsed != null && parsed.first.isNotEmpty()) return parsed
    }
    return emptyList<String>() to emptyList()
}

private fun JsonObject.trackNames(): Pair<List<String>, List<Int>> {
    val names = (this["names"] as? JsonArray).orEmpty().mapNotNull { item ->
        (item as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }
    val order = (this["order"] as? JsonArray).orEmpty().mapNotNull { item ->
        val value = item as? JsonPrimitive ?: return@mapNotNull null
        value.intOrNull ?: value.contentOrNull?.toIntOrNull()
    }
    return names to order
}

private fun extractJsonObject(html: String, start: Int): String? = extractJson(html, start, '{', '}')

private fun extractJsonArray(html: String, start: Int): String? = extractJson(html, start, '[', ']')

private fun extractJson(html: String, start: Int, open: Char, close: Char): String? {
    if (start < 0 || html.getOrNull(start) != open) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (index in start until html.length) {
        val char = html[index]
        if (inString) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            open -> depth++
            close -> {
                depth--
                if (depth == 0) return html.substring(start, index + 1)
            }
        }
    }
    return null
}

private fun JsonObject.string(key: String): String? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.contentOrNull?.cleanUrl()?.takeIf { it.isNotBlank() }
}

private fun JsonObject.int(key: String): Int? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.intOrNull ?: value.contentOrNull?.toIntOrNull()
}

private val unicodeEscape = Regex("""\\u([0-9a-fA-F]{4})""")

private fun String.cleanUrl(): String = replace("\\/", "/")
    .replace(unicodeEscape) { match -> match.groupValues[1].toInt(16).toChar().toString() }
