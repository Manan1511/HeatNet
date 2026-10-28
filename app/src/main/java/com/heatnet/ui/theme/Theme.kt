package com.heatnet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand colours. The heatmap scale (red to green) is separate so the data colours stay unambiguous. */
object Brand {
    val Sky = Color(0xFF38BDF8)
    val Blue = Color(0xFF3B82F6)
    val Indigo = Color(0xFF6366F1)
    val Amber = Color(0xFFFBBF24)

    /** Signature gradient used on primary actions, the logo word and highlights. */
    val gradient: Brush = Brush.linearGradient(listOf(Sky, Blue))

    /** Deeper variant for selected states and emphasis. */
    val deepGradient: Brush = Brush.linearGradient(listOf(Blue, Indigo))
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF021B3A),
    primaryContainer = Color(0xFF0E2A55),
    onPrimaryContainer = Color(0xFFCFE4FF),
    secondary = Color(0xFF8AA4FF),
    onSecondary = Color(0xFF0E1A4D),
    secondaryContainer = Color(0xFF1B2A6B),
    onSecondaryContainer = Color(0xFFDCE4FF),
    tertiary = Brand.Amber,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF4A3600),
    onTertiaryContainer = Color(0xFFFFE9A8),
    background = Color(0xFF040A1C),
    onBackground = Color(0xFFE8F0FF),
    surface = Color(0xFF040A1C),
    onSurface = Color(0xFFE8F0FF),
    surfaceVariant = Color(0xFF15264F),
    onSurfaceVariant = Color(0xFF8FA3CC),
    surfaceContainerLowest = Color(0xFF030715),
    surfaceContainerLow = Color(0xFF081230),
    surfaceContainer = Color(0xFF0C1838),
    surfaceContainerHigh = Color(0xFF132246),
    surfaceContainerHighest = Color(0xFF1A2E5A),
    outline = Color(0xFF5870A0),
    outlineVariant = Color(0xFF20325E),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D5FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF06214F),
    secondary = Color(0xFF4F5BD5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E7FF),
    onSecondaryContainer = Color(0xFF111B5E),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF0C7),
    onTertiaryContainer = Color(0xFF452A03),
    background = Color(0xFFEEF4FF),
    onBackground = Color(0xFF071330),
    surface = Color(0xFFEEF4FF),
    onSurface = Color(0xFF071330),
    surfaceVariant = Color(0xFFDDE7FA),
    onSurfaceVariant = Color(0xFF4D6086),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAFF),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFE6EEFC),
    surfaceContainerHighest = Color(0xFFD9E4F8),
    outline = Color(0xFF7D90B8),
    outlineVariant = Color(0xFFD3E0FA),
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
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
    ) {
        // Screens use transparent Scaffolds over the aurora background, which would otherwise leave
        // default text black (unreadable in dark mode). Set the base text colour for the whole app.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
