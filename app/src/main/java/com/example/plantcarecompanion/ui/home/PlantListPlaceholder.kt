package com.example.plantcarecompanion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.data.local.entity.Plant

/**
 * TEMPORARY home screen so the Add Plant flow can be run and tested end to end.
 * Member A: replace this with the real plant list; all you need to keep is a way to call
 * `navController.navigate(SpeciesSearch)` (see PlantCareApp.kt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListPlaceholder(
    plants: List<Plant>,
    onAddPlant: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My plants") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlant,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add plant") }
            )
        }
    ) { padding ->
        if (plants.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No plants yet — tap “Add plant”.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plants, key = { it.id }) { plant -> PlantCard(plant) }
            }
        }
    }
}

@Composable
private fun PlantCard(plant: Plant) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                if (plant.imageUri != null) {
                    AsyncImage(
                        model = plant.imageUri,
                        contentDescription = "Photo of ${plant.nickname}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(plant.nickname, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(plant.speciesName, plant.location).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Water every ${plant.wateringIntervalDays} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
