package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    var notificationsEnabled by mutableStateOf(true)
        private set

    var dataCleared by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.notificationsEnabled.collect { enabled ->
                notificationsEnabled = enabled
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setNotificationsEnabled(enabled)
    }

    fun clearLocalData() = viewModelScope.launch {
        repository.clearLocalData()
        dataCleared = true
    }

    fun dismissDataClearedMessage() {
        dataCleared = false
    }
}
