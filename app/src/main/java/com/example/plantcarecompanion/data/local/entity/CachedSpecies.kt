package com.example.plantcarecompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One Perenual species we have looked up successfully at least once.
 * This is the offline cache: it lets search + the Add Plant form keep working without a network.
 * [id] is Perenual's own species id (always positive).
 */
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
