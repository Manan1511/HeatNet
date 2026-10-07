package com.heatnet.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.ui.AppContainer

/**
 * Step 1 of a session: detect Wi-Fi or mobile data with Member 1's [ConnectionMonitor] and lock
 * the session to it (F2). Wi-Fi sessions are blocked until location permission is granted,
 * because Android hides Wi-Fi details without it.
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

    var demoType by rememberSaveable { mutableStateOf(ConnectionType.WIFI) }
    val snapshot by container.connectionMonitor.state.collectAsStateWithLifecycle()
    val detected: ConnectionType? = if (fake) demoType else snapshot.connectionType

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New session") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (fake) {
                Text("Demo mode: pick a connection type to pretend to use.", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectionType.entries.forEach { type ->
                        FilterChip(selected = demoType == type, onClick = { demoType = type }, label = { Text(type.label) })
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Your connection", style = MaterialTheme.typography.labelLarge)
                        Text(
                            when {
                                detected != null -> detected.label
                                !snapshot.hasTransportObservation -> "Checking…"
                                snapshot.network == null -> "Not connected"
                                else -> "Not supported (use Wi-Fi or mobile data)"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                Text(
                    "The whole session stays on this connection. Wi-Fi and mobile data readings are never mixed in one map.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val needsLocation = !fake && detected == ConnectionType.WIFI && !hasLocation
            if (needsLocation) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Button(
                onClick = { detected?.let(onContinue) },
                enabled = detected != null && !needsLocation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (detected != null) "Continue on ${detected.label}" else "Continue")
            }
        }
    }
}
