package com.example.plantcarecompanion.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.example.plantcarecompanion.data.local.AppDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

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

    /**
     * Builds a JSON export of all plants and their care log entries (file names only for
     * photos, never the photo data itself) and writes it to the app cache. Returns null when
     * there is nothing to export.
     */
    suspend fun exportDataToJson(): File? = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val plants = db.plantDao().getAll()
        if (plants.isEmpty()) return@withContext null

        val careLogDao = db.careLogDao()
        val plantsArray = JSONArray()
        for (plant in plants) {
            val careLogs = careLogDao.getForPlant(plant.id)
            val careLogArray = JSONArray()
            for (log in careLogs) {
                careLogArray.put(
                    JSONObject().apply {
                        put("id", log.id)
                        put("careType", log.careType)
                        put("timestamp", log.timestamp)
                        put("notes", log.notes ?: JSONObject.NULL)
                    }
                )
            }
            plantsArray.put(
                JSONObject().apply {
                    put("id", plant.id)
                    put("nickname", plant.nickname)
                    put("speciesName", plant.speciesName)
                    put("wateringIntervalDays", plant.wateringIntervalDays)
                    put("lightRequirement", plant.lightRequirement ?: JSONObject.NULL)
                    put("location", plant.location ?: JSONObject.NULL)
                    put("imageFileName", plant.imageUri?.let { File(it).name } ?: JSONObject.NULL)
                    put("dateAdded", plant.dateAdded)
                    put("careLog", careLogArray)
                }
            )
        }

        val root = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("plants", plantsArray)
        }

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        exportsDir.listFiles()?.forEach { it.delete() }
        val file = File(exportsDir, "plant_care_export_${System.currentTimeMillis()}.json")
        file.writeText(root.toString(2))
        file
    }

    /** Nulls out each plant's stored photo reference, leaving the plant and its care log intact. */
    suspend fun clearPhotosOnly() = withContext(Dispatchers.IO) {
        val plantDao = AppDatabase.get(context).plantDao()
        plantDao.getAll()
            .filter { it.imageUri != null }
            .forEach { plant -> plantDao.update(plant.copy(imageUri = null)) }
    }

    fun appVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    } catch (e: PackageManager.NameNotFoundException) {
        "—"
    }
}
