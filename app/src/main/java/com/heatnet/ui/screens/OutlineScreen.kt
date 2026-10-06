package com.heatnet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.ui.AppContainer
import com.heatnet.ui.geometry.Geometry
import com.heatnet.ui.heatmap.formatValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineScreen(
    container: AppContainer,
    connectionType: ConnectionType,
    onBack: () -> Unit,
    onSessionCreated: (Long) -> Unit,
) {
    val vm = viewModel { OutlineViewModel(container.repository, connectionType) }
    val closeRadius = with(LocalDensity.current) { 28.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.closed) "Set the scale" else "Draw the room") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (vm.closed) {
                    "Tap the wall you can measure, then type its real length."
                } else {
                    "Tap each corner of the room in order. Tap the first corner again (or press Close shape) to finish."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            OutlineCanvas(vm, closeRadius, Modifier.fillMaxWidth().weight(1f))

            if (!vm.closed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = vm::undo, enabled = vm.corners.isNotEmpty()) { Text("Undo") }
                    OutlinedButton(onClick = vm::clear, enabled = vm.corners.isNotEmpty()) { Text("Clear") }
                    Button(onClick = vm::close, enabled = vm.corners.size >= 3) { Text("Close shape") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Straighten walls", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = vm.snap, onCheckedChange = { vm.snap = it })
                }
            } else {
                val scaled = vm.scaledOutline()
                OutlinedTextField(
                    value = vm.wallLengthText,
                    onValueChange = { vm.wallLengthText = it },
                    label = { Text("Length of the highlighted wall (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = vm.wallLengthText.isNotBlank() && scaled.isFailure,
                    supportingText = {
                        val text = scaled.fold(
                            onSuccess = {
                                val b = Geometry.bounds(it)
                                "Room is about ${formatValue(b.width)} × ${formatValue(b.height)} m"
                            },
                            onFailure = { e -> if (vm.wallLengthText.isBlank()) "" else e.message.orEmpty() },
                        )
                        Text(text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text("Session name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = vm::undo) { Text("Edit outline") }
                    Button(
                        onClick = { vm.create(onSessionCreated) },
                        enabled = scaled.isSuccess && !vm.creating,
                        modifier = Modifier.weight(1f),
                    ) { Text("Start mapping") }
                }
            }
        }
    }
}

@Composable
private fun OutlineCanvas(vm: OutlineViewModel, closeRadius: Float, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(
        modifier
            .padding(vertical = 4.dp)
            .pointerInput(vm) { detectTapGestures { vm.onTap(com.heatnet.data.model.Point(it.x, it.y), closeRadius) } },
    ) {
        drawRect(colors.surfaceContainerLow)
        val gridStep = 24.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(colors.outlineVariant, Offset(x, 0f), Offset(x, size.height), 1f)
            x += gridStep
        }
        var y = 0f
        while (y < size.height) {
            drawLine(colors.outlineVariant, Offset(0f, y), Offset(size.width, y), 1f)
            y += gridStep
        }

        val pts = vm.corners.map { Offset(it.x, it.y) }
        if (pts.isEmpty()) return@Canvas
        if (vm.closed) {
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                pts.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, colors.primaryContainer.copy(alpha = 0.5f))
            pts.indices.forEach { i ->
                val a = pts[i]
                val b = pts[(i + 1) % pts.size]
                val selected = i == vm.selectedWall
                drawLine(
                    if (selected) colors.primary else colors.onSurface,
                    a,
                    b,
                    if (selected) 8.dp.toPx() else 3.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        } else {
            for (i in 0 until pts.lastIndex) drawLine(colors.onSurface, pts[i], pts[i + 1], 3.dp.toPx(), StrokeCap.Round)
            if (pts.size >= 3) {
                drawLine(
                    colors.outline,
                    pts.last(),
                    pts.first(),
                    2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
                )
                drawCircle(colors.primary, closeRadius, pts.first(), style = Stroke(2.dp.toPx()))
            }
        }
        pts.forEachIndexed { i, p ->
            drawCircle(if (i == 0) colors.primary else colors.onSurface, 6.dp.toPx(), p)
        }
    }
}
