package com.heatnet.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Compose ports of a few effects from reactbits.dev (a React library, so the components themselves
 * cannot be used in a native app): Gradient Text, Count Up, Fade Content / Animated Content,
 * Particles and Star Border.
 */

/** React Bits "Gradient Text": text filled with a gradient that slowly flows across it. */
@Composable
fun FlowingGradientText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gradient-text")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = SPAN,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "gradient-text-shift",
    )
    // First and last colours match and the tile mode repeats, so the loop has no visible seam.
    val brush = Brush.linearGradient(
        colors = listOf(Brand.Blue, Brand.Sky, Color.White, Brand.Sky, Brand.Blue),
        start = Offset(shift, 0f),
        end = Offset(shift + SPAN, 0f),
        tileMode = TileMode.Repeated,
    )
    Text(text, modifier, style = style.copy(brush = brush))
}

private const val SPAN = 700f

/** React Bits "Count Up": a number that rolls up from zero to [value]. */
@Composable
fun CountUpText(value: Int, style: TextStyle, modifier: Modifier = Modifier) {
    var start by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { start = value }
    val shown by animateIntAsState(start, tween(1100, easing = FastOutSlowInEasing), label = "count-up")
    Text("$shown", modifier, style = style)
}

/** React Bits "Fade Content": the block fades in while sliding up, after [delayMillis]. */
@Composable
fun Reveal(
    modifier: Modifier = Modifier.fillMaxWidth(),
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 28.dp.toPx()
        },
    ) { content() }
}

private class Mote(val x: Float, val y: Float, val speed: Float, val radius: Float, val phase: Float)

/** React Bits "Particles": small dots drifting upward and twinkling behind the content. */
@Composable
fun Particles(color: Color, modifier: Modifier = Modifier.fillMaxSize(), count: Int = 26) {
    val motes = remember {
        val random = Random(7)
        List(count) {
            Mote(
                x = random.nextFloat(),
                y = random.nextFloat(),
                speed = 0.3f + random.nextFloat() * 0.7f,
                radius = 1f + random.nextFloat() * 2f,
                phase = random.nextFloat(),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "particles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
        label = "particles-t",
    )
    Canvas(modifier) {
        motes.forEach { m ->
            val y = ((m.y - t * m.speed * 6f) % 1f + 1f) % 1f
            val x = m.x + 0.02f * sin((t * 8f + m.phase) * 2f * PI.toFloat())
            val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin((t * 40f * m.speed + m.phase) * 2f * PI.toFloat()))
            drawCircle(
                color = color.copy(alpha = color.alpha * twinkle),
                radius = m.radius.dp.toPx(),
                center = Offset(x * size.width, y * size.height),
            )
        }
    }
}

/** React Bits "Star Border": a bright streak that travels around the edge of a pill. */
fun Modifier.starBorder(enabled: Boolean, streak: Color = Color.White, thickness: Float = 2.5f): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "star-border")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "star-border-angle",
    )
    drawWithContent {
        drawContent()
        if (!enabled) return@drawWithContent
        val corner = CornerRadius(size.height / 2f)
        val stroke = thickness.dp.toPx()
        val ring = Path.combine(
            PathOperation.Difference,
            Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) },
            Path().apply {
                addRoundRect(
                    RoundRect(stroke, stroke, size.width - stroke, size.height - stroke, CornerRadius(size.height / 2f - stroke)),
                )
            },
        )
        val centre = Offset(size.width / 2f, size.height / 2f)
        clipPath(ring) {
            rotate(angle, pivot = centre) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.95f to streak,
                        1f to Color.Transparent,
                        center = centre,
                    ),
                    radius = size.width,
                    center = centre,
                )
            }
        }
    }
}
