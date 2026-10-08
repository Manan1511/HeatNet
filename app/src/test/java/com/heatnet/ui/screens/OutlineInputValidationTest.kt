package com.heatnet.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlineInputValidationTest {
    @Test
    fun rejectsNonFiniteWallLengthsAsValidationFailures() {
        listOf("NaN", "Infinity", "-Infinity").forEach { value ->
            val result = OutlineInputValidation.parseWallLength(value, minMetres = 0.5f, maxMetres = 50f)

            assertTrue("$value must be rejected", result.isFailure)
        }
    }

    @Test
    fun parsesDecimalCommaAndKeepsExistingLengthBounds() {
        assertEquals(4.25f, OutlineInputValidation.parseWallLength("4,25", 0.5f, 50f).getOrThrow(), 0.001f)
        assertTrue(OutlineInputValidation.parseWallLength("0.2", 0.5f, 50f).isFailure)
        assertTrue(OutlineInputValidation.parseWallLength("51", 0.5f, 50f).isFailure)
    }
}
