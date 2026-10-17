package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.ui.BackTopBar
import com.example.plantcarecompanion.ui.PlantCareViewModel
import java.text.DateFormat
import java.util.Date
import androidx.compose.material3.Scaffold

@Composable
fun CareLogScreen(
    plantId: Int,
    vm: PlantCareViewModel,
    onBack: () -> Unit
) {
    // Load care history for the selected plant
    LaunchedEffect(plantId) {
        vm.loadCareLogs(plantId)
    }

    Scaffold(
        topBar = { BackTopBar(title = "Care History", onBack = onBack) }
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {


        // Show message if the plant has no care records
        if (vm.careLogs.isEmpty()) {
            item {
                Text(
                    text = "No care history yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {

            // Display all previous care activities
            items(
                items = vm.careLogs,
                key = { entry -> entry.id }
            ) { entry ->
                CareLogCard(entry)
            }
        }
    }
    } // end Scaffold content
}

@Composable
fun CareLogCard(entry: CareLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // Type of care activity
            Text(
                text = entry.careType,
                style = MaterialTheme.typography.titleMedium
            )

            // Date and time of the activity
            Text(
                text = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )

            // Display notes only if they exist
            entry.notes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}