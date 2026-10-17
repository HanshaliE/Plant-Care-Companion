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

@OptIn(ExperimentalMaterial3Api::class) // allows the use of TopAppBar which is marked as experimental
@Composable
fun PlantListPlaceholder(
    plants: List<Plant>, //list of plants that should be displayed on the screen
    onAddPlant: () -> Unit //a callback that runs when the user taps the add plant button, the parent screen decides what action to perform
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My plants") }) }, //app bar displayed on top of the screen
        floatingActionButton = {
            ExtendedFloatingActionButton( //floating action button used to start adding a plant
                onClick = onAddPlant, //calls the callback supplied by the parent screen
                icon = { Icon(Icons.Default.Add, contentDescription = null) }, //display a plus icon inside the button
                text = { Text("Add plant") } //text displayed beside the icon
            )
        }
    ) { padding ->
        if (plants.isEmpty()) { //check whether the list contains any plants
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center //places the message in the center of the box
            ) {
                Text("No plants yet — tap “Add plant”.") //message displayed when user has no saved plants
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

// a card to display an individual plant
@Composable
private fun PlantCard(plant: Plant) {
    Card(modifier = Modifier.fillMaxWidth()) { //card give the plant's information a contained visual surface and makes the card stretch across available width
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { //places the image and plant information side by side
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                if (plant.imageUri != null) { //only try to load an image if the plant has a URI
                    AsyncImage(
                        model = plant.imageUri, // the URI identified the plant's saved image
                        contentDescription = "Photo of ${plant.nickname}", // accessibility description for the image
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(plant.nickname, style = MaterialTheme.typography.titleMedium)
                Text( //builds a line containing the species name and location seperated by a middle dot
                    listOfNotNull(plant.speciesName, plant.location).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Water every ${plant.wateringIntervalDays} days", //displayed how often this plant should be watered. number comes from the plant data object
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
