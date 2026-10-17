package com.example.plantcarecompanion.ui.addplant

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.domain.MANUAL_ENTRY
import kotlinx.serialization.Serializable

// represents the species search screen's navigation route
@Serializable // allows navigation to work with this route
data object SpeciesSearch


@Serializable
data class AddPlant(val speciesId: Int = MANUAL_ENTRY) //speciesId identifies the species selected by the user. Destination screen uses this ID to load species details
//manual entry means the user enters details manually

fun NavGraphBuilder.addPlantGraph(
    navController: NavController,
    viewModel: AddPlantViewModel,
    onPlantSaved: (Plant) -> Unit
) {
    composable<SpeciesSearch> { //registers the species search as a navigation destination
        SpeciesSearchScreen( //displays the species search screen and connects it to the viewmodel
            query = viewModel.query, // the current search text
            state = viewModel.searchState, //state of the search - success, loading or error
            suggestions = viewModel.suggestions, // list of species suggestions to display
            onQueryChange = viewModel::onQueryChange, //call viewmodel whenever search query changes
            onSearch = viewModel::searchNow, //starts search when the user requests search
            onRetry = viewModel::searchNow,
            onSpeciesClick = { species -> navController.navigate(AddPlant(species.id)) },
            onManualEntry = { navController.navigate(AddPlant(MANUAL_ENTRY)) }, //when user chooses to enter plant details manually
            onBack = { navController.popBackStack() } //runs when user presses back button on the search. returns to previous destination in navigation stack
        )
    }

    composable<AddPlant> { backStackEntry ->
        val route: AddPlant = backStackEntry.toRoute()
        AddPlantRoute(
            speciesId = route.speciesId, //passes id so it can load selected species details or allow manual entry
            viewModel = viewModel, //for plant entry state
            onSave = onPlantSaved,
            onBack = { //go to previous screen through back stack
                viewModel.discardDraft()
                navController.popBackStack()
            }
        )
    }
}
