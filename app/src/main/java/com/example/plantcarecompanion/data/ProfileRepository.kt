package com.example.plantcarecompanion.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.profileDataStore by preferencesDataStore(name = "profile")

data class ProfileData(
    val name: String,
    val bio: String,
    val photoPath: String?,
    val memberSinceMillis: Long
)

class ProfileRepository(private val context: Context) {

    private val nameKey = stringPreferencesKey("name")
    private val bioKey = stringPreferencesKey("bio")
    private val photoPathKey = stringPreferencesKey("photo_path")
    private val memberSinceKey = longPreferencesKey("member_since")

    val profile: Flow<ProfileData> = context.profileDataStore.data.map { prefs ->
        ProfileData(
            name = prefs[nameKey] ?: "Plant Parent",
            bio = prefs[bioKey] ?: "",
            photoPath = prefs[photoPathKey],
            memberSinceMillis = prefs[memberSinceKey] ?: 0L
        )
    }

    suspend fun ensureMemberSinceSet() {
        context.profileDataStore.edit { prefs ->
            if (prefs[memberSinceKey] == null) {
                prefs[memberSinceKey] = System.currentTimeMillis()
            }
        }
    }

    suspend fun saveProfile(name: String, bio: String, photoPath: String?) {
        context.profileDataStore.edit { prefs ->
            prefs[nameKey] = name
            prefs[bioKey] = bio
            if (photoPath != null) {
                prefs[photoPathKey] = photoPath
            }
        }
    }

    suspend fun copyImageToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val outFile = File(context.filesDir, "profile_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
