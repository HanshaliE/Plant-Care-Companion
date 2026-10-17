package com.example.plantcarecompanion.ui.addplant

import com.example.plantcarecompanion.domain.LookupError
import com.example.plantcarecompanion.domain.Species

//a sealed interface defining all possible states of the species search UI
//sealed - restricts which classes or objects can implement this interface
sealed interface SearchUiState {
    data object Idle : SearchUiState // the search is idle - no search is currently being performed
    data object Loading : SearchUiState //search is currently in progress. the UI uses this state to display a loading indicator

    data class Results(val species: List<Species>, val notice: LookupError? = null) : SearchUiState //a search completed and returned one or more matching species

    data class Empty(val query: String) : SearchUiState // search completed, but no matching species were found. stores query that produces no results

    data class Error(val error: LookupError) : SearchUiState
}

//for state of plant care information
sealed interface CareInfoState {
    data object Idle : CareInfoState //care information has not started loading, or the state has been reset
    data object Loading : CareInfoState //care information is currently being retrieved
    data object Loaded : CareInfoState // successfully retrieved care info
    data class Unavailable(val error: LookupError?) : CareInfoState //cannot be obtained fully, stores the reason if knowsn can be null if no specific error available.
}
