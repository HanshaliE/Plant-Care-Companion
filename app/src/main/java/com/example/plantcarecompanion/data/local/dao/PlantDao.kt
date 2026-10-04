package com.example.plantcarecompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.plantcarecompanion.data.local.entity.Plant

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants")
    suspend fun getAll(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getById(plantId: Int): Plant?

    @Insert
    suspend fun insert(plant: Plant)

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}