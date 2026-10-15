package com.example.plantcarecompanion

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title")
        val notes = inputData.getString("notes")

        if (title != null) {
            showNotification(
                notificationId = System.currentTimeMillis().toInt(),
                contentTitle = title,
                contentText = notes?.takeIf { it.isNotBlank() } ?: "Tap to open Plant Care"
            )
            return Result.success()
        }

        val plantName = inputData.getString("plantName") ?: return Result.failure()
        val plantId = inputData.getInt("plantId", -1)

        showNotification(
            notificationId = plantId,
            contentTitle = "Time to water $plantName",
            contentText = "Tap to open Plant Care and log it"
        )
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(notificationId: Int, contentTitle: String, contentText: String) {
        val channelId = "watering_reminders"
        val manager = applicationContext.getSystemService(NotificationManager::class.java)

        // Notification channels only exist on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId, "Watering reminders", NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setAutoCancel(true)
            .build()

        // Runtime check — required on API 33+ (Android 13)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }
    }
}