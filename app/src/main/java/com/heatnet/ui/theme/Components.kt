package com.heatnet.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/** Filled content card with a hairline border; the one card style used across the app. */
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
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** Large pill-shaped primary action. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(),
        elevation = null,
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Top bar that blends into the page background instead of using a tinted surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun heatNetTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)

/** Minimal hero: a soft heat field on a quiet surface, echoing what the real map looks like. */
@Composable
fun HeatHero(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val card = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, w, h, androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                ),
            )
        }
        clipPath(card) {
            drawRect(scheme.surfaceContainer)
            fun blob(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(
                brush = Brush.radialGradient(
                    listOf(c.copy(alpha = 0.7f), c.copy(alpha = 0f)),
                    center = Offset(w * cx, h * cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(w * cx, h * cy),
            )
            blob(0.18f, 0.7f, h * 0.7f, Color(0xFFF87171))
            blob(0.42f, 0.35f, h * 0.65f, Color(0xFFFCD34D))
            blob(0.72f, 0.65f, h * 0.75f, Color(0xFF34D399))
            blob(0.92f, 0.25f, h * 0.5f, Color(0xFFA3E635))
            listOf(0.18f to 0.7f, 0.42f to 0.35f, 0.72f to 0.65f, 0.92f to 0.25f).forEach { (x, y) ->
                drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(w * x, h * y))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatNetTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) = TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, colors = heatNetTopBarColors())
