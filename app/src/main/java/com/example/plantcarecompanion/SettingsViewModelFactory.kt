package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.SettingsRepository

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = SettingsRepository(context)
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(repository) as T
    }
}
