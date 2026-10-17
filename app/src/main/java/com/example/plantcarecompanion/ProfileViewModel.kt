package com.example.plantcarecompanion

import android.net.Uri //the reference to an image selected by the user
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope //lets us launch coroutines that are cancelled automatically when the ViewModel is cleared
import com.example.plantcarecompanion.data.ProfileRepository
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch //launch is used to start coroutines for asynchronous operations

//Viewmodel manages the information displayed on the ProfileScreen
class ProfileViewModel(
    private val profileRepository: ProfileRepository,  //receives objects it needs instead of creating them itself (dependency injection)
    private val settingsRepository: SettingsRepository,
    private val plantDao: PlantDao,
    private val careLogDao: CareLogDao
) : ViewModel() {

    var name by mutableStateOf("Plant Parent")
        private set // other classes can read the name but only this viewmodel can directly change it

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

    //a property that returns the first letter of the user's name
    val initial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P" //if no name, us P as thr fallback initial

    //creates a readable label for the membershit timestamp
    val memberSinceLabel: String
        get() {
            if (memberSinceMillis == 0L) return "" // 0 means real membership date is not set or not loaded
            val formatted = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // creates a data formatter
                .format(Date(memberSinceMillis)) // coverts timestamp into a Date and formats it
            return "Plant parent since $formatted"
        }

    init { //init runs automatically when a ProfileView Model instance is created
        //starts the background tasks needed for load profile information, notification settings, plant statistics
        viewModelScope.launch {
            profileRepository.ensureMemberSinceSet() //repository handles the actual storage logic
            profileRepository.profile.collect { data -> // whenever the profile data changes, the code inside collect runs again
                //updates the ViewModel's state using thr latest saved profile. Compose can then update the UI to reflect these changes
                name = data.name
                bio = data.bio
                memberSinceMillis = data.memberSinceMillis
                photoPath = data.photoPath?.takeIf { File(it).exists() } //keep photo path only if the file still exists
            }
        }
        //starts another coroutine to observe notification settings
        viewModelScope.launch {
            //collects updates from the notification settings flow. This keeps the view model's value in sync with the saved settings
            settingsRepository.notificationsEnabled.collect { enabled ->
                notificationsEnabled = enabled //updates the state used by ProfileScreen
            }
        }
        viewModelScope.launch { //starts a coroutine to load plant and care-action statistics
            val plants = plantDao.getAll() //retrieves all plants from the local database
            plantCount = plants.size //The number of plants is the size of the returned list
            careActionCount = plants.sumOf { careLogDao.getForPlant(it.id).size } //for each plant, get its care logs and count them. sum of all those to get the total
        }
    }

    fun saveProfile(newName: String, newBio: String, pickedPhotoUri: Uri?) {
        val trimmedName = newName.trim() //removes unnessasary whitespaces from the name
        if (trimmedName.isBlank()) return // prevents empty or whitespaces being saved. return exists funtion immediately

        viewModelScope.launch { // starts a coroutine because saving the image and profile need file and database operations
            val newPhotoPath = pickedPhotoUri?.let { profileRepository.copyImageToInternalStorage(it) } //if user selects a photo copy it to app's internal storage and obtain saved file path
            profileRepository.saveProfile(trimmedName, newBio.trim(), newPhotoPath) //repository handles actual persistence. info saved
            profileSaved = true
        }
    }

    fun dismissProfileSaved() { //called by profile screen after success snackbar has been shown
        profileSaved = false //resets the flag so the same success message is not triggered repeatedly
    }
}
