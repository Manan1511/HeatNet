package com.heatnet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand colours. The heatmap scale (red to green) is separate so the data colours stay unambiguous. */
object Brand {
    val Cyan = Color(0xFF22D3EE)
    val Teal = Color(0xFF2DD4BF)
    val Violet = Color(0xFF8B5CF6)
    val Pink = Color(0xFFF472B6)
    val Amber = Color(0xFFFBBF24)

    /** Signature gradient used on primary actions, the logo word and highlights. */
    val gradient: Brush = Brush.linearGradient(listOf(Cyan, Violet))

    /** Warmer variant for emphasis moments. */
    val warmGradient: Brush = Brush.linearGradient(listOf(Violet, Pink))
}

private val DarkColors = darkColorScheme(
    primary = Brand.Teal,
    onPrimary = Color(0xFF03211D),
    primaryContainer = Color(0xFF0F3B37),
    onPrimaryContainer = Color(0xFFB8F5EC),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1048),
    secondaryContainer = Color(0xFF2E2063),
    onSecondaryContainer = Color(0xFFE4DBFF),
    tertiary = Brand.Amber,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF4A3600),
    onTertiaryContainer = Color(0xFFFFE9A8),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFEAF0FF),
    surface = Color(0xFF070B14),
    onSurface = Color(0xFFEAF0FF),
    surfaceVariant = Color(0xFF172036),
    onSurfaceVariant = Color(0xFF93A1C0),
    surfaceContainerLowest = Color(0xFF050810),
    surfaceContainerLow = Color(0xFF0C1322),
    surfaceContainer = Color(0xFF0F182B),
    surfaceContainerHigh = Color(0xFF172238),
    surfaceContainerHighest = Color(0xFF1E2B45),
    outline = Color(0xFF5F6E8F),
    outlineVariant = Color(0xFF26324D),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E9F8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1F7F1),
    onPrimaryContainer = Color(0xFF053B35),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = Color(0xFF2B1066),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF0C7),
    onTertiaryContainer = Color(0xFF452A03),
    background = Color(0xFFF3F6FC),
    onBackground = Color(0xFF0B1220),
    surface = Color(0xFFF3F6FC),
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE3E9F5),
    onSurfaceVariant = Color(0xFF55627F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEAEFF9),
    surfaceContainerHighest = Color(0xFFDFE6F4),
    outline = Color(0xFF8A96B2),
    outlineVariant = Color(0xFFD8E0F0),
    error = Color(0xFFB42318),
)

private val base = TextStyle(fontFamily = FontFamily.SansSerif)

private val AppTypography = Typography(
    displayMedium = base.copy(fontSize = 48.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp),
    displaySmall = base.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
    headlineMedium = base.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
    labelMedium = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    labelSmall = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun HeatNetTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
