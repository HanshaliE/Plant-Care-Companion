package com.example.plantcarecompanion.domain
enum class SpeciesSource { LIVE, CACHED, BUILT_IN }

data class Species(
    val id: Int,
    val commonName: String,
    val scientificName: String? = null,
    val wateringIntervalDays: Int? = null,
    val wateringLabel: String? = null,
    val lightRequirement: String? = null,
    val imageUrl: String? = null,
    val source: SpeciesSource = SpeciesSource.LIVE
)
const val MANUAL_ENTRY = 0
