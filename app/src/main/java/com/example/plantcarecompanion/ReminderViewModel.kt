package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.CustomReminderDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.data.local.entity.CustomReminder
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
    private val customReminderDao: CustomReminderDao,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    // Backed by SettingsRepository's DataStore Flow (collected in init below), the same
    // preference Settings and Profile read/write -- not a ViewModel-local flag only this screen
    // ever saw, which is what let the switch disagree with the other two screens (Bug 2).
    var remindersEnabled by mutableStateOf(true)
        private set

    var plants by mutableStateOf<List<Plant>>(emptyList())
        private set

    private var _sortMode by mutableStateOf(SortMode.DUE_DATE)
    val sortMode: SortMode get() = _sortMode

    var showCompleted by mutableStateOf(true)
        private set

    /** plantId -> timestamp of its most recent "Watered" care log entry, if any. */
    var lastWateredMillis by mutableStateOf<Map<Int, Long>>(emptyMap())
        private set

    /** Reminders added from the "+" sheet; persisted so they survive tab switches and restarts (Bug 3). */
    var customReminders by mutableStateOf<List<CustomReminder>>(emptyList())
        private set

    /**
     * True while the plant is still inside its current watering cycle (watered more recently than
     * one full interval ago). Derived straight from [lastWateredMillis], which itself comes from a
     * Room Flow, so the checkmark survives tab switches/rotation/app restart and resets itself
     * automatically once the next due date arrives -- there's no separate "watered today" flag
     * that can go stale (Bug 1).
     */
    fun isWatered(plant: Plant): Boolean {
        val last = lastWateredMillis[plant.id] ?: return false
        return daysUntilWatering(plant, last) > 0
    }

    val visiblePlants: List<Plant>
        get() {
            val filtered = if (showCompleted) plants else plants.filterNot { isWatered(it) }
            return when (sortMode) {
                SortMode.DUE_DATE -> filtered.sortedWith(
                    compareBy<Plant> { isWatered(it) }
                        .thenBy { daysUntilNextWatering(it) }
                        .thenBy { it.nickname }
                )
                SortMode.NAME -> filtered.sortedBy { it.nickname }
            }
        }

    val plantsDueToday: Int
        get() = plants.count { daysUntilNextWatering(it) == 0 && !isWatered(it) }

    init {
        viewModelScope.launch {
            plants = plantDao.getAll()
        }

        // Collected as a Flow straight from Room: this re-derives "watered" status/next due date
        // from the real care log every time the ViewModel is (re)created, which is what makes
        // Bug 1's fix work even though the ViewModel itself still gets torn down on tab switches.
        viewModelScope.launch {
            careLogDao.getAllFlow().collect { entries ->
                lastWateredMillis = entries
                    .filter { it.careType.equals("Watered", ignoreCase = true) }
                    .groupBy { it.plantId }
                    .mapValues { (_, forPlant) -> forPlant.maxOf { it.timestamp } }
            }
        }

        // Single source of truth for the reminders toggle, shared with Settings/Profile (Bug 2).
        viewModelScope.launch {
            settingsRepository.notificationsEnabled.collect { enabled ->
                remindersEnabled = enabled
            }
        }

        // Custom reminders list, kept live from Room so it updates immediately after adding one (Bug 3).
        viewModelScope.launch {
            customReminderDao.getAllFlow().collect { reminders ->
                customReminders = reminders
            }
        }
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
     * Marking watered writes a real CareLogEntry so PlantDetailsScreen/CareLogScreen pick it up
     * (same CareLogDao/Room instance) and so this screen's own checkmark updates, via the
     * getAllFlow() collector in init -- there's no separate in-memory flag to keep in sync (Bug 1).
     */
    fun toggleWatered(plantId: Int) {
        val plant = plants.firstOrNull { it.id == plantId } ?: return
        if (isWatered(plant)) return // already watered this cycle, per the persisted care log

        viewModelScope.launch {
            careLogDao.insert(
                CareLogEntry(plantId = plantId, careType = "Watered", timestamp = System.currentTimeMillis())
            )
            // lastWateredMillis updates itself from the getAllFlow() collector in init once Room's
            // invalidation tracker notices this insert -- no manual map update needed here.

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
        // Writes the shared DataStore preference; remindersEnabled flips once the Flow collected
        // in init re-emits, which is what keeps Reminders/Settings/Profile in agreement (Bug 2).
        settingsRepository.setNotificationsEnabled(true)
        plants.forEach { plant ->
            scheduler.scheduleReminder(
                plantId = plant.id,
                plantName = plant.nickname,
                frequencyDays = plant.wateringIntervalDays
            )
        }
    }

    fun disableReminders() = viewModelScope.launch {
        settingsRepository.setNotificationsEnabled(false)
        plants.forEach { plant ->
            scheduler.cancelReminder(plant.id)
        }
    }

    /**
     * NewReminderSheet has no plant picker, so a custom reminder can't be tied to a specific
     * plant's watering schedule -- it's saved as its own persisted row (Bug 3) and scheduled as a
     * one-off WorkManager notification, same as before.
     */
    fun scheduleCustomReminder(title: String, notes: String, dayOffset: Int?, hour: Int?) {
        val triggerAtMillis = computeTriggerAtMillis(dayOffset, hour)

        // Persisted so it shows up in the list immediately via the getAllFlow() collector in
        // init, and is still there after a tab switch or app restart (Bug 3).
        viewModelScope.launch {
            customReminderDao.insert(
                CustomReminder(title = title, notes = notes, triggerAtMillis = triggerAtMillis)
            )
        }

        val delayMinutes = ((triggerAtMillis - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L)
        scheduler.scheduleOneOff(title, notes, delayMinutes)
    }

    private fun computeTriggerAtMillis(dayOffset: Int?, hour: Int?): Long {
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset ?: 0)
            if (hour != null) {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        return target.timeInMillis
    }
}
