package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.ui.GradientHeroCard
import com.example.plantcarecompanion.ui.PlantCareViewModel
import com.example.plantcarecompanion.ui.PlantSortOption

// The first screen: a gradient header, search + sort, the list of plants,
// and a small "Add plant" button floating at the bottom-right corner that stays put while the list scrolls.
@Composable
fun MyPlantsScreen(
    vm: PlantCareViewModel,
    onPlantClick: (Int) -> Unit, // called with the plant's id when a plant card is tapped
    onAddPlant: () -> Unit // called when the small bottom-right "Add plant" button is tapped
) {
    var sortMenuExpanded by remember { mutableStateOf(false) } // whether the sort dropdown is open

    // Box lets the small button sit on top of the list (aligned to the bottom-right corner)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // keeps content clear of the status bar and system bars
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // bottom padding so the last plant card can scroll up above the small floating Add plant button
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PlantsHeroCard(
                    totalPlants = vm.plants.size,
                    plantsWithPhotos = vm.plants.count { it.imageUri != null }
                )
            }

            item {
                OutlinedTextField(
                    value = vm.searchQuery,
                    onValueChange = { query -> vm.updateSearchQuery(query) },
                    label = { Text("Search plants") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        // clear button only shows once something has been typed
                        if (vm.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Box so the dropdown opens right under the sort pill
                    Box {
                        Surface(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = when (vm.sortOption) {
                                    PlantSortOption.RECENTLY_ADDED -> "Sort: Recently added"
                                    PlantSortOption.NAME_A_TO_Z -> "Sort: Name A-Z"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
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

                    // how many plants match the current search
                    Text(
                        text = "${vm.filteredPlants.size} shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (vm.filteredPlants.isEmpty()) {
                item { EmptyPlantsCard(searching = vm.searchQuery.isNotBlank()) }
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

        // Small round "add" button floating at the bottom-right corner; it stays put while the list scrolls
        ExtendedFloatingActionButton(
            onClick = onAddPlant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null
                )
            },
            text = {
                Text("Add Plant")
            }
        )
    }
}

// Gradient header card (same style as Reminders / Profile) with the title and two stat tiles
@Composable
private fun PlantsHeroCard(totalPlants: Int, plantsWithPhotos: Int) {
    GradientHeroCard {
        Text(
            "🌿 My Plants",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (totalPlants == 0) "Your garden is waiting for its first plant." else "Everything growing in your garden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroStatTile(
                modifier = Modifier.weight(1f),
                emoji = "🪴",
                value = totalPlants.toString(),
                label = "Total plants"
            )
            HeroStatTile(
                modifier = Modifier.weight(1f),
                emoji = "📷",
                value = plantsWithPhotos.toString(),
                label = "With photos"
            )
        }
    }
}

// One translucent stat tile that sits on the gradient (same look as the Reminders stat tiles)
@Composable
private fun HeroStatTile(modifier: Modifier = Modifier, emoji: String, value: String, label: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f))
            .padding(12.dp)
    ) {
        Text(
            "$emoji $value",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// Shown instead of the list when there is nothing to display
@Composable
private fun EmptyPlantsCard(searching: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌱", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (searching) "No matching plants" else "No plants yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (searching) "Try a different name." else "Tap “Add Plant” below to add your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// One plant in the list: photo (or a leaf placeholder), nickname, species, location and a watering chip
@Composable
fun PlantCard(
    plant: Plant,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (plant.imageUri != null) {
                AsyncImage(
                    model = plant.imageUri,
                    contentDescription = "Photo of ${plant.nickname}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                // no photo yet: coloured square with a plant emoji so every card lines up
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪴", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = plant.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = plant.speciesName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // location is optional, so only show the line when one was entered
                if (!plant.location.isNullOrBlank()) {
                    Text(
                        text = "📍 ${plant.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // watering interval chip, same pill style as the status chips on the Reminders screen
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "💧 ${plant.wateringIntervalDays}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
