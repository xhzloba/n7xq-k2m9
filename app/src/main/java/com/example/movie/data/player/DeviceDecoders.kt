package com.example.movie.data.player

import android.media.MediaCodecList

internal object DeviceDecoders {
    private val tiers: Map<String, DecoderTier> by lazy { scan() }

    fun tier(mime: String): DecoderTier = tiers[mime.lowercase()] ?: DecoderTier.None

    private fun scan(): Map<String, DecoderTier> {
        val found = HashMap<String, DecoderTier>()
        val infos = runCatching { MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos }.getOrNull() ?: return found
        for (info in infos) {
            if (info.isEncoder) continue
            val hardware = runCatching { info.isHardwareAccelerated && !info.isSoftwareOnly }.getOrDefault(false)
            val tier = if (hardware) DecoderTier.Hardware else DecoderTier.Software
            for (type in info.supportedTypes) {
                val key = type.lowercase()
                if (found[key] != DecoderTier.Hardware) found[key] = tier
            }
        }
        return found
    }
}

internal fun WatchManifest.decoderTier(): DecoderTier {
    val mime = when (codec) {
        WatchCodec.Av1 -> "video/av01"
        WatchCodec.Vp9 -> "video/x-vnd.on2.vp9"
        WatchCodec.H264 -> "video/avc"
    }
    return DeviceDecoders.tier(mime)
}
