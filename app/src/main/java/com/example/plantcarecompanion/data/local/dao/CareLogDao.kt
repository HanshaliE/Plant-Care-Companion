package com.example.plantcarecompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.plantcarecompanion.data.local.entity.CareLogEntry

@Dao
interface CareLogDao {

    @Query("SELECT * FROM care_log_entries WHERE plantId = :plantId ORDER BY timestamp DESC")
    suspend fun getForPlant(plantId: Int): List<CareLogEntry>

    @Insert
    suspend fun insert(entry: CareLogEntry)

    @Update
    suspend fun update(entry: CareLogEntry)

    @Delete
    suspend fun delete(entry: CareLogEntry)
}