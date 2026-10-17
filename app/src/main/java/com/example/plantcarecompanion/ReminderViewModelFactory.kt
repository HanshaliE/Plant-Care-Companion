package com.example.plantcarecompanion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plantcarecompanion.data.local.AppDatabase

class ReminderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.get(context)
        val scheduler = ReminderScheduler(context)
        @Suppress("UNCHECKED_CAST")
        return ReminderViewModel(db.plantDao(), db.careLogDao(), scheduler) as T
    }
}