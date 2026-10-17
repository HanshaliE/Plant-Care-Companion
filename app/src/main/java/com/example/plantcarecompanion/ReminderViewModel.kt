package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.data.local.entity.Plant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

enum class SortMode { DUE_DATE, NAME }

/**
 * 0 = due today, 1 = due tomorrow, etc.
 * When [lastWateredMillis] is known (a real "Watered" care log entry exists) the next due date
 * counts forward from that, floored at 0 (overdue shows the same as due today).
 * Otherwise it falls back to a repeating estimate from [Plant.dateAdded] for a plant that has
 * never been watered yet.
 */
fun daysUntilWatering(plant: Plant, lastWateredMillis: Long? = null): Int {
    val interval = plant.wateringIntervalDays.coerceAtLeast(1)
    if (lastWateredMillis != null) {
        val daysSinceWatered = ((System.currentTimeMillis() - lastWateredMillis) / DAY_MILLIS)
            .toInt()
            .coerceAtLeast(0)
        return (interval - daysSinceWatered).coerceAtLeast(0)
    }
    val daysSinceAdded = ((System.currentTimeMillis() - plant.dateAdded) / DAY_MILLIS)
        .toInt()
        .coerceAtLeast(0)
    val daysSinceLastCycle = daysSinceAdded % interval
    return (interval - daysSinceLastCycle) % interval
}

class ReminderViewModel(
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao,
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

    /** plantId -> timestamp of its most recent "Watered" care log entry, if any. */
    var lastWateredMillis by mutableStateOf<Map<Int, Long>>(emptyMap())
        private set

    val visiblePlants: List<Plant>
        get() {
            val filtered = if (showCompleted) plants else plants.filterNot { it.id in wateredIds }
            return when (sortMode) {
                SortMode.DUE_DATE -> filtered.sortedWith(
                    compareBy<Plant> { it.id in wateredIds }
                        .thenBy { daysUntilNextWatering(it) }
                        .thenBy { it.nickname }
                )
                SortMode.NAME -> filtered.sortedBy { it.nickname }
            }
        }

    val plantsDueToday: Int
        get() = plants.count { daysUntilNextWatering(it) == 0 && it.id !in wateredIds }

    init {
        viewModelScope.launch {
            plants = plantDao.getAll()
            refreshLastWatered()
        }
    }

    private suspend fun refreshLastWatered() {
        val updated = mutableMapOf<Int, Long>()
        plants.forEach { plant ->
            // getForPlant() is already sorted by timestamp DESC, so the first "Watered" match is the latest.
            val latest = careLogDao.getForPlant(plant.id)
                .firstOrNull { it.careType.equals("Watered", ignoreCase = true) }
            // A plant with no entry here falls back to the dateAdded-based estimate in daysUntilWatering().
            if (latest != null) updated[plant.id] = latest.timestamp
        }
        lastWateredMillis = updated
    }

    /** [daysUntilWatering] using this plant's real watering history, if any has been recorded. */
    fun daysUntilNextWatering(plant: Plant): Int = daysUntilWatering(plant, lastWateredMillis[plant.id])

    fun nextWateringLabel(plant: Plant): String {
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysUntilNextWatering(plant))
        }
        return SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(target.time)
    }

    /**
     * Marking watered is one-directional (no "undo"): it writes a real CareLogEntry so
     * PlantDetailsScreen/CareLogScreen pick it up (same CareLogDao/Room instance, re-queried on
     * next screen entry), moves this plant's due date out by a full interval, and re-enqueues its
     * periodic WorkManager reminder so the next notification lands on the new schedule.
     */
    fun toggleWatered(plantId: Int) {
        if (plantId in wateredIds) return // already marked watered this session
        val plant = plants.firstOrNull { it.id == plantId } ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            careLogDao.insert(CareLogEntry(plantId = plantId, careType = "Watered", timestamp = now))
            lastWateredMillis = lastWateredMillis + (plantId to now)
            wateredIds = wateredIds + plantId // drives the checkmark / "Watered" chip immediately

            // Only touch WorkManager if the user actually has reminders on — matches
            // enableReminders()/disableReminders() below, which gate scheduling the same way.
            if (remindersEnabled) {
                scheduler.scheduleReminder(
                    plantId = plant.id,
                    plantName = plant.nickname,
                    frequencyDays = plant.wateringIntervalDays
                )
            }
        }
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
