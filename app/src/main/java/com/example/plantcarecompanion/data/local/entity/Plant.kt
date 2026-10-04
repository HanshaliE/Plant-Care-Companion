package com.example.plantcarecompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nickname: String,
    val speciesName: String,
    val wateringIntervalDays: Int,
    val lightRequirement: String? = null,
    val location: String? = null,
    val imageUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)