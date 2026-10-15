package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.ProfileRepository
import com.example.plantcarecompanion.data.SettingsRepository
import com.example.plantcarecompanion.data.local.AppDatabase

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.get(context)
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(
            profileRepository = ProfileRepository(context),
            settingsRepository = SettingsRepository(context),
            plantDao = db.plantDao(),
            careLogDao = db.careLogDao()
        ) as T
    }
}
