package com.example.plantcarecompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CareLogDao {

    @Query("SELECT * FROM care_log_entries WHERE plantId = :plantId ORDER BY timestamp DESC")
    suspend fun getForPlant(plantId: Int): List<CareLogEntry>

    // Flow (not a one-shot suspend fun) so "watered" status can be derived live from Room instead
    // of cached ViewModel state that resets whenever the ViewModel is recreated, e.g. on tab
    // switches (Bug 1).
    @Query("SELECT * FROM care_log_entries ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<CareLogEntry>>

    @Insert
    suspend fun insert(entry: CareLogEntry)

    @Update
    suspend fun update(entry: CareLogEntry)

    @Delete
    suspend fun delete(entry: CareLogEntry)
}