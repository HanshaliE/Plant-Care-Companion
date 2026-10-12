package com.example.plantcarecompanion.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.example.plantcarecompanion.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")

    val notificationsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[notificationsEnabledKey] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[notificationsEnabledKey] = enabled
        }
    }

    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelAllWork()
        AppDatabase.get(context).clearAllTables()
        context.settingsDataStore.edit { it.clear() }
    }
}
