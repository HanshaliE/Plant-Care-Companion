package com.example.plantcarecompanion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantcarecompanion.data.SettingsRepository
import java.io.File
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    sealed interface ExportState {
        data object Idle : ExportState
        data object Working : ExportState
        data class Done(val file: File) : ExportState
        data object Empty : ExportState
        data class Error(val message: String) : ExportState
    }

    var notificationsEnabled by mutableStateOf(true)
        private set

    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    var dataCleared by mutableStateOf(false)
        private set

    var photosCleared by mutableStateOf(false)
        private set

    val appVersion: String = repository.appVersionName()

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

    fun exportData() {
        if (exportState is ExportState.Working) return
        exportState = ExportState.Working
        viewModelScope.launch {
            exportState = try {
                val file = repository.exportDataToJson()
                if (file != null) ExportState.Done(file) else ExportState.Empty
            } catch (e: Exception) {
                ExportState.Error(e.message ?: "Couldn't export your data.")
            }
        }
    }

    fun resetExportState() {
        exportState = ExportState.Idle
    }

    fun clearPhotosOnly() = viewModelScope.launch {
        repository.clearPhotosOnly()
        photosCleared = true
    }

    fun dismissPhotosClearedMessage() {
        photosCleared = false
    }

    fun clearLocalData() = viewModelScope.launch {
        repository.clearLocalData()
        dataCleared = true
    }

    fun dismissDataClearedMessage() {
        dataCleared = false
    }
}
