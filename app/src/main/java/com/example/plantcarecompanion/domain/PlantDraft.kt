package com.example.plantcarecompanion.domain

import com.example.plantcarecompanion.data.local.entity.Plant

data class PlantDraft(
    val speciesId: Int? = null,
    val speciesName: String = "",
    val nickname: String = "",
    val location: String = "",
    val wateringIntervalDays: String = "",
    val lightRequirement: String = "",
    val photoUri: String? = null
)

data class DraftErrors(
    val nickname: Boolean = false,
    val species: Boolean = false,
    val interval: Boolean = false
) {
    val any: Boolean get() = nickname || species || interval
}

const val MIN_INTERVAL_DAYS = 1
const val MAX_INTERVAL_DAYS = 365

fun PlantDraft.validate(): DraftErrors {
    val interval = wateringIntervalDays.trim().toIntOrNull()
    return DraftErrors(
        nickname = nickname.isBlank(),
        species = speciesName.isBlank(),
        interval = interval == null || interval !in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS
    )
}

fun PlantDraft.toPlantOrNull(): Plant? {
    if (validate().any) return null
    return Plant(
        nickname = nickname.trim(),
        speciesName = speciesName.trim(),
        wateringIntervalDays = wateringIntervalDays.trim().toInt(),
        lightRequirement = lightRequirement.trim().ifBlank { null },
        location = location.trim().ifBlank { null },
        imageUri = photoUri
    )
}
