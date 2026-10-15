package com.example.plantcarecompanion.ui.addplant

import com.example.plantcarecompanion.domain.LookupError
import com.example.plantcarecompanion.domain.Species

/**
 * Everything the search screen can be showing — one value instead of a pile of Booleans, so
 * "loading AND error" is impossible (Lecture 2: sealed classes; Lecture 5: ScreenState).
 */
sealed interface SearchUiState {
    /** Nothing typed yet (or fewer than 2 characters): show the built-in common plants. */
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    /**
     * [notice] != null means the live lookup failed and these results are the offline fallback
     * (saved lookups + built-in list); the screen shows a banner with the reason and a Retry.
     */
    data class Results(val species: List<Species>, val notice: LookupError? = null) : SearchUiState

    /** The lookup worked but nothing matched. */
    data class Empty(val query: String) : SearchUiState

    /** The lookup failed and there was no fallback result for this query either. */
    data class Error(val error: LookupError) : SearchUiState
}

/** State of fetching watering/light info for the chosen species on the Add Plant screen. */
sealed interface CareInfoState {
    data object Idle : CareInfoState
    data object Loading : CareInfoState
    data object Loaded : CareInfoState
    /** Couldn't get it — the user can still type watering/light by hand. */
    data class Unavailable(val error: LookupError?) : CareInfoState
}
