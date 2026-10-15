package com.example.plantcarecompanion.data.repository

import com.example.plantcarecompanion.data.local.entity.CachedSpecies
import com.example.plantcarecompanion.data.remote.dto.SpeciesDto
import com.example.plantcarecompanion.domain.Species
import com.example.plantcarecompanion.domain.SpeciesSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull


private fun String.isUpgradePlaceholder(): Boolean =
    contains("upgrade plans", ignoreCase = true) || contains("upgrade_access", ignoreCase = true)

internal fun JsonElement?.cleanText(): String? = when (this) {
    is JsonPrimitive -> if (isString) contentOrNull else null
    is JsonArray -> firstNotNullOfOrNull { it.cleanText() }
    else -> null
}?.trim()?.takeIf { it.isNotEmpty() && !it.isUpgradePlaceholder() }

internal fun JsonElement?.cleanTextList(): List<String> = when (this) {
    is JsonPrimitive -> listOfNotNull(cleanText())
    is JsonArray -> mapNotNull { it.cleanText() }
    else -> emptyList()
}

private val imageKeys = listOf("regular_url", "medium_url", "small_url", "thumbnail", "original_url")

internal fun JsonElement?.cleanImageUrl(): String? {
    val obj = this as? JsonObject ?: return null
    return imageKeys.firstNotNullOfOrNull { key -> obj[key].cleanText() }?.takeIf { it.startsWith("http") }
}

internal fun wateringLabelToDays(label: String?): Int? = when (label?.trim()?.lowercase()) {
    "frequent" -> 3
    "average" -> 7
    "minimum" -> 14
    "none" -> 30
    else -> null
}

internal fun parseBenchmarkDays(benchmark: JsonElement?): Int? {
    val obj = benchmark as? JsonObject ?: return null
    // value can be a string ("5-7"), a quoted string ("\"5-7\"") or a bare number (7)
    val value = (obj["value"] as? JsonPrimitive)?.contentOrNull?.takeIf { !it.isUpgradePlaceholder() } ?: return null
    val numbers = Regex("\\d+").findAll(value).map { it.value.toInt() }.toList()
    if (numbers.isEmpty()) return null
    val mid = numbers.average()
    val unit = obj["unit"].cleanText()?.lowercase().orEmpty()
    val days = if (unit.startsWith("week")) mid * 7 else mid
    return Math.round(days).toInt().coerceIn(1, 365)
}

private fun humaniseLight(raw: String): String = when (raw.trim().lowercase()) {
    "full_sun", "full sun" -> "Full sun"
    "part_shade", "part shade" -> "Part shade"
    "full_shade", "full shade" -> "Full shade"
    "sun-part_shade", "sun-part shade" -> "Sun / part shade"
    else -> raw.replace('_', ' ').replace('-', ' ').trim().replaceFirstChar { it.uppercase() }
}

internal fun JsonElement?.lightText(): String? =
    cleanTextList().map(::humaniseLight).distinct().joinToString(", ").ifBlank { null }

internal fun String.titleCase(): String =
    split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

internal fun SpeciesDto.toSpecies(source: SpeciesSource = SpeciesSource.LIVE): Species? {
    if (id <= 0) return null
    val name = commonName.cleanText()?.titleCase() ?: scientificName.cleanText() ?: return null
    val label = watering.cleanText()
    return Species(
        id = id,
        commonName = name,
        scientificName = scientificName.cleanText(),
        wateringIntervalDays = parseBenchmarkDays(wateringBenchmark) ?: wateringLabelToDays(label),
        wateringLabel = label,
        lightRequirement = sunlight.lightText(),
        imageUrl = defaultImage.cleanImageUrl(),
        source = source
    )
}

internal fun Species.toEntity(now: Long): CachedSpecies = CachedSpecies(
    id = id,
    commonName = commonName,
    scientificName = scientificName,
    wateringLabel = wateringLabel,
    wateringIntervalDays = wateringIntervalDays,
    lightRequirement = lightRequirement,
    imageUrl = imageUrl,
    cachedAt = now
)

internal fun CachedSpecies.toSpecies(): Species = Species(
    id = id,
    commonName = commonName,
    scientificName = scientificName,
    wateringIntervalDays = wateringIntervalDays,
    wateringLabel = wateringLabel,
    lightRequirement = lightRequirement,
    imageUrl = imageUrl,
    source = SpeciesSource.CACHED
)
