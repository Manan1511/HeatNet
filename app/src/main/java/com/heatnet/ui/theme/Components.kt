package com.heatnet.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Rounded content card with a hairline border; the one card style used across the app. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** Large pill-shaped primary action filled with the brand gradient. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val fill: Brush = if (enabled) Brand.gradient else SolidColor(scheme.surfaceVariant)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(brush = fill, shape = CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color(0xFF04101C) else scheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val border = Brush.linearGradient(listOf(Brand.Cyan.copy(alpha = 0.55f), Brand.Violet.copy(alpha = 0.55f)))
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = CircleShape,
        border = BorderStroke(1.2.dp, border),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Small rounded label with a coloured dot, optionally pulsing like a live indicator. */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    pulsing: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "pill")
    val glow by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pill-glow",
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(dotColor.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = if (pulsing) glow else 1f)),
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = dotColor)
    }
}

/** Full-screen backdrop: the page colour with slow drifting colour glows, like an aurora. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora-t",
    )
    Box(modifier.fillMaxSize().background(scheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strength = if (dark) 0.34f else 0.22f
            fun glow(cx: Float, cy: Float, radius: Float, c: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.copy(alpha = strength), c.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }
            glow(w * (0.12f + 0.25f * t), h * (0.08f + 0.05f * t), w * 0.95f, Brand.Violet)
            glow(w * (0.95f - 0.25f * t), h * (0.34f + 0.10f * t), w * 0.8f, Brand.Cyan)
            glow(w * (0.25f + 0.2f * t), h * (0.98f - 0.1f * t), w * 0.9f, Brand.Pink)
        }
        content()
    }
}

/**
 * Animated hero: a miniature room with signal "pings" rippling out from four measurement
 * points over drifting heat colours, echoing what the real map looks like.
 */
@Composable
fun HeatHero(modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 200.dp) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "hero-pulse",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hero-drift",
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val corner = 32.dp.toPx()
        val card = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(corner))) }
        clipPath(card) {
            drawRect(scheme.surfaceContainer)

            // faint floor-plan grid
            val gridStep = 28.dp.toPx()
            var gx = gridStep
            while (gx < w) {
                drawLine(scheme.outlineVariant.copy(alpha = 0.35f), Offset(gx, 0f), Offset(gx, h), 1.dp.toPx())
                gx += gridStep
            }
            var gy = gridStep
            while (gy < h) {
                drawLine(scheme.outlineVariant.copy(alpha = 0.35f), Offset(0f, gy), Offset(w, gy), 1.dp.toPx())
                gy += gridStep
            }

            fun blob(cx: Float, cy: Float, radius: Float, c: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.copy(alpha = 0.62f), c.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                )
            }
            val shift = (drift - 0.5f) * 0.08f
            blob(w * (0.18f + shift), h * 0.70f, h * 0.75f, Color(0xFFF87171))
            blob(w * (0.42f - shift), h * 0.32f, h * 0.70f, Color(0xFFFCD34D))
            blob(w * (0.72f + shift), h * 0.66f, h * 0.80f, Color(0xFF34D399))
            blob(w * (0.92f - shift), h * 0.24f, h * 0.55f, Color(0xFF22D3EE))

            val points = listOf(0.18f to 0.70f, 0.42f to 0.32f, 0.72f to 0.66f, 0.92f to 0.24f)
            points.forEachIndexed { index, (px, py) ->
                val centre = Offset(w * px, h * py)
                val p = (pulse + index * 0.25f) % 1f
                drawCircle(
                    color = Color.White,
                    radius = p * 56.dp.toPx(),
                    center = centre,
                    alpha = (1f - p) * 0.6f,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(Color.White, radius = 5.dp.toPx(), center = centre)
                drawCircle(
                    Color(0xFF0B1220),
                    radius = 5.dp.toPx(),
                    center = centre,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        drawRoundRect(
            color = scheme.outlineVariant,
            cornerRadius = CornerRadius(corner),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** Top bar that blends into the page instead of using a tinted surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun heatNetTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatNetTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) = TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, colors = heatNetTopBarColors())
