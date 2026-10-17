package com.example.plantcarecompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// A standalone reminder created from the Reminders screen's "+" sheet (it has no plant picker,
// so it isn't tied to any Plant's watering schedule). Persisted in Room so the Reminders list it
// is added to shows it right away and keeps showing it across tab switches and app restarts
// (Bug 3) -- previously nothing was saved for the screen to read back.
@Entity(tableName = "custom_reminders")
data class CustomReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val notes: String,
    val triggerAtMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)
