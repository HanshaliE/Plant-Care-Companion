package com.example.plantcarecompanion

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.ProfileRepository
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.photo.PlantPhotoStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao,
    private val photos: PlantPhotoStorage
) : ViewModel() {

    var name by mutableStateOf("Plant Parent")
        private set

    var bio by mutableStateOf("")
        private set

    var photoPath by mutableStateOf<String?>(null)
        private set

    private var memberSinceMillis by mutableStateOf(0L)

    var notificationsEnabled by mutableStateOf(true)
        private set

    var plantCount by mutableStateOf(0)
        private set

    var careActionCount by mutableStateOf(0)
        private set

    var profileSaved by mutableStateOf(false)
        private set

    var photoError by mutableStateOf<String?>(null)
        private set

    // Tracks the file created for an in-flight camera shot (between launching the camera app and
    // getting its result back), so a cancelled/failed shot can clean up after itself.
    private var pendingCameraFile: File? = null

    /** Step 1 of taking a photo: returns the URI the camera app should write to. */
    fun prepareCameraShot(): Uri {
        pendingCameraFile?.delete() // in case a previous shot was abandoned without a result
        val file = photos.newPhotoFile()
        pendingCameraFile = file
        photoError = null
        return photos.uriFor(file)
    }

    /** Step 2: the camera app finished. Returns the captured file's path on success, or null (cancelled/failed). */
    fun onCameraResult(success: Boolean): String? {
        val file = pendingCameraFile ?: return null
        pendingCameraFile = null
        return if (success && file.length() > 0) {
            // File already lives permanently under filesDir/plant_photos/ — no copy needed.
            file.absolutePath
        } else {
            file.delete() // user backed out of the camera app, or it wrote nothing
            null
        }
    }

    fun onCameraUnavailable() {
        pendingCameraFile?.delete()
        pendingCameraFile = null
        photoError = "No camera app available on this device."
    }

    /** Called when the edit sheet closes without saving: throws away any not-yet-saved capture. */
    fun discardPendingCameraShot(capturedPhotoPath: String?) {
        pendingCameraFile?.delete()
        pendingCameraFile = null
        // capturedPhotoPath is the sheet's local "pending photo" state — only persisted once
        // saveProfile() stores it in ProfileRepository, so it's safe to delete here on cancel.
        if (capturedPhotoPath != null) File(capturedPhotoPath).delete()
    }

    val initial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"

    val memberSinceLabel: String
        get() {
            if (memberSinceMillis == 0L) return ""
            val formatted = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(memberSinceMillis))
            return "Plant parent since $formatted"
        }

    init {
        viewModelScope.launch {
            profileRepository.ensureMemberSinceSet()
            profileRepository.profile.collect { data ->
                name = data.name
                bio = data.bio
                memberSinceMillis = data.memberSinceMillis
                photoPath = data.photoPath?.takeIf { File(it).exists() }
            }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabled.collect { enabled ->
                notificationsEnabled = enabled
            }
        }
        viewModelScope.launch {
            val plants = plantDao.getAll()
            plantCount = plants.size
            careActionCount = plants.sumOf { careLogDao.getForPlant(it.id).size }
        }
    }

    fun saveProfile(newName: String, newBio: String, pickedGalleryUri: Uri?, capturedPhotoPath: String?) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            val newPhotoPath = when {
                // Already written to internal storage by the camera capture — no copy needed.
                capturedPhotoPath != null -> capturedPhotoPath
                pickedGalleryUri != null -> profileRepository.copyImageToInternalStorage(pickedGalleryUri)
                else -> null
            }
            profileRepository.saveProfile(trimmedName, newBio.trim(), newPhotoPath)
            profileSaved = true
        }
    }

    fun dismissProfileSaved() {
        profileSaved = false
    }
}
