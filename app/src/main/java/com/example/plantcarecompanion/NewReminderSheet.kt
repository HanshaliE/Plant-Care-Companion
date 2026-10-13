package com.example.plantcarecompanion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class DateOption(val label: String, val dayOffset: Int)
private data class TimeOption(val label: String, val hour: Int)

private val dateOptions = listOf(
    DateOption("Today", 0),
    DateOption("Tomorrow", 1),
    DateOption("In 3 days", 3),
    DateOption("Next week", 7)
)

private val timeOptions = listOf(
    TimeOption("8:00 am", 8),
    TimeOption("12:00 pm", 12),
    TimeOption("6:00 pm", 18)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReminderSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, notes: String, dayOffset: Int?, hour: Int?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var dateEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedDayOffset by rememberSaveable { mutableIntStateOf(-1) }
    var timeEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedHour by rememberSaveable { mutableIntStateOf(-1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Text(
                    "New Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        onAdd(
                            title.trim(),
                            notes.trim(),
                            if (dateEnabled && selectedDayOffset >= 0) selectedDayOffset else null,
                            if (timeEnabled && selectedHour >= 0) selectedHour else null
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗓️ Date", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = dateEnabled,
                    onCheckedChange = { enabled ->
                        dateEnabled = enabled
                        if (!enabled) selectedDayOffset = -1
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle date for reminder"
                    }
                )
            }

            if (dateEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateOptions.forEach { option ->
                        FilterChip(
                            selected = selectedDayOffset == option.dayOffset,
                            onClick = { selectedDayOffset = option.dayOffset },
                            label = { Text(option.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⏰ Time", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = timeEnabled,
                    onCheckedChange = { enabled ->
                        timeEnabled = enabled
                        if (!enabled) selectedHour = -1
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle time for reminder"
                    }
                )
            }

            if (timeEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeOptions.forEach { option ->
                        FilterChip(
                            selected = selectedHour == option.hour,
                            onClick = { selectedHour = option.hour },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
