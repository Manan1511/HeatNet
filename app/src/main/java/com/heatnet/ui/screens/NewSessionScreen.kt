package com.heatnet.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.ui.AppContainer
import com.heatnet.ui.theme.AuroraBackground
import com.heatnet.ui.theme.Brand
import com.heatnet.ui.theme.HeatNetTopBar
import com.heatnet.ui.theme.PrimaryButton
import com.heatnet.ui.theme.SignalBarsGlyph
import com.heatnet.ui.theme.StatusPill
import com.heatnet.ui.theme.SurfaceCard
import com.heatnet.ui.theme.WifiGlyph
import com.heatnet.ui.theme.statusGreen

/**
 * Step 1 of a session: choose Wi-Fi or mobile data. The app checks which one the phone is really
 * using (Member 1's [ConnectionMonitor]) and only lets you continue on that connection, so a
 * session is locked to one transport (F2). Wi-Fi sessions are blocked until location permission
 * is granted, because Android hides Wi-Fi details without it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(container: AppContainer, onBack: () -> Unit, onContinue: (ConnectionType) -> Unit) {
    val context = LocalContext.current
    val fake = container.useFakeMeasurements

    fun locationGranted() =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    var hasLocation by remember { mutableStateOf(locationGranted()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var onlyApproximate by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        hasLocation = locationGranted()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasLocation = WifiLocationPermissionPolicy.hasRequiredPermission(grants)
        onlyApproximate = WifiLocationPermissionPolicy.isApproximateOnly(grants)
        askedOnce = true
    }

    val snapshot by container.connectionMonitor.state.collectAsStateWithLifecycle()
    // The connection the phone is really using right now (none in demo mode).
    val active: ConnectionType? = if (fake) null else snapshot.connectionType
    var chosen by rememberSaveable { mutableStateOf<ConnectionType?>(null) }
    val selected: ConnectionType? = chosen ?: if (fake) ConnectionType.WIFI else active
    val canMeasureSelected = selected != null && (fake || active == selected)
    val needsLocation = !fake && selected == ConnectionType.WIFI && canMeasureSelected && !hasLocation

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HeatNetTopBar(
                    title = { Text("New session") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Choose your connection", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Pick what you want to map. The whole session stays on that connection, so Wi-Fi and " +
                        "mobile data readings are never mixed in one map.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ConnectionType.entries.forEach { type ->
                    val live = !fake && active == type
                    ConnectionOptionCard(
                        type = type,
                        selected = selected == type,
                        statusText = when {
                            fake -> "Demo"
                            live -> "Connected now"
                            else -> "Not active"
                        },
                        live = live || fake,
                        onClick = { chosen = type },
                    )
                }

                if (fake) {
                    StatusPill("Demo mode: readings are simulated", dotColor = Brand.Amber)
                } else {
                    val note = when {
                        !snapshot.hasTransportObservation -> "Checking your connection…"
                        snapshot.network == null -> "You're not connected to a network."
                        active == null -> "Your current connection isn't Wi-Fi or mobile data."
                        selected != null && active != selected ->
                            "You're on ${active.label} right now. Switch your phone to ${selected.label}, then continue."
                        else -> null
                    }
                    note?.let {
                        SurfaceCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (needsLocation) {
                    SurfaceCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.tertiaryContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Location permission needed", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Android only shares Wi-Fi details (signal strength, channel, access point) with apps " +
                                    "that have location permission. HeatNet does not use your location.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (onlyApproximate) {
                                Text(
                                    "You allowed only approximate location. Wi-Fi details need precise location: " +
                                        "tap Allow location again and choose Precise.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Button(onClick = { launcher.launch(WifiLocationPermissionPolicy.requestPermissions) }) {
                                Text("Allow location")
                            }
                            if (askedOnce) {
                                OutlinedButton(onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                                    )
                                }) { Text("Open app settings") }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    text = if (selected != null) "Continue with ${selected.label}" else "Choose a connection",
                    onClick = { selected?.let(onContinue) },
                    enabled = canMeasureSelected && !needsLocation,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** One selectable connection option: icon, name, live status and a radio dot. */
@Composable
private fun ConnectionOptionCard(
    type: ConnectionType,
    selected: Boolean,
    statusText: String,
    live: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val tint = if (selected) scheme.primary else scheme.onSurfaceVariant
    val borderBrush: Brush = if (selected) Brand.gradient else SolidColor(scheme.outlineVariant)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainer)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderBrush), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (selected) scheme.primary.copy(alpha = 0.18f) else scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (type == ConnectionType.WIFI) {
                WifiGlyph(tint, Modifier.size(30.dp))
            } else {
                SignalBarsGlyph(tint, Modifier.size(26.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(type.label, style = MaterialTheme.typography.titleMedium)
            StatusPill(
                text = statusText,
                dotColor = if (live) statusGreen() else scheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}
