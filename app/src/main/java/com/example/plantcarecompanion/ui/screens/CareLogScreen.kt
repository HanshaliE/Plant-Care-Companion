package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.ui.PlantCareViewModel
import java.text.DateFormat
import java.util.Date


@Composable
fun CareLogScreen(
    plantId: Int,
    vm: PlantCareViewModel
) {
    LaunchedEffect(plantId) {
        vm.loadCareLogs(plantId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Care History",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            OutlinedTextField(
                value = vm.careLogNote,
                onValueChange = { note ->
                    vm.updateCareLogNote(note)
                },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = {
                    vm.addCareAction(
                        plantId = plantId,
                        careType = "Watered"
                    )
                }
            ) {
                Text("Mark as Watered")
            }
        }

        item {
            Button(
                onClick = {
                    vm.addCareAction(
                        plantId = plantId,
                        careType = "Fertilised"
                    )
                }
            ) {
                Text("Mark as Fertilised")
            }
        }

        item {
            Button(
                onClick = {
                    vm.addCareAction(
                        plantId = plantId,
                        careType = "Pruned"
                    )
                }
            ) {
                Text("Mark as Pruned")
            }
        }

        item {
            Button(
                onClick = {
                    vm.addCareAction(
                        plantId = plantId,
                        careType = "Repotted"
                    )
                }
            ) {
                Text("Mark as Repotted")
            }
        }

        if (vm.careLogs.isEmpty()) {
            item {
                Text("No care history yet")
            }
        } else {
            items(
                items = vm.careLogs,
                key = { entry -> entry.id }
            ) { entry ->
                CareLogCard(entry)
            }
        }
    }
}

@Composable
fun CareLogCard(entry: CareLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = entry.careType,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )

            entry.notes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}