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

class PlantCareViewModel(
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao
) : ViewModel() {

    var plants by mutableStateOf(listOf<Plant>())
        private set

    var careLogs by mutableStateOf(listOf<CareLogEntry>())
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
}