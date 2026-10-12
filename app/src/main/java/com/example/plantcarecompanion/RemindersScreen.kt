package com.example.plantcarecompanion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RemindersScreen() {
    val context = LocalContext.current
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(context)
    )
    var showDeniedMessage by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableReminders()
            showDeniedMessage = false
        } else {
            showDeniedMessage = true
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Watering Reminders", style = MaterialTheme.typography.headlineSmall)

        Switch(
            checked = viewModel.remindersEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.enableReminders()
                    }
                } else {
                    viewModel.disableReminders()
                }
            }
        )

        if (showDeniedMessage) {
            Text(
                "Notifications are off — enable them in system settings to get reminders.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}