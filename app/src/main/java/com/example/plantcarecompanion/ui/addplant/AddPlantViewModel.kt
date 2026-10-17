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

//viewmodel manages add plant screens data and business logic. It survives configuration changes such as screen rotations
class AddPlantViewModel(
    private val repository: SpeciesRepository,  // repo provides plant species data and search funtionality
    private val photos: PlantPhotoStorage //photo storage manages the plants image files
) : ViewModel() {



    var query by mutableStateOf("") //stores the text currently enteredin the species search field
        private set

    var searchState by mutableStateOf<SearchUiState>(SearchUiState.Idle) //stores the state of the current search
        private set

    //provides locally available plant species for suggestions
    val suggestions: List<Species> = LocalPlantCatalog.species

    //stores the currently running search coroutine, it allows an earlier search to be cancelled when a query is entered
    private var searchJob: Job? = null

    //called whenever user types or changes the search query
    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel() // cancel any pending or currently running search job
        val q = repository.normalize(newQuery) //normalize the current search field valie
        if (q.length < SpeciesRepository.MIN_QUERY_LENGTH) { //check whether the query meets the minimum length requirmenet
            searchState = SearchUiState.Idle // no valid search can be performed so reset the search state
            return
        }
        // Debounce: wait for a pause in typing so one word costs one request, not one per letter.
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MILLIS)
            runSearch(q)
        }
    }
    //starts immedietly without waiting for the debounce delay
    fun searchNow() {
        searchJob?.cancel()
        val q = repository.normalize(query)
        if (q.length < SpeciesRepository.MIN_QUERY_LENGTH) {
            searchState = SearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch { runSearch(q) } //launch a coroutine to perform the search asynchronously
    }

    //performs actual species search
    private suspend fun runSearch(normalizedQuery: String) { //suspend allows this function to perform suspending operations such as network requests without blocking the main thread
        searchState = SearchUiState.Loading //tell the UI that a search is currently in progress
        val outcome = repository.search(normalizedQuery)
        searchState = when { //decide which ui state should be displayed based on the search result
            outcome.species.isEmpty() && outcome.error != null -> SearchUiState.Error(outcome.error) // no species founf - error
            outcome.species.isEmpty() -> SearchUiState.Empty(normalizedQuery) //no species were found but no error
            else -> SearchUiState.Results(outcome.species, outcome.error) //show species found, and error
        }
    }

    var draft by mutableStateOf(PlantDraft())
        private set

    var errors by mutableStateOf(DraftErrors())
        private set

    var careInfo by mutableStateOf<CareInfoState>(CareInfoState.Idle)
        private set

    // If the user typed their own value, a late API answer must not overwrite it.
    private var intervalEdited = false //checks if user has manually edited watering interval. if true, fetched care information should not overwrite user's value
    private var lightEdited = false
    private var careJob: Job? = null //holds the coroutine that loads and enriches plant care information. It can be cancelled if another species is selected.

    //called when add plant screen recieves a species id from navigation
    fun loadSpecies(speciesId: Int) {
        if (draft.speciesId == speciesId) return //if this species is already loaded, do nothing
        careJob?.cancel() //cancel any care information request from the previous species
        intervalEdited = false //reset the manual edit flags for the newly selected species
        lightEdited = false
        errors = DraftErrors() //clear validation errors when loading a new species

        //check whether user selected manual entry

        if (speciesId == MANUAL_ENTRY) {
            draft = draft.copy( //copy creates a new plantdraft while preserving unspecified fields.
                speciesId = MANUAL_ENTRY, speciesName = "", nickname = "",
                wateringIntervalDays = "", lightRequirement = "" //reset the species related fields so the user can enter their own data
            )
            careInfo = CareInfoState.Idle //no external care info needed for manual entry
            return
        }

        //setup a clean draft for the selected species
        draft = draft.copy(
            speciesId = speciesId, speciesName = "", nickname = "",
            wateringIntervalDays = "", lightRequirement = ""
        )
        careJob = viewModelScope.launch { //load the selected species and its care information asynchronously

            //retrieve local species information first.
            val local = repository.getSpecies(speciesId)

            //if the species cannot be found locally, stop loading
            if (local == null) {
                careInfo = CareInfoState.Unavailable(null) //inform the UI that the care information is unavailable
                return@launch
            }
            applySpecies(local, firstTime = true) //apply locally  available information immediately.
            //firstTime = true allows the species name and initial nickname to be populated

            // Attempt to retrieve additional or updated care information.
            careInfo = CareInfoState.Loading
            val outcome = repository.enrichCareInfo(local) //ask repository to enrich the species with care information
            applySpecies(outcome.species, firstTime = false) //apply the returned data while respecting manually edited fields. firstime = false prevents the initial species name/nickname setup from being repeated
            careInfo = if (outcome.species.wateringIntervalDays != null) { // determine the final care information state
                CareInfoState.Loaded //watering information is available
            } else {
                CareInfoState.Unavailable(outcome.error) // watering information is still missing. pass the error
            }
        }
    }

    //copies relevant information from the species object into a form draft
    private fun applySpecies(species: Species, firstTime: Boolean) {
        var d = draft //starts with the current draft so existing user entered data is preserved
        if (firstTime) {
            d = d.copy(
                speciesName = species.commonName, //set plant's name from the catalogue
                nickname = if (d.nickname.isBlank()) species.commonName else d.nickname //if field is blank, use common name. else use name user entered
            )
        }
        //fill watering interval only if the user has not edited it and the species provides a watering interval
        if (!intervalEdited && species.wateringIntervalDays != null) {
            d = d.copy(wateringIntervalDays = species.wateringIntervalDays.toString()) //convert the numeric interval to a String for the form's text field
        }

        //fill the light requirement only if user has not edited it and species provides a non-empty light requirement
        if (!lightEdited && !species.lightRequirement.isNullOrBlank()) {
            d = d.copy(lightRequirement = species.lightRequirement)
        }
        draft = d //publish the updated draft so that compose can refresh the UI
    }

    //called whenever the user changes the plant's nickname
    fun onNicknameChange(value: String) {
        draft = draft.copy(nickname = value) //update only the nickname field, preserving the other draft values
        if (errors.nickname) errors = errors.copy(nickname = false) // if nickname previouslt has a validation error, clear that error when the user edits the field
    }

    fun onSpeciesNameChange(value: String) {
        draft = draft.copy(speciesName = value)
        if (errors.species) errors = errors.copy(species = false)
    }

    fun onLocationChange(value: String) {
        draft = draft.copy(location = value) //update location while preserving the rest of the form
    }

    fun onIntervalChange(value: String) {
        intervalEdited = true //mark field as manually edited. later care-information must not overwrite it
        draft = draft.copy(wateringIntervalDays = value.filter(Char::isDigit).take(3)) //keep only digit characters and limit input to three digits
        if (errors.interval) errors = errors.copy(interval = false)
    }

    fun onLightChange(value: String) {
        lightEdited = true
        draft = draft.copy(lightRequirement = value)
    }

    //CAMERA, GALLERY

    var photoBusy by mutableStateOf(false) //whether a importing image is in progress
        private set

    var photoError by mutableStateOf<String?>(null) //stores optional error message related to photo operations
        private set

    private var pendingCameraFile: File? = null //temporarily stores the file created for a camera photo. pending until camera reports its result

    //prepare a file before launching the camera
    fun prepareCameraShot(): Uri { //returns URI that the camera should write the photo to
        pendingCameraFile?.delete() //deleted older unfinished camera files if one exists
        val file = photos.newPhotoFile() //create a new file in the application's photo storage
        pendingCameraFile = file //remember the file until the camera result is recieved
        photoError = null
        return photos.uriFor(file) //convert the file into a URI that can be supplied to the camera
    }

    //handle the result returned by the camera
    fun onCameraResult(success: Boolean) { //success is true when camera reports success
        val file = pendingCameraFile ?: return // retrieve the pending camera file. If no file exists there is nothing to process
        pendingCameraFile = null // clear  the pending reference because this camera operation has finished
        if (success && file.length() > 0) setPhoto(file) else file.delete() //accept the photo only if the camera succeded and the file is non-empty. save the photo's URI in the plant draft
        //else delete the incomplete or cancelled camera file
    }

    fun onCameraUnavailable() { // called when the application cannot launch a camera app
        pendingCameraFile?.delete() //remove any unfinished camera file
        pendingCameraFile = null //clear the pending file reference
        photoError = "No camera app available on this device." //set an error message for the UI to display
    }

    fun onGalleryPicked(uri: Uri?) { //called when the user selects an image from the gallery. URI may be null if the user cancels the image picker
        if (uri == null) return  // do nothing when the user backs out of the gallery
        viewModelScope.launch { //import the selected image asynchronously
            photoBusy = true //tell UI that photo import is in progress
            photoError = null //clear any previous photo error
            val file = photos.importFromGallery(uri) //copy import the selected image into the app's photo storage
            photoBusy = false //mark import operation as finished
            if (file != null) setPhoto(file) else photoError = "Couldn't read that image." //if import is succedded, assign the photo to the draft. otherwise, show an error message
        }
    }

    //called when the user removes the selected plant photo
    fun removePhoto() {
        //delete the current photo file using the URI stored in the draft
        photos.deleteByUriString(draft.photoUri) //remove the photo reference from the draft
        draft = draft.copy(photoUri = null)
    }

    //assign new photo file to the current plant draft
    private fun setPhoto(file: File) {
        //delete the previous photo so replacing the image does not leave an unused file behind
        photos.deleteByUriString(draft.photoUri)
        draft = draft.copy(photoUri = photos.uriString(file)) //convert the new file into a URI string and store it in the drat. TheUI can use this URI to display the selected image
    }

    //validates current form and attempts to create plant entity.
    fun buildPlant(): Plant? {
        errors = draft.validate()
        return draft.toPlantOrNull() //return draft if successful, null if not
    }

    private var savedPendingClear = false //checks whether a successful save was done to the form and cleared after the screen leaves

    //called by the screen after the plant has been successfully saved.
    fun onPlantSaved() {
        savedPendingClear = true // do not clear form immediatly. mark for clearing when the add plant screen is disposed
    }

    //form cleared when save is sucessful
    fun onAddPlantScreenDisposed() {
        if (savedPendingClear) { //check whether successful save is waiting to be completed
            savedPendingClear = false //reset the flag so the draft is not cleared repeatedly
            clearDraft() //rest of the form and its temporarys state
        }
    }

    //reset form after successful save
    fun clearDraft() {
        careJob?.cancel()
        draft = PlantDraft()
        errors = DraftErrors()
        careInfo = CareInfoState.Idle
        intervalEdited = false
        lightEdited = false
        photoError = null
    }

    //called when user leaves without saving the plant
    fun discardDraft() {
        photos.deleteByUriString(draft.photoUri)
        pendingCameraFile?.delete()
        pendingCameraFile = null
        clearDraft()
    }

    private companion object { //stores constants and helper values associated with this viewmodel
        const val DEBOUNCE_MILLIS = 500L //the search waits  milliseconds after typing stops before making a search request.
    }
}
