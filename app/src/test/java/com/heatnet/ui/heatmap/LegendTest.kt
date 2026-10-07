package com.heatnet.ui.heatmap

import org.junit.Assert.assertEquals
import org.junit.Test

class LegendTest {

    @Test
    fun formatValueWithAbsoluteValueLessThanTenIncludesOneDecimalPlace() {
        assertEquals("0.0", formatValue(0f))
        assertEquals("0.5", formatValue(0.5f))
        assertEquals("9.9", formatValue(9.9f))
        assertEquals("-0.5", formatValue(-0.5f))
        assertEquals("-9.9", formatValue(-9.9f))
    }

    @Test
    fun formatValueWithAbsoluteValueGreaterOrEqualTenFormatsAsInteger() {
        assertEquals("10", formatValue(10f))
        assertEquals("25", formatValue(25.3f))
        assertEquals("100", formatValue(100f))
        assertEquals("-10", formatValue(-10f))
        assertEquals("-25", formatValue(-25.3f))
        assertEquals("-100", formatValue(-100f))
    }

    @Test
    fun formatValueBoundaryValues() {
        assertEquals("9.9", formatValue(9.9f))
        assertEquals("10", formatValue(10.0f))
        assertEquals("-9.9", formatValue(-9.9f))
        assertEquals("-10", formatValue(-10.0f))
    }
}
