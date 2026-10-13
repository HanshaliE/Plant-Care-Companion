package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.Plant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

enum class SortMode { DUE_DATE, NAME }

/** 0 = due today, 1 = due tomorrow, etc. Estimated from [Plant.dateAdded] and [Plant.wateringIntervalDays]. */
fun daysUntilWatering(plant: Plant): Int {
    val interval = plant.wateringIntervalDays.coerceAtLeast(1)
    val daysSinceAdded = ((System.currentTimeMillis() - plant.dateAdded) / DAY_MILLIS)
        .toInt()
        .coerceAtLeast(0)
    val daysSinceLastCycle = daysSinceAdded % interval
    return (interval - daysSinceLastCycle) % interval
}

class ReminderViewModel(
    private val plantDao: PlantDao,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    var remindersEnabled by mutableStateOf(false)
        private set

    var plants by mutableStateOf<List<Plant>>(emptyList())
        private set

    private var _sortMode by mutableStateOf(SortMode.DUE_DATE)
    val sortMode: SortMode get() = _sortMode

    var showCompleted by mutableStateOf(true)
        private set

    var wateredIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    val visiblePlants: List<Plant>
        get() {
            val filtered = if (showCompleted) plants else plants.filterNot { it.id in wateredIds }
            return when (sortMode) {
                SortMode.DUE_DATE -> filtered.sortedWith(
                    compareBy<Plant> { it.id in wateredIds }
                        .thenBy { daysUntilWatering(it) }
                        .thenBy { it.nickname }
                )
                SortMode.NAME -> filtered.sortedBy { it.nickname }
            }
        }

    val plantsDueToday: Int
        get() = plants.count { daysUntilWatering(it) == 0 && it.id !in wateredIds }

    init {
        viewModelScope.launch {
            plants = plantDao.getAll()
        }
    }

    fun nextWateringLabel(plant: Plant): String {
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysUntilWatering(plant))
        }
        return SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(target.time)
    }

    fun toggleWatered(plantId: Int) {
        wateredIds = if (plantId in wateredIds) wateredIds - plantId else wateredIds + plantId
    }

    fun setSortMode(mode: SortMode) {
        _sortMode = mode
    }

    fun toggleShowCompleted() {
        showCompleted = !showCompleted
    }

    fun enableReminders() = viewModelScope.launch {
        remindersEnabled = true
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
        plants.forEach { plant ->
            scheduler.cancelReminder(plant.id)
        }
    }

    fun scheduleCustomReminder(title: String, notes: String, dayOffset: Int?, hour: Int?) {
        scheduler.scheduleOneOff(title, notes, computeDelayMinutes(dayOffset, hour))
    }

    private fun computeDelayMinutes(dayOffset: Int?, hour: Int?): Long {
        if (dayOffset == null && hour == null) return 1L

        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset ?: 0)
            if (hour != null) {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val diffMinutes = (target.timeInMillis - System.currentTimeMillis()) / 60_000L
        return diffMinutes.coerceAtLeast(1L)
    }
}
