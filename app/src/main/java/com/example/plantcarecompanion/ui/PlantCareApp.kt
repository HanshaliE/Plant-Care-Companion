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
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute
import com.example.plantcarecompanion.ui.screens.CareLogScreen
import com.example.plantcarecompanion.ui.screens.MyPlantsScreen
import com.example.plantcarecompanion.ui.screens.PlantDetailsScreen

@Serializable
data object PlantList

@Serializable
data class PlantDetails(val plantId: Int)

@Serializable
data class CareLog(val plantId: Int)
@Composable
fun PlantCareApp() {
    val navController = rememberNavController()
    val plantCareViewModel: PlantCareViewModel = viewModel(factory = AppContainer.plantCareViewModelFactory)
    val addPlantViewModel: AddPlantViewModel = viewModel(factory = AppContainer.addPlantViewModelFactory)

    NavHost(navController = navController, startDestination = PlantList) {
        composable<PlantList> {
            MyPlantsScreen(
                vm = plantCareViewModel,
                onPlantClick = { plantId ->
                    navController.navigate(PlantDetails(plantId))
                },
                onAddPlant = {
                    navController.navigate(SpeciesSearch)
                }
            )
        }

        composable<PlantDetails> { backStackEntry ->
            val route: PlantDetails = backStackEntry.toRoute()

            PlantDetailsScreen(
                plantId = route.plantId,
                vm = plantCareViewModel,
                onCareLogClick = { plantId ->
                    navController.navigate(CareLog(plantId))
                }
            )
        }

        composable<CareLog> { backStackEntry ->
            val route: CareLog = backStackEntry.toRoute()

            CareLogScreen(
                plantId = route.plantId,
                vm = plantCareViewModel
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
