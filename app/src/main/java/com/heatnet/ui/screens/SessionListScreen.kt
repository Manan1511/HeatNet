package com.heatnet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import com.heatnet.ui.AppContainer

/**
 * Minimal saved-sessions list so the map can be reopened. Member 3 owns this screen (F6, F7):
 * delete, rename and the Compare entry point belong here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(container: AppContainer, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val summaries by remember { container.repository.observeSummaries() }.collectAsStateWithLifecycle(null)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved sessions") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        val list = summaries
        if (list != null && list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No sessions yet. Start one from the home screen.")
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(list.orEmpty(), key = { it.id }) { s ->
                ListItem(
                    headlineContent = { Text(s.name) },
                    supportingContent = {
                        Text("${s.connectionType.label} · ${s.readingCount} readings · ${dateFormat.format(Date(s.createdAt))}")
                    },
                    modifier = Modifier.clickable { onOpen(s.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
