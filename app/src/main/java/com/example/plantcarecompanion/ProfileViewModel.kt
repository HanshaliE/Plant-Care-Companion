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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao
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

    fun saveProfile(newName: String, newBio: String, pickedPhotoUri: Uri?) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            val newPhotoPath = pickedPhotoUri?.let { profileRepository.copyImageToInternalStorage(it) }
            profileRepository.saveProfile(trimmedName, newBio.trim(), newPhotoPath)
            profileSaved = true
        }
    }

    fun dismissProfileSaved() {
        profileSaved = false
    }
}
