package com.heatnet.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlineCodecTest {
    @Test
    fun `outline survives a round trip`() {
        val outline = listOf(Point(0f, 0f), Point(4.5f, 0f), Point(4.5f, 3.25f), Point(0f, 3.25f))
        assertEquals(outline, OutlineCodec.decode(OutlineCodec.encode(outline)))
    }

    @Test
    fun `blank text decodes to an empty outline`() {
        assertTrue(OutlineCodec.decode("").isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed point is rejected`() {
        OutlineCodec.decode("1,2;3")
    }
}
