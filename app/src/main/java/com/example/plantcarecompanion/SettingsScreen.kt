package com.example.plantcarecompanion

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context)
    )
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.exportState) {
        when (val state = viewModel.exportState) {
            is SettingsViewModel.ExportState.Done -> {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    state.file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export plant care data"))
                viewModel.resetExportState()
            }
            is SettingsViewModel.ExportState.Empty -> {
                snackbarHostState.showSnackbar("Nothing to export yet — add a plant first.")
                viewModel.resetExportState()
            }
            is SettingsViewModel.ExportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(viewModel.photosCleared) {
        if (viewModel.photosCleared) {
            snackbarHostState.showSnackbar("Photos cleared.")
            viewModel.dismissPhotosClearedMessage()
        }
    }

    LaunchedEffect(viewModel.dataCleared) {
        if (viewModel.dataCleared) {
            snackbarHostState.showSnackbar("Your data has been cleared.")
            viewModel.dismissDataClearedMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SettingsSection(label = "Notifications") {
                SettingsSwitchRow(
                    emoji = "🔔",
                    title = "Watering reminders",
                    subtitle = if (viewModel.notificationsEnabled) {
                        "On: we'll remind you"
                    } else {
                        "Off"
                    },
                    checked = viewModel.notificationsEnabled,
                    onCheckedChange = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                    contentDescription = "Toggle watering reminder notifications"
                )
            }

            SettingsSection(label = "Your data") {
                val isExporting = viewModel.exportState is SettingsViewModel.ExportState.Working
                SettingsActionRow(
                    emoji = "📤",
                    title = "Export my data",
                    subtitle = "Download a JSON file of your plants and care history",
                    onClick = { viewModel.exportData() },
                    enabled = !isExporting,
                    trailing = {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
                SettingsDivider()
                SettingsActionRow(
                    emoji = "🖼️",
                    title = "Clear photos only",
                    subtitle = "Keep your plants and logs, just remove saved photos",
                    onClick = { viewModel.clearPhotosOnly() }
                )
                SettingsDivider()
                SettingsActionRow(
                    emoji = "🗑️",
                    title = "Clear all my data",
                    subtitle = "Delete every plant, care log, and reminder",
                    onClick = { showClearConfirm = true },
                    destructive = true
                )
            }

            InfoCard(
                title = "What's stored on your phone",
                lines = listOf(
                    "Your plants, care logs, photos, and preferences stay on this device only. " +
                        "The only thing sent over the network is the species name you search, " +
                        "used to fetch care info from the Perenual plant API. No account, no " +
                        "tracking, no ads.",
                    "This data is removed if you clear it above or uninstall the app."
                )
            )

            SettingsSection(label = "About") {
                SettingsInfoRow(
                    emoji = "🪴",
                    title = stringResource(R.string.app_name),
                    value = "Version ${viewModel.appVersion}"
                )
                SettingsDivider()
                SettingsInfoRow(
                    emoji = "🎓",
                    title = "Made for COMP2008",
                    value = null
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all your data?") },
            text = {
                Text(
                    "This deletes all plants, care log entries, and reminder settings stored " +
                        "on this device, and turns off watering reminders. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearLocalData()
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsSwitchRow(
    emoji: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .size(48.dp)
                .semantics { this.contentDescription = contentDescription }
        )
    }
}

@Composable
private fun SettingsActionRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(emoji: String, title: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            lines.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
