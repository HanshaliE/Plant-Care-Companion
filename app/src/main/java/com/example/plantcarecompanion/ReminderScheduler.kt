package com.example.plantcarecompanion

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

//this class schedules and cancels plant care reminders. Acts as the connection between the app's reminder features and Android's WorkManager background task system
class ReminderScheduler(private val context: Context) {

    //Shedules a reminder that repeats at a specified interval
    fun scheduleReminder(plantId: Int, plantName: String, frequencyDays: Int) { //frequency days specifies how many days should separate repeat intervals
        val data = workDataOf( //packages the information that reminderworker needs when it runs
            "plantName" to plantName,
            "plantId" to plantId
        )

        val request = PeriodicWorkRequestBuilder<ReminderWorker>( //beuilds a repeated background task for reminderworker
            frequencyDays.toLong(), TimeUnit.DAYS //converts into to long because workmanager expevts the repeat interval as long, interval is measured in days
        ).setInputData(data).build() //  attaches plant information so reminderworker can indentify which plant the reminder belongs to

        WorkManager.getInstance(context).enqueueUniquePeriodicWork( //shedules the periodic task using a unique name for the plant
            "reminder_$plantId",
            ExistingPeriodicWorkPolicy.UPDATE, //replaces the existing work specifivation with the new one when periodic task with this unique name already exists
            request //the task that workmanager should shedule
        )
    }

    fun cancelReminder(plantId: Int) { //cancel the repeating reminder for a perticular plant
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$plantId")
    }

    //shedules a reminder that can run once after a specified delay
    fun scheduleOneOff(title: String, notes: String, delayMinutes: Long) {
        val data = workDataOf(
            "title" to title,
            "notes" to notes
        )

        //creates a one time background work request
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES) // sets the initial delay before the task can run
            .setInputData(data) //passes the title and notes to reminder worker
            .build() // builds the final request

        WorkManager.getInstance(context).enqueue(request) //enqueues the task so workmanager can run it when eligible
    }
}