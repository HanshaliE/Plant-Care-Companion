package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.local.AppDatabase

class ReminderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val plantDao = AppDatabase.get(context).plantDao()
        val scheduler = ReminderScheduler(context)
        @Suppress("UNCHECKED_CAST")
        return ReminderViewModel(plantDao, scheduler) as T
    }
}