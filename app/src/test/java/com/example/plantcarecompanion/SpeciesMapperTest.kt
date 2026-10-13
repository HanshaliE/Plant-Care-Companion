package com.example.plantcarecompanion

import com.example.plantcarecompanion.data.remote.NetworkModule
import com.example.plantcarecompanion.data.remote.dto.SpeciesListResponse
import com.example.plantcarecompanion.data.repository.toSpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeciesMapperTest {

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun parse(text: String) = json.decodeFromString<SpeciesListResponse>(text).data

    @Test
    fun mapsNormalFreeTierItem() {
        val s = parse(
            """{"data":[{"id":12,"common_name":"snake plant","scientific_name":["Dracaena trifasciata"],
            "watering":"Minimum","sunlight":["part shade","full sun"],
            "default_image":{"regular_url":"https://example.com/a.jpg"}}]}"""
        ).first().toSpecies()!!

        assertEquals("Snake Plant", s.commonName)
        assertEquals("Dracaena trifasciata", s.scientificName)
        assertEquals(14, s.wateringIntervalDays)
        assertEquals("Part shade, Full sun", s.lightRequirement)
        assertEquals("https://example.com/a.jpg", s.imageUrl)
    }

    @Test
    fun upgradePlaceholdersBecomeNullInsteadOfCrashing() {
        val s = parse(
            """{"data":[{"id":4000,"common_name":"tomato",
            "scientific_name":"Upgrade Plans To Premium/Supreme - https://perenual.com/pricing",
            "watering":"Upgrade Plans To Premium/Supreme - https://perenual.com/pricing",
            "sunlight":"Upgrade Plans To Premium/Supreme - https://perenual.com/pricing",
            "default_image":{"regular_url":"https://perenual.com/storage/image/upgrade_access.jpg"}}]}"""
        ).first().toSpecies()!!

        assertEquals("Tomato", s.commonName)
        assertNull(s.scientificName)
        assertNull(s.wateringIntervalDays)
        assertNull(s.lightRequirement)
        assertNull(s.imageUrl)
    }

    @Test
    fun missingFieldsAndNullImageAreFine() {
        val s = parse("""{"data":[{"id":7,"common_name":"fern","default_image":null}]}""").first().toSpecies()!!
        assertEquals("Fern", s.commonName)
        assertNull(s.imageUrl)
    }

    @Test
    fun benchmarkRangeBecomesMidpointDays() {
        val s = parse(
            """{"data":[{"id":9,"common_name":"x","watering":"Average",
            "watering_general_benchmark":{"value":"\"5-9\"","unit":"days"}}]}"""
        ).first().toSpecies()!!
        assertEquals(7, s.wateringIntervalDays)          // benchmark wins over the "Average" label
    }

    @Test
    fun entriesWithoutIdAreDropped() {
        assertNull(parse("""{"data":[{"common_name":"ghost"}]}""").first().toSpecies())
    }

    @Test
    fun networkModuleJsonIsLenient() {
        // Same settings the app uses: unknown keys must not throw.
        NetworkModule.json.decodeFromString<SpeciesListResponse>("""{"data":[],"brand_new_field":123}""")
    }
}
