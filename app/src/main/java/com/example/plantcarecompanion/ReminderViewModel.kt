package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.local.dao.PlantDao
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val plantDao: PlantDao,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    var remindersEnabled by mutableStateOf(false)
        private set

    fun enableReminders() = viewModelScope.launch {
        remindersEnabled = true
        val plants = plantDao.getAll()
        plants.forEach { plant ->
            scheduler.scheduleReminder(
                plantId = plant.id,
                plantName = plant.nickname,
                frequencyDays = plant.wateringIntervalDays
            )
        }
    }

    fun disableReminders() = viewModelScope.launch {
        remindersEnabled = false
        val plants = plantDao.getAll()
        plants.forEach { plant ->
            scheduler.cancelReminder(plant.id)
        }
    }
}