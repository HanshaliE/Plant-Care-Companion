package com.example.plantcarecompanion.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SpeciesListResponse(
    val data: List<SpeciesDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)

@Serializable
data class SpeciesDto(
    val id: Int = 0,
    @SerialName("common_name") val commonName: JsonElement? = null,
    @SerialName("scientific_name") val scientificName: JsonElement? = null,
    val watering: JsonElement? = null,
    val sunlight: JsonElement? = null,
    @SerialName("watering_general_benchmark") val wateringBenchmark: JsonElement? = null,
    @SerialName("default_image") val defaultImage: JsonElement? = null
)
