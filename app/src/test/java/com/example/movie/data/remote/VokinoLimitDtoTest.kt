package com.example.movie.data.remote

import com.example.movie.data.mapper.kpId
import com.example.movie.data.remote.dto.TimelineDto
import com.example.movie.data.remote.dto.ViewResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VokinoLimitDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun viewLimitIsNotAnEmptyMovie() {
        val body = json.decodeFromString<ViewResponseDto>("""{"error":429,"message":"E_TOO_MANY_REQUESTS"}""")
        assertEquals(429, body.error)
        assertEquals("", body.details.name)
    }

    @Test
    fun normalViewHasNoLimit() {
        val body = json.decodeFromString<ViewResponseDto>("""{"details":{"id":"abc","name":"Фильм"}}""")
        assertNull(body.error)
        assertEquals("abc", body.details.id)
    }

    @Test
    fun timelineLimitHasNoKp() {
        val body = json.decodeFromString<TimelineDto>("""{"error":429,"message":"E_TOO_MANY_REQUESTS"}""")
        assertEquals(429, body.error)
        assertNull(body.kpId())
    }

    @Test
    fun timelineKeepsKp() {
        val body = json.decodeFromString<TimelineDto>("""{"kp_id":326}""")
        assertNull(body.error)
        assertEquals(326, body.kpId())
    }
}
