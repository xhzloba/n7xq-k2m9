package com.example.movie.data.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiftEmbedTest {
    @Test
    fun prefersHlsManifestAndCaptions() {
        val html = """
            source: {
                dash: "https://cdn.example/video.mpd?t=1",
                hls: "https://cdn.example/master.m3u8?t=1",
                cc: [{"url":"https://cdn.example/ru.vtt?t=1","name":"Рус. полные"}],
                audio: {"names":["Рус. Дублированный","LostFilm"],"order":[1,0]}
            }
        """.trimIndent()

        val playback = parseLiftEmbed(html)

        assertEquals("https://cdn.example/master.m3u8?t=1", playback?.stream?.url)
        assertEquals(WatchKind.Hls, playback?.stream?.kind)
        assertEquals(
            listOf(WatchSubtitle("https://cdn.example/ru.vtt?t=1", "Рус. полные")),
            playback?.stream?.subtitles,
        )
        assertTrue(playback?.episodes.orEmpty().isEmpty())
        assertEquals(listOf("Рус. Дублированный", "LostFilm"), playback?.stream?.audioNames)
        assertEquals(listOf(1, 0), playback?.stream?.audioOrder)
        assertEquals("https://cdn.example/video.mpd?t=1", playback?.stream?.alternateUrl)
        assertEquals(WatchKind.Dash, playback?.stream?.alternateKind)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8?t=1", WatchKind.Hls),
                WatchManifest("https://cdn.example/video.mpd?t=1", WatchKind.Dash),
            ),
            playback?.stream?.manifests,
        )
    }

    @Test
    fun parsesDashAv1AndVp9Candidates() {
        val html = """
            source: {
                hls: "https://cdn.example/master.m3u8",
                dash: "https://cdn.example/h264.mpd",
                dash_vp9: "https://cdn.example/vp9.mpd",
                dash_av1: "https://cdn.example/av1.mpd"
            }
        """.trimIndent()

        val stream = parseLiftEmbed(html)?.stream

        assertEquals("https://cdn.example/master.m3u8", stream?.url)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8", WatchKind.Hls, WatchCodec.H264),
                WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
                WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
                WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash, WatchCodec.H264),
            ),
            stream?.manifests,
        )
    }

    @Test
    fun playlistApiNestedStreamsPreferAv1Vp9Hls() {
        val raw = """
            {
              "title":"Мстители",
              "streams":{
                "hls":"https://cdn.example/master.m3u8",
                "dash_vp9":"https://cdn.example/vp9.mpd",
                "dash_av1":"https://cdn.example/av1.mpd"
              },
              "audio":{"names":["Рус"],"order":[0]},
              "cc":[{"url":"https://cdn.example/ru.vtt","name":"Рус"}]
            }
        """.trimIndent()

        val stream = parseLiftPlaylist(raw)?.stream

        assertEquals("https://cdn.example/master.m3u8", stream?.url)
        assertEquals(WatchKind.Hls, stream?.kind)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/master.m3u8", WatchKind.Hls, WatchCodec.H264),
                WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
                WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
            ),
            stream?.manifests,
        )
        assertEquals(listOf("Рус"), stream?.audioNames)
        assertEquals("https://cdn.example/ru.vtt", stream?.subtitles?.single()?.url)
    }

    @Test
    fun playlistInfosCropTheActivePicture() {
        val raw = """
            {
              "streams":{"hls":"https://cdn.example/master.m3u8","dash_av1":"https://cdn.example/av1.mpd"},
              "infos":{
                "hls":{
                  "720":{"width":"1280","height":"720","active_x":"0","active_y":"94","active_width":"1280","active_height":"532","active_has_crop":"1"},
                  "eng4":{"bitrate":"1","audio_codec":"aac"}
                },
                "dash_av1":{
                  "1080":{"width":"1920","height":"1080","active_x":"0","active_y":"140","active_width":"1920","active_height":"800","active_has_crop":"1"},
                  "4K":{"width":"3840","height":"2160","active_x":"0","active_y":"280","active_width":"3840","active_height":"1600","active_has_crop":"0"}
                }
              }
            }
        """.trimIndent()

        val stream = parseLiftPlaylist(raw)?.stream
        assertEquals(532, stream?.frameCrop(720)?.activeHeight)
        val av1 = stream?.selectManifest(WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1))
        assertEquals(800, av1?.frameCrop(1080)?.activeHeight)
        assertEquals(800, av1?.frameCrop(2160)?.activeHeight)
    }

    @Test
    fun playlistApiSeriesEpisodeStreams() {
        val raw = """
            {
              "title":"Игра престолов",
              "seasons":[{
                "season":5,
                "episodes":[{
                  "episode":"1",
                  "streams":{
                    "hls":"https://cdn.example/s5e1.m3u8",
                    "dash_vp9":"https://cdn.example/s5e1-vp9.mpd",
                    "dash_av1":"https://cdn.example/s5e1-av1.mpd"
                  },
                  "audio":{"names":["LostFilm"],"order":[0]}
                }]
              }]
            }
        """.trimIndent()

        val playback = parseLiftPlaylist(raw, season = 5, episode = 1)

        assertEquals("https://cdn.example/s5e1.m3u8", playback?.stream?.url)
        assertEquals(
            listOf(
                WatchManifest("https://cdn.example/s5e1.m3u8", WatchKind.Hls, WatchCodec.H264),
                WatchManifest("https://cdn.example/s5e1-av1.mpd", WatchKind.Dash, WatchCodec.Av1),
                WatchManifest("https://cdn.example/s5e1-vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
            ),
            playback?.stream?.manifests,
        )
        assertEquals(listOf(5 to 1), playback?.episodes?.map { it.season to it.episode })
    }

    @Test
    fun dashManifestKeepsHeightsHlsDrops() {
        val hls = "#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1272x530\n"
        val dash = """<Representation height="2160"/><Representation height="1080"/><Representation height="720"/>"""
        assertEquals(setOf(530), manifestHeights(hls))
        assertEquals(setOf(2160, 1080, 720), manifestHeights(dash))
    }

    @Test
    fun fallsBackToDash() {
        val playback = parseLiftEmbed("""dash: "https://cdn.example/a.mpd"""")
        assertEquals(WatchKind.Dash, playback?.stream?.kind)
        assertEquals("https://cdn.example/a.mpd", playback?.stream?.url)
    }

    @Test
    fun seriesPlaylistOpensCurrentEpisode() {
        val html = """
            current: { season: 1, episode: "2" },
            seasons:[{
              "season":1,
              "episodes":[
                {"episode":"1","dash":"https://cdn.example/e1.mpd","hls":"https://cdn.example/e1.m3u8","audio":{"names":["Кубик в кубе","Рус. Дублированный"],"order":[0,1]},"cc":[{"url":"https://cdn.example/e1.vtt","name":"Рус"}]},
                {"episode":"2","hls":"https://cdn.example/e2.m3u8"}
              ]
            },{
              "season":2,
              "episodes":[{"episode":1,"hls":"https://cdn.example/s2e1.m3u8"}]
            }]
        """.trimIndent()

        val playback = parseLiftEmbed(html)

        assertEquals("https://cdn.example/e2.m3u8", playback?.stream?.url)
        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1), playback?.episodes?.map { it.season to it.episode })
        assertEquals("https://cdn.example/e1.vtt", playback?.episodes?.first()?.stream?.subtitles?.single()?.url)
        assertEquals(listOf("Кубик в кубе", "Рус. Дублированный"), playback?.episodes?.first()?.stream?.audioNames)
        assertEquals(listOf(0, 1), playback?.episodes?.first()?.stream?.audioOrder)
    }

    @Test
    fun queryPicksRequestedEpisode() {
        val html = """
            seasons:[{"season":1,"episodes":[
              {"episode":"1","hls":"https://cdn.example/e1.m3u8"},
              {"episode":"3","hls":"https://cdn.example/e3.m3u8"}
            ]}]
        """.trimIndent()

        val playback = parseLiftEmbed(html, season = 1, episode = 3)

        assertEquals("https://cdn.example/e3.m3u8", playback?.stream?.url)
    }

    @Test
    fun autoPrefersHardwareAv1OverSoftwareVp9() {
        val manifests = listOf(
            WatchManifest("https://cdn.example/master.m3u8", WatchKind.Hls, WatchCodec.H264),
            WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
            WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
        )
        val order = manifests.playableOrder { manifest ->
            when (manifest.codec) {
                WatchCodec.Av1 -> DecoderTier.Hardware
                WatchCodec.Vp9 -> DecoderTier.Software
                WatchCodec.H264 -> DecoderTier.Hardware
            }
        }
        assertEquals(listOf("https://cdn.example/av1.mpd", "https://cdn.example/master.m3u8"), order.map { it.url })
    }

    @Test
    fun autoFallsToSoftwareWhenDeviceHasNoHardware() {
        val manifests = listOf(
            WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash, WatchCodec.H264),
            WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
            WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
        )
        val order = manifests.playableOrder { DecoderTier.Software }
        assertEquals(
            listOf("https://cdn.example/av1.mpd", "https://cdn.example/vp9.mpd", "https://cdn.example/h264.mpd"),
            order.map { it.url },
        )
    }

    @Test
    fun hlsPqAndSdrBothCount() {
        val text = """
            #EXT-X-STREAM-INF:BANDWIDTH=1,RESOLUTION=1920x1080,VIDEO-RANGE=PQ,CODECS="hvc1.2.4.L153.B0"
            video.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1,RESOLUTION=1280x720,VIDEO-RANGE=SDR,CODECS="avc1.64001f"
            sdr.m3u8
        """.trimIndent()
        val range = manifestRange(text)
        assertEquals(true, range.sdr)
        assertEquals(true, range.hdr)
        assertEquals(true, range.playsOn(displayHdr = false))
    }

    @Test
    fun hlsPqOnlyIsRejectedOnSdrScreen() {
        val text = """
            #EXT-X-STREAM-INF:BANDWIDTH=1,RESOLUTION=1920x1080,VIDEO-RANGE=PQ
            video.m3u8
        """.trimIndent()
        val range = manifestRange(text)
        assertEquals(false, range.sdr)
        assertEquals(true, range.hdr)
        assertEquals(false, range.playsOn(displayHdr = false))
        assertEquals(true, range.playsOn(displayHdr = true))
    }

    @Test
    fun dashHdrOnlyAndMixed() {
        val hdrOnly = """
            <AdaptationSet mimeType="video/mp4" codecs="hvc1.2.4.L153.B0">
              <EssentialProperty schemeIdUri="urn:mpeg:mpegB:cicp:TransferCharacteristics" value="16"/>
              <Representation height="2160"/>
            </AdaptationSet>
        """.trimIndent()
        val mixed = hdrOnly + """
            <AdaptationSet mimeType="video/mp4" codecs="avc1.640028">
              <Representation height="1080"/>
            </AdaptationSet>
        """.trimIndent()
        val dolby = """
            <AdaptationSet mimeType="video/mp4" codecs="dvh1.05.01">
              <Representation height="2160"/>
            </AdaptationSet>
            <AdaptationSet mimeType="audio/mp4" codecs="mp4a.40.2">
              <Representation id="a"/>
            </AdaptationSet>
        """.trimIndent()
        assertEquals(false, manifestRange(hdrOnly).playsOn(displayHdr = false))
        assertEquals(true, manifestRange(mixed).playsOn(displayHdr = false))
        assertEquals(false, manifestRange(dolby).sdr)
        assertEquals(true, manifestRange(dolby).hdr)
    }

    @Test
    fun av1CeilingKeepsSoftwareAv1First() {
        val manifests = listOf(
            WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash, WatchCodec.H264),
            WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
            WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
        )
        val order = manifests.underCeiling(CodecCeiling.Av1) { manifest ->
            if (manifest.codec == WatchCodec.Av1) DecoderTier.Software else DecoderTier.Hardware
        }
        assertEquals("https://cdn.example/av1.mpd", order.first().url)
    }

    @Test
    fun vp9CeilingDropsAv1() {
        val manifests = listOf(
            WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
            WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
            WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash, WatchCodec.H264),
        )
        val order = manifests.underCeiling(CodecCeiling.Vp9) { DecoderTier.Hardware }
        assertEquals(
            listOf("https://cdn.example/vp9.mpd", "https://cdn.example/h264.mpd"),
            order.map { it.url },
        )
    }

    @Test
    fun castAutoStaysOnH264UntilTheHeightNeedsAnotherCodec() {
        val manifests = listOf(
            WatchManifest("https://cdn.example/av1.mpd", WatchKind.Dash, WatchCodec.Av1),
            WatchManifest("https://cdn.example/h264.mpd", WatchKind.Dash, WatchCodec.H264),
            WatchManifest("https://cdn.example/vp9.mpd", WatchKind.Dash, WatchCodec.Vp9),
        )
        val heights = mapOf(
            "https://cdn.example/av1.mpd" to setOf(2160, 1080),
            "https://cdn.example/h264.mpd" to setOf(1080, 720),
            "https://cdn.example/vp9.mpd" to setOf(2160, 1080),
        )
        val tier = { _: WatchManifest -> DecoderTier.Hardware }
        val of = { manifest: WatchManifest -> heights.getValue(manifest.url) }
        assertEquals(
            "https://cdn.example/h264.mpd",
            pickCastManifest(manifests, CodecCeiling.Auto, null, of, tier)?.url,
        )
        assertEquals(
            "https://cdn.example/h264.mpd",
            pickCastManifest(manifests, CodecCeiling.Auto, 1080, of, tier)?.url,
        )
        assertEquals(
            "https://cdn.example/vp9.mpd",
            pickCastManifest(manifests, CodecCeiling.Auto, 2160, of, tier)?.url,
        )
    }

    @Test
    fun ignoresPageWithoutManifest() {
        assertNull(parseLiftEmbed("<html>no player</html>"))
    }
}
