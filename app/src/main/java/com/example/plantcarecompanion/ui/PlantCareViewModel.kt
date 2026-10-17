package com.example.plantcarecompanion.ui
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.data.local.entity.Plant
import kotlinx.coroutines.launch

enum class PlantSortOption {
    RECENTLY_ADDED,
    NAME_A_TO_Z
}
class PlantCareViewModel(
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao
) : ViewModel() {

    var plants by mutableStateOf(listOf<Plant>())
        private set

    var careLogs by mutableStateOf(listOf<CareLogEntry>())
        private set

    var selectedPlant by mutableStateOf<Plant?>(null)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var sortOption by mutableStateOf(PlantSortOption.RECENTLY_ADDED)
        private set

    val lastWateredEntry: CareLogEntry?
        get() = careLogs.firstOrNull { entry ->
            entry.careType.equals("Watered", ignoreCase = true)
        }

    var careLogNote by mutableStateOf("")
        private set

    var editNickname by mutableStateOf("")
        private set

    var editLocation by mutableStateOf("")
        private set

    var editWateringInterval by mutableStateOf("")
        private set

    var editLightRequirement by mutableStateOf("")
        private set

    private var editingPlantId: Int? = null

    init {
        refreshPlants()
    }

    fun refreshPlants() = viewModelScope.launch {
        plants = plantDao.getAll()
    }

    fun addPlant(plant: Plant) = viewModelScope.launch {
        plantDao.insert(plant)
        refreshPlants()
    }

    fun updatePlant(plant: Plant) = viewModelScope.launch {
        plantDao.update(plant)
        refreshPlants()
    }

    fun deletePlant(plant: Plant) = viewModelScope.launch {
        plantDao.delete(plant)
        refreshPlants()
    }

    fun loadPlant(plantId: Int) = viewModelScope.launch {
        selectedPlant = plantDao.getById(plantId)
    }

    fun loadCareLogs(plantId: Int) = viewModelScope.launch {
        careLogs = careLogDao.getForPlant(plantId)
    }

    fun addCareLog(entry: CareLogEntry) = viewModelScope.launch {
        careLogDao.insert(entry)
        loadCareLogs(entry.plantId)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateSortOption(option: PlantSortOption) {
        sortOption = option
    }

    fun updateCareLogNote(note: String) {
        careLogNote = note
    }


    fun clearCareLogNote() {
        careLogNote = ""
    }

    //care log entry for notes such as watering
    fun addCareAction(plantId: Int, careType: String) {
        addCareLog(
            CareLogEntry(
                plantId = plantId, //identifies which plant recieves the care
                careType = careType, //describes the action, such as watered
                notes = careLogNote.trim().ifBlank { null } //removes unnecessary spaces from the note. stores null if note is empty or contains whitespaces
            )
        )

        clearCareLogNote() //clears the note field after stating the save operation
    }

    //returns the plant list after applying the current search query and sorting option
    val filteredPlants: List<Plant>
        get() {
            //if search field is empty or contains only whitespace, include all plants in the filtered list
            val filtered = if (searchQuery.isBlank()) {
                plants
            } else {
                //otherwise include plants whose nickname OR species name contains the search query
                plants.filter { plant ->
                    plant.nickname.contains(searchQuery, ignoreCase = true) ||
                            plant.speciesName.contains(searchQuery, ignoreCase = true)
                }
            }

            //apply the selected sorting method to the filtered list
            return when (sortOption) {
                PlantSortOption.RECENTLY_ADDED -> //sort by dateadded with the newest date first
                    filtered.sortedByDescending { it.dateAdded }

                PlantSortOption.NAME_A_TO_Z -> //sort alphabetically by the nickname.
                    filtered.sortedBy { it.nickname.lowercase() }
            }
        }

    //prepares the selected plant's existing details for editing
    fun prepareEditPlant(plant: Plant) {
        if (editingPlantId == plant.id) return // if plant is edited dont overwrite it

        editingPlantId = plant.id //remember which plant is being edited

        editNickname = plant.nickname //copy the current database values into the editable fields
        editLocation = plant.location ?: "" //convert the null location to an empty string for the text field
        editWateringInterval = plant.wateringIntervalDays.toString() //convert the integer watering interval to text so it can be displayed and edited in a text field
        editLightRequirement = plant.lightRequirement ?: "" //convert a null light requirement into an empty string
    }

    fun updateEditNickname(value: String) {
        editNickname = value
    }

    fun updateEditLocation(value: String) {
        editLocation = value
    }

    fun updateEditWateringInterval(value: String) {
        editWateringInterval = value
    }

    fun updateEditLightRequirement(value: String) {
        editLightRequirement = value
    }

    //validates the edit fields and saves the updated plant and return true if validation succeeds and the update is started
    fun saveEditedPlant(): Boolean {
        val plant = selectedPlant ?: return false //stop if no plant is selected

        val wateringDays = editWateringInterval.trim().toIntOrNull() //remove surrounding spaces and convert the watering level from text to integer

        if (editNickname.isBlank() || wateringDays == null || wateringDays <= 0) { //nickname cannot be blank, watering level must a valid integer greater than zero
            return false // do not save if any validation rule fails
        }

        //create a copy of selected plant with the edited value
        val updatedPlant = plant.copy(
            nickname = editNickname.trim(), //save the nickname with surrounding whitespace
            location = editLocation.trim().ifBlank { null }, //save null instead of empty location
            wateringIntervalDays = wateringDays, //save validated watering level as an integer
            lightRequirement = editLightRequirement.trim().ifBlank { null } //save null instead of an empty light requirement
        )

        selectedPlant = updatedPlant //update selected plant immedietly so UI can display without waiting for the database
        updatePlant(updatedPlant) //start saving the updated plant to the database

        return true
    }

    fun cancelEditPlant() { //cancel the current edit session by clearing the editing plant ID
        editingPlantId = null
    }
}