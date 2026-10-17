package com.example.plantcarecompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_species")
data class CachedSpecies(
    @PrimaryKey
    val id: Int,
    val commonName: String,
    val scientificName: String? = null,
    val wateringLabel: String? = null,
    val wateringIntervalDays: Int? = null,
    val lightRequirement: String? = null,
    val imageUrl: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
