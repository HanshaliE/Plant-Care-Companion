package com.example.plantcarecompanion

import androidx.compose.foundation.horizontalScroll // allows rows to scroll horizontally when content is too wide
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer //above controls how UI elements are arranged
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding //adds padding when the on-screen keyboard is displayed
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api //allows use of Material 3 APIs marked as experimental
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch //creates an on/off switch
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
import androidx.compose.ui.unit.dp //provides density-independent pixel units

//defines a data class representing a date option.
//label - the text displayed to the user, dataOffset - number of days from today. ex: ("Tomorrow", 1) means tomorrow
private data class DateOption(val label: String, val dayOffset: Int)
//defines a data class representing a time option.
private data class TimeOption(val label: String, val hour: Int)

//a predefined list of date choices
private val dateOptions = listOf(
    DateOption("Today", 0),
    DateOption("Tomorrow", 1),
    DateOption("In 3 days", 3),
    DateOption("Next week", 7)
)

// a predefined list of time choices
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
    var timeEnabled by rememberSaveable { mutableStateOf(false) } // stores whether uses has enabled the time option
    var selectedHour by rememberSaveable { mutableIntStateOf(-1) } //-1 means no time has been selected. ex: 8 means 8.00 am, 18 means 6.00 pm

    ModalBottomSheet( //creates bottom sheet that slides up from the bottom
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
                horizontalArrangement = Arrangement.SpaceBetween, // distributes children across available width
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
                    checked = dateEnabled, // reflects the current dataEnabled state. True means on, false means off
                    onCheckedChange = { enabled -> //runs whenever the user changes the switch
                        dateEnabled = enabled //updates dateEnabled to the new switch value. "enabled" is the new boolean value.
                        if (!enabled) selectedDayOffset = -1 //if the user turns the date switch off, reset selected date to -1. This prevents old selection from being used while the date option is disabled
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle date for reminder"
                    }
                )
            }

            //only displays the date choices if the date switch is on
            if (dateEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //loops through every option in dateOptions.
                    dateOptions.forEach { option ->
                        //creates a selectable chip for each date
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
                //displays the time label with a clock emoji
                Text("⏰ Time", style = MaterialTheme.typography.bodyLarge)
                Switch( //creates the on/off switch for the time option
                    checked = timeEnabled, //reflects the current time enabled state
                    onCheckedChange = { enabled -> //runs whenever the user turns the time switch off, reset the selected hour to -1
                        timeEnabled = enabled
                        if (!enabled) selectedHour = -1
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle time for reminder" //accessibility description
                    }
                )
            }

            if (timeEnabled) { //only displays time choices if the time switch is on
                Row( //places time choices horizontally
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()), //scrollable
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeOptions.forEach { option ->
                        FilterChip( //creates selectable chip for each time
                            selected = selectedHour == option.hour, //highlights the chip if hour matches the currently selected hour
                            onClick = { selectedHour = option.hour }, //saves the selected hour when clicked
                            label = { Text(option.label) } //displays time label
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
