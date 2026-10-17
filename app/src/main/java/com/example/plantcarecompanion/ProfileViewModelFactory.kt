package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.ProfileRepository
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.AppDatabase

//this factory is responsible for creating ProfileViewModel objects.
class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {  //ViewModelProvider.Factory is called when android wants to create a viewmodel
    override fun <T : ViewModel> create(modelClass: Class<T>): T { // modelclass is the tyoe of viewmodel requested. T is generic type, so this function can return different viewmodel types
        val db = AppDatabase.get(context) //app reuses an existing database instead od creating a new one everytime this factory is called
        @Suppress("UNCHECKED_CAST") //suppresses a compiler warnig about an unchecked type cast
        return ProfileViewModel(
            profileRepository = ProfileRepository(context), //creates repository responsible for profile information
            settingsRepository = SettingsRepository(context), //repository for app settings
            plantDao = db.plantDao(), //dao for accessing plant records
            careLogDao = db.careLogDao() //dao for accessing care-action records
        ) as T //converts profileviewmodel to the requested generic type
    }
}
