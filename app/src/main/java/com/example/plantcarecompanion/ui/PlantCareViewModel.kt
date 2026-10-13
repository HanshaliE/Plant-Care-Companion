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

    fun updateCareLog(entry: CareLogEntry) = viewModelScope.launch {
        careLogDao.update(entry)
        loadCareLogs(entry.plantId)
    }

    fun deleteCareLog(entry: CareLogEntry) = viewModelScope.launch {
        careLogDao.delete(entry)
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

    fun addCareAction(plantId: Int, careType: String) {
        addCareLog(
            CareLogEntry(
                plantId = plantId,
                careType = careType,
                notes = careLogNote.trim().ifBlank { null }
            )
        )

        clearCareLogNote()
    }

    val filteredPlants: List<Plant>
        get() {
            val filtered = if (searchQuery.isBlank()) {
                plants
            } else {
                plants.filter { plant ->
                    plant.nickname.contains(searchQuery, ignoreCase = true) ||
                            plant.speciesName.contains(searchQuery, ignoreCase = true)
                }
            }

            return when (sortOption) {
                PlantSortOption.RECENTLY_ADDED ->
                    filtered.sortedByDescending { it.dateAdded }

                PlantSortOption.NAME_A_TO_Z ->
                    filtered.sortedBy { it.nickname.lowercase() }
            }
        }
}