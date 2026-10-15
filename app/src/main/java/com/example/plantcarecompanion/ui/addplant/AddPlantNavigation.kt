package com.example.plantcarecompanion.ui.addplant

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.domain.MANUAL_ENTRY
import kotlinx.serialization.Serializable

/** Type-safe routes (see the navigation lecture): a typo is a compile error, not a runtime crash. */
@Serializable
data object SpeciesSearch

/** Carries only a small id (not the whole object) — the screen loads the rest itself. 0 = type it manually. */
@Serializable
data class AddPlant(val speciesId: Int = MANUAL_ENTRY)

/**
 * Member B's two destinations. Plug into any NavHost:
 *
 *   addPlantGraph(navController, addPlantViewModel, onPlantSaved = { plant -> ...store it, navigate home... })
 *
 * and open the flow from anywhere with `navController.navigate(SpeciesSearch)`.
 * The screens never see the NavController — only plain lambdas.
 */
fun NavGraphBuilder.addPlantGraph(
    navController: NavController,
    viewModel: AddPlantViewModel,
    onPlantSaved: (Plant) -> Unit
) {
    composable<SpeciesSearch> {
        SpeciesSearchScreen(
            query = viewModel.query,
            state = viewModel.searchState,
            suggestions = viewModel.suggestions,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::searchNow,
            onRetry = viewModel::searchNow,
            onSpeciesClick = { species -> navController.navigate(AddPlant(species.id)) },
            onManualEntry = { navController.navigate(AddPlant(MANUAL_ENTRY)) },
            onBack = { navController.popBackStack() }
        )
    }

    composable<AddPlant> { backStackEntry ->
        val route: AddPlant = backStackEntry.toRoute()
        AddPlantRoute(
            speciesId = route.speciesId,
            viewModel = viewModel,
            onSave = onPlantSaved,
            onBack = {
                viewModel.discardDraft()
                navController.popBackStack()
            }
        )
    }
}
