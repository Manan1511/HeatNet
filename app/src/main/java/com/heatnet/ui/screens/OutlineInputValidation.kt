package com.heatnet.ui.screens

internal object OutlineInputValidation {
    fun parseWallLength(text: String, minMetres: Float, maxMetres: Float): Result<Float> {
        val length = text.trim().replace(',', '.').toFloatOrNull()
            ?: return Result.failure(IllegalArgumentException("Enter the wall's length in metres."))
        if (!length.isFinite()) {
            return Result.failure(IllegalArgumentException("Enter a finite wall length."))
        }
        if (length < minMetres || length > maxMetres) {
            return Result.failure(
                IllegalArgumentException("Enter a length between $minMetres and $maxMetres m."),
            )
        }
        return Result.success(length)
    }
}
