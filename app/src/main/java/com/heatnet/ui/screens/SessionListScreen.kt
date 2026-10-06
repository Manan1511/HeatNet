package com.heatnet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heatnet.data.model.SessionSummary
import com.heatnet.ui.AppContainer
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Saved sessions (F6): open, rename, delete, and pick two sessions to compare (F7). Comparing
 * needs two sessions on the same connection type, so the second pick is limited to that type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onCompare: (beforeId: Long, afterId: Long) -> Unit,
) {
    val summaries by remember { container.repository.observeSummaries() }.collectAsStateWithLifecycle(null)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val scope = rememberCoroutineScope()

    var comparing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptyList<SessionSummary>()) }
    var renaming by remember { mutableStateOf<SessionSummary?>(null) }
    var deleting by remember { mutableStateOf<SessionSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (comparing) "Pick two sessions" else "Saved sessions") },
                navigationIcon = {
                    if (comparing) {
                        IconButton(onClick = { comparing = false; selected = emptyList() }) {
                            Icon(Icons.Filled.Close, "Cancel compare")
                        }
                    } else {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                },
                actions = {
                    if (!comparing && (summaries?.size ?: 0) >= CompareSelection.REQUIRED) {
                        TextButton(onClick = { comparing = true }) { Text("Compare") }
                    }
                },
            )
        },
        bottomBar = {
            if (comparing) {
                val ids = CompareSelection.orderedIds(selected)
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = { ids?.let { (before, after) -> onCompare(before, after) } },
                        enabled = ids != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (ids == null) "Select ${CompareSelection.REQUIRED - selected.size} more" else "Compare") }
                    Text(
                        "Only sessions on the same connection type (Wi-Fi or mobile data) can be compared.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
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
                val isSelected = selected.any { it.id == s.id }
                val enabled = !comparing || CompareSelection.canSelect(selected, s)
                ListItem(
                    headlineContent = { Text(s.name) },
                    supportingContent = {
                        Text("${s.connectionType.label} · ${s.readingCount} readings · ${dateFormat.format(Date(s.createdAt))}")
                    },
                    leadingContent = if (comparing) {
                        { Checkbox(checked = isSelected, onCheckedChange = null, enabled = enabled) }
                    } else {
                        null
                    },
                    trailingContent = if (comparing) {
                        null
                    } else {
                        { SessionMenu(onRename = { renaming = s }, onDelete = { deleting = s }) }
                    },
                    modifier = Modifier.clickable(enabled = enabled) {
                        if (comparing) selected = CompareSelection.toggle(selected, s) else onOpen(s.id)
                    },
                )
                HorizontalDivider()
            }
        }
    }

    renaming?.let { target ->
        RenameDialog(
            current = target.name,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                scope.launch { container.repository.rename(target.id, name) }
            },
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this session?") },
            text = { Text("\"${target.name}\" and its ${target.readingCount} readings will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    selected = selected.filterNot { it.id == target.id }
                    scope.launch { container.repository.delete(target.id) }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SessionMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "Session options") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { open = false; onRename() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename session") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
