package com.example.plantcarecompanion.ui.addplant

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.photo.PlantPhotoStorage
import com.example.plantcarecompanion.data.repository.SpeciesRepository
import com.example.plantcarecompanion.domain.DraftErrors
import com.example.plantcarecompanion.domain.LocalPlantCatalog
import com.example.plantcarecompanion.domain.MANUAL_ENTRY
import com.example.plantcarecompanion.domain.PlantDraft
import com.example.plantcarecompanion.data.local.entity.Plant
import com.example.plantcarecompanion.domain.Species
import com.example.plantcarecompanion.domain.toPlantOrNull
import com.example.plantcarecompanion.domain.validate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * State + decisions for the whole "add a plant" flow: Species Search -> Add Plant.
 * One instance is shared by both screens (created above the NavHost, Lecture 5), so the typed query,
 * the results and the half-filled form all survive rotation and moving between the two screens.
 *
 * It never touches Retrofit or Room directly — only [SpeciesRepository] — and it never saves the plant
 * itself: [buildPlant] hands back a validated Plant and the caller (hoisted callback) stores it.
 */
class AddPlantViewModel(
    private val repository: SpeciesRepository,
    private val photos: PlantPhotoStorage
) : ViewModel() {

    // ---------- search ----------

    var query by mutableStateOf("")
        private set

    var searchState by mutableStateOf<SearchUiState>(SearchUiState.Idle)
        private set

    /** Shown while the search box is empty — available with zero network. */
    val suggestions: List<Species> = LocalPlantCatalog.species

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel()
        val q = repository.normalize(newQuery)
        if (q.length < SpeciesRepository.MIN_QUERY_LENGTH) {
            searchState = SearchUiState.Idle
            return
        }
        // Debounce: wait for a pause in typing so one word costs one request, not one per letter.
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            runSearch(q)
        }
    }

    /** Keyboard "Search" key and the Retry button: go now, no debounce. */
    fun searchNow() {
        searchJob?.cancel()
        val q = repository.normalize(query)
        if (q.length < SpeciesRepository.MIN_QUERY_LENGTH) {
            searchState = SearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch { runSearch(q) }
    }

    private suspend fun runSearch(normalizedQuery: String) {
        searchState = SearchUiState.Loading
        val outcome = repository.search(normalizedQuery)
        searchState = when {
            outcome.species.isEmpty() && outcome.error != null -> SearchUiState.Error(outcome.error)
            outcome.species.isEmpty() -> SearchUiState.Empty(normalizedQuery)
            else -> SearchUiState.Results(outcome.species, outcome.error)
        }
    }

    // ---------- the form ----------

    var draft by mutableStateOf(PlantDraft())
        private set

    var errors by mutableStateOf(DraftErrors())
        private set

    var careInfo by mutableStateOf<CareInfoState>(CareInfoState.Idle)
        private set

    // If the user typed their own value, a late API answer must not overwrite it.
    private var intervalEdited = false
    private var lightEdited = false
    private var careJob: Job? = null

    /** Called by the Add Plant destination with the id from its route. 0 = manual entry. */
    fun loadSpecies(speciesId: Int) {
        if (draft.speciesId == speciesId) return          // already set up (rotation / came back)
        careJob?.cancel()
        intervalEdited = false
        lightEdited = false
        errors = DraftErrors()

        if (speciesId == MANUAL_ENTRY) {
            draft = draft.copy(
                speciesId = MANUAL_ENTRY, speciesName = "", nickname = "",
                wateringIntervalDays = "", lightRequirement = ""
            )
            careInfo = CareInfoState.Idle
            return
        }

        draft = draft.copy(
            speciesId = speciesId, speciesName = "", nickname = "",
            wateringIntervalDays = "", lightRequirement = ""
        )
        careJob = viewModelScope.launch {
            // 1) instant, local: name + whatever care info we already have
            val local = repository.getSpecies(speciesId)
            if (local == null) {
                careInfo = CareInfoState.Unavailable(null)
                return@launch
            }
            applySpecies(local, firstTime = true)

            // 2) only if watering info is missing/stale: one network call, failure is non-fatal
            careInfo = CareInfoState.Loading
            val outcome = repository.enrichCareInfo(local)
            applySpecies(outcome.species, firstTime = false)
            careInfo = if (outcome.species.wateringIntervalDays != null) {
                CareInfoState.Loaded
            } else {
                CareInfoState.Unavailable(outcome.error)
            }
        }
    }

    private fun applySpecies(species: Species, firstTime: Boolean) {
        var d = draft
        if (firstTime) {
            d = d.copy(
                speciesName = species.commonName,
                nickname = if (d.nickname.isBlank()) species.commonName else d.nickname
            )
        }
        if (!intervalEdited && species.wateringIntervalDays != null) {
            d = d.copy(wateringIntervalDays = species.wateringIntervalDays.toString())
        }
        if (!lightEdited && !species.lightRequirement.isNullOrBlank()) {
            d = d.copy(lightRequirement = species.lightRequirement)
        }
        draft = d
    }

    fun onNicknameChange(value: String) {
        draft = draft.copy(nickname = value)
        if (errors.nickname) errors = errors.copy(nickname = false)
    }

    fun onSpeciesNameChange(value: String) {
        draft = draft.copy(speciesName = value)
        if (errors.species) errors = errors.copy(species = false)
    }

    fun onLocationChange(value: String) {
        draft = draft.copy(location = value)
    }

    fun onIntervalChange(value: String) {
        intervalEdited = true
        draft = draft.copy(wateringIntervalDays = value.filter(Char::isDigit).take(3))
        if (errors.interval) errors = errors.copy(interval = false)
    }

    fun onLightChange(value: String) {
        lightEdited = true
        draft = draft.copy(lightRequirement = value)
    }

    // ---------- photo (camera / gallery) ----------

    var photoBusy by mutableStateOf(false)
        private set

    var photoError by mutableStateOf<String?>(null)
        private set

    private var pendingCameraFile: File? = null

    /** Step 1 of taking a photo: returns the URI the camera app should write to. */
    fun prepareCameraShot(): Uri {
        pendingCameraFile?.delete()
        val file = photos.newPhotoFile()
        pendingCameraFile = file
        photoError = null
        return photos.uriFor(file)
    }

    /** Step 2: the camera app finished (success = false if the user cancelled). */
    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile ?: return
        pendingCameraFile = null
        if (success && file.length() > 0) setPhoto(file) else file.delete()
    }

    fun onCameraUnavailable() {
        pendingCameraFile?.delete()
        pendingCameraFile = null
        photoError = "No camera app available on this device."
    }

    fun onGalleryPicked(uri: Uri?) {
        if (uri == null) return                              // user backed out of the picker
        viewModelScope.launch {
            photoBusy = true
            photoError = null
            val file = photos.importFromGallery(uri)
            photoBusy = false
            if (file != null) setPhoto(file) else photoError = "Couldn't read that image."
        }
    }

    fun removePhoto() {
        photos.deleteByUriString(draft.photoUri)
        draft = draft.copy(photoUri = null)
    }

    private fun setPhoto(file: File) {
        photos.deleteByUriString(draft.photoUri)             // replacing: don't leave the old file behind
        draft = draft.copy(photoUri = photos.uriString(file))
    }

    // ---------- finishing ----------

    /** Validates; returns the Plant to store, or null (and sets [errors]) if the form isn't valid yet. */
    fun buildPlant(): Plant? {
        errors = draft.validate()
        return draft.toPlantOrNull()
    }

    private var savedPendingClear = false

    /** The caller stored the plant. The form is emptied once the screen has actually left. */
    fun onPlantSaved() {
        savedPendingClear = true
    }

    /** Called when the Add Plant screen leaves composition (not on rotation — the flag is only set after a save). */
    fun onAddPlantScreenDisposed() {
        if (savedPendingClear) {
            savedPendingClear = false
            clearDraft()
        }
    }

    /** After a successful save: empty the form but KEEP the photo file (the plant now owns it). */
    fun clearDraft() {
        careJob?.cancel()
        draft = PlantDraft()
        errors = DraftErrors()
        careInfo = CareInfoState.Idle
        intervalEdited = false
        lightEdited = false
        photoError = null
    }

    /** User backed out without saving: throw away the draft and any photo taken for it. */
    fun discardDraft() {
        photos.deleteByUriString(draft.photoUri)
        pendingCameraFile?.delete()
        pendingCameraFile = null
        clearDraft()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
