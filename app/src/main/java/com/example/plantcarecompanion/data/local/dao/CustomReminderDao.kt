package com.example.plantcarecompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.plantcarecompanion.data.local.entity.CustomReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomReminderDao {

    // Flow so the Reminders list updates itself the instant a new reminder is inserted, with no
    // manual re-fetch needed on the screen's side (Bug 3).
    @Query("SELECT * FROM custom_reminders ORDER BY triggerAtMillis ASC")
    fun getAllFlow(): Flow<List<CustomReminder>>

    @Insert
    suspend fun insert(reminder: CustomReminder)
}
