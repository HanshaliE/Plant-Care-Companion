package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.ui.PlantCareViewModel
import com.example.plantcarecompanion.ui.PlantSortOption

@Composable
fun MyPlantsScreen(
    vm: PlantCareViewModel,
    onPlantClick: (Int) -> Unit
) {
    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "My Plants",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { query ->
                    vm.updateSearchQuery(query)
                },
                label = { Text("Search plants") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            TextButton(
                onClick = {
                    sortMenuExpanded = true
                }
            ) {
                Text(
                    text = when (vm.sortOption) {
                        PlantSortOption.RECENTLY_ADDED -> "Sort: Recently added"
                        PlantSortOption.NAME_A_TO_Z -> "Sort: Name A-Z"
                    }
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = {
                    sortMenuExpanded = false
                }
            ) {
                DropdownMenuItem(
                    text = { Text("Recently added") },
                    onClick = {
                        vm.updateSortOption(PlantSortOption.RECENTLY_ADDED)
                        sortMenuExpanded = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Name A-Z") },
                    onClick = {
                        vm.updateSortOption(PlantSortOption.NAME_A_TO_Z)
                        sortMenuExpanded = false
                    }
                )
            }
        }
        if (vm.filteredPlants.isEmpty()) {
            item {
                Text(
                    text = if (vm.searchQuery.isBlank()) {
                        "No plants added yet"
                    } else {
                        "No matching plants"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            items(
                items = vm.filteredPlants,
                key = { plant -> plant.id }
            ) { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onPlantClick(plant.id) }
                )
            }
        }

    }
}

@Composable
fun PlantCard(
    plant: Plant,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = plant.nickname,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = plant.speciesName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}