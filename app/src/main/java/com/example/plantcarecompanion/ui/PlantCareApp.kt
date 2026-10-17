package com.example.plantcarecompanion.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.plantcarecompanion.ProfileScreen
import com.example.plantcarecompanion.RemindersScreen
import com.example.plantcarecompanion.SettingsScreen
import com.example.plantcarecompanion.di.AppContainer
import com.example.plantcarecompanion.ui.addplant.AddPlantViewModel
import com.example.plantcarecompanion.ui.addplant.SpeciesSearch
import com.example.plantcarecompanion.ui.addplant.addPlantGraph
import com.example.plantcarecompanion.ui.screens.CareLogScreen
import com.example.plantcarecompanion.ui.screens.EditPlantScreen
import com.example.plantcarecompanion.ui.screens.MyPlantsScreen
import com.example.plantcarecompanion.ui.screens.PlantDetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data object PlantList

@Serializable
data class PlantDetails(val plantId: Int)

@Serializable
data class CareLog(val plantId: Int)

@Serializable
data class EditPlant(val plantId: Int)

@Serializable
data object RemindersRoute

@Serializable
data object SettingsRoute

@Serializable
data object ProfileRoute

@Composable
fun PlantCareApp() {
    val navController = rememberNavController()
    val plantCareViewModel: PlantCareViewModel = viewModel(factory = AppContainer.plantCareViewModelFactory)
    val addPlantViewModel: AddPlantViewModel = viewModel(factory = AppContainer.addPlantViewModelFactory)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            // The bar now stays visible on every page. Sub-pages (plant details, care log, edit,
            // search, add plant) count as part of the Plants tab, so Plants stays highlighted there.
            val onReminders = destination?.hasRoute(RemindersRoute::class) == true
            val onProfile = destination?.hasRoute(ProfileRoute::class) == true
            val onSettings = destination?.hasRoute(SettingsRoute::class) == true
            val onPlants = !onReminders && !onProfile && !onSettings
            val showBar = true

            if (showBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = onPlants,
                        onClick = {
                            navController.navigate(PlantList) {
                                popUpTo(PlantList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Plants") },
                        label = { Text("Plants") }
                    )
                    NavigationBarItem(
                        selected = onReminders,
                        onClick = {
                            navController.navigate(RemindersRoute) {
                                popUpTo(PlantList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = "Reminders") },
                        label = { Text("Reminders") }
                    )
                    NavigationBarItem(
                        selected = onProfile,
                        onClick = {
                            navController.navigate(ProfileRoute) {
                                popUpTo(PlantList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                    NavigationBarItem(
                        selected = onSettings,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(PlantList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PlantList,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
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

            composable<PlantDetails> { entry ->
                val route: PlantDetails = entry.toRoute()

                PlantDetailsScreen(
                    plantId = route.plantId,
                    vm = plantCareViewModel,
                    onCareLogClick = { plantId ->
                        navController.navigate(CareLog(plantId))
                    },
                    onEditClick = { plantId ->
                        navController.navigate(EditPlant(plantId))
                    },
                    onDeleted = {
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack() // back to the Plants list
                    }
                )
            }

            composable<CareLog> { entry ->
                val route: CareLog = entry.toRoute()

                CareLogScreen(
                    plantId = route.plantId,
                    vm = plantCareViewModel,
                    onBack = {
                        navController.popBackStack() // back to Plant Details
                    }
                )
            }

            composable<EditPlant> { entry ->
                val route: EditPlant = entry.toRoute()

                EditPlantScreen(
                    plantId = route.plantId,
                    vm = plantCareViewModel,
                    onSaved = {
                        navController.popBackStack() // back to Plant Details, changes saved
                    },
                    onBack = {
                        navController.popBackStack() // back to Plant Details, no changes (now also wired to the top bar's arrow)
                    }
                )
            }

            composable<RemindersRoute> { RemindersScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ProfileRoute> { ProfileScreen() }

            addPlantGraph(
                navController = navController,
                viewModel = addPlantViewModel,
                onPlantSaved = { plant ->
                    plantCareViewModel.addPlant(plant)
                    navController.popBackStack(PlantList, inclusive = false)
                }
            )
        }
    }
}