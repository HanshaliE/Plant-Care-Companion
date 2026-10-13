package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.ui.PlantCareViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun PlantDetailsScreen(
    plantId: Int,
    vm: PlantCareViewModel,
    onCareLogClick: (Int) -> Unit,
    onEditClick: (Int) -> Unit
) {
    LaunchedEffect(plantId) {
        vm.loadPlant(plantId)
        vm.loadCareLogs(plantId)
    }

    val plant = vm.selectedPlant

    if (plant == null) {
        Text("Loading plant...")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        if (plant.imageUri != null) {
            AsyncImage(
                model = plant.imageUri,
                contentDescription = "Photo of ${plant.nickname}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = plant.nickname,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = plant.speciesName,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Care Schedule",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Water every ${plant.wateringIntervalDays} days")

        val lastWatered = vm.lastWateredEntry

        Text(
            text = if (lastWatered != null) {
                "Last watered: ${
                    DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(Date(lastWatered.timestamp))
                }"
            } else {
                "Last watered: No record yet"
            }
        )

        plant.lightRequirement?.let {
            Text("Light: $it")
        }

        plant.location?.let {
            Text("Location: $it")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onEditClick(plant.id)
            }
        ) {
            Text("Edit Plant")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onCareLogClick(plant.id) }
        ) {
            Text("View Care History")
        }
    }
}