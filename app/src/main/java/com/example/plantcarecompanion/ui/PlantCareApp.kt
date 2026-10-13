package com.example.plantcarecompanion.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plantcarecompanion.di.AppContainer
import com.example.plantcarecompanion.ui.addplant.AddPlantViewModel
import com.example.plantcarecompanion.ui.addplant.SpeciesSearch
import com.example.plantcarecompanion.ui.addplant.addPlantGraph
import com.example.plantcarecompanion.ui.home.PlantListPlaceholder
import kotlinx.serialization.Serializable

@Serializable
data object PlantList

@Composable
fun PlantCareApp() {
    val navController = rememberNavController()
    val plantCareViewModel: PlantCareViewModel = viewModel(factory = AppContainer.plantCareViewModelFactory)
    val addPlantViewModel: AddPlantViewModel = viewModel(factory = AppContainer.addPlantViewModelFactory)

    NavHost(navController = navController, startDestination = PlantList) {
        composable<PlantList> {
            PlantListPlaceholder(
                plants = plantCareViewModel.plants,
                onAddPlant = { navController.navigate(SpeciesSearch) }
            )
        }

        addPlantGraph(
            navController = navController,
            viewModel = addPlantViewModel,
            onPlantSaved = { plant ->
                plantCareViewModel.addPlant(plant)               // Member A's ViewModel stores it in Room
                navController.popBackStack(PlantList, inclusive = false)
            }
        )
    }
}
