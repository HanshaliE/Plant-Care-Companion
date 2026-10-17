package com.example.plantcarecompanion.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.plantcarecompanion.ui.BackTopBar
import com.example.plantcarecompanion.ui.PlantCareViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun EditPlantScreen(
    plantId: Int,
    vm: PlantCareViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var showError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(plantId) {
        vm.loadPlant(plantId)
    }

    val plant = vm.selectedPlant

    LaunchedEffect(plant?.id) {
        if (plant != null && plant.id == plantId) {
            vm.prepareEditPlant(plant)
        }
    }

    // onBack already existed (wired to the Cancel button below) — reuse it for the top bar too.
    Scaffold(
        topBar = { BackTopBar(title = "Edit Plant", onBack = onBack) }
    ) { innerPadding ->
        if (plant == null) {
            Text("Loading plant...", modifier = Modifier.padding(innerPadding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        Text(
            text = "Edit Plant",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Species: ${plant.speciesName}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = vm.editNickname,
            onValueChange = { vm.updateEditNickname(it) },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = vm.editLocation,
            onValueChange = { vm.updateEditLocation(it) },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = vm.editWateringInterval,
            onValueChange = { vm.updateEditWateringInterval(it) },
            label = { Text("Water every (days)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = vm.editLightRequirement,
            onValueChange = { vm.updateEditLightRequirement(it) },
            label = { Text("Light requirement") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (vm.saveEditedPlant()) {
                    onSaved()
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                vm.cancelEditPlant()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

        if (showError) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Enter a nickname and a valid watering interval.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    } // end Scaffold content
}