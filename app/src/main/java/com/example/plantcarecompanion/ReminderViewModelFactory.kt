package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.AppDatabase

class ReminderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.get(context)
        val scheduler = ReminderScheduler(context)
        // Same SettingsRepository (same DataStore-backed Flow) that Settings/Profile already use,
        // so the reminders switch on all three screens reads and writes one source of truth (Bug 2).
        val settingsRepository = SettingsRepository(context)
        @Suppress("UNCHECKED_CAST")
        return ReminderViewModel(
            plantDao = db.plantDao(),
            careLogDao = db.careLogDao(),
            customReminderDao = db.customReminderDao(),
            settingsRepository = settingsRepository,
            scheduler = scheduler
        ) as T
    }
}