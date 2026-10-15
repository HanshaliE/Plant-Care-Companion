package com.example.plantcarecompanion

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(plantId: Int, plantName: String, frequencyDays: Int) {
        val data = workDataOf(
            "plantName" to plantName,
            "plantId" to plantId
        )

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            frequencyDays.toLong(), TimeUnit.DAYS
        ).setInputData(data).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reminder_$plantId",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelReminder(plantId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$plantId")
    }

    fun scheduleOneOff(title: String, notes: String, delayMinutes: Long) {
        val data = workDataOf(
            "title" to title,
            "notes" to notes
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}