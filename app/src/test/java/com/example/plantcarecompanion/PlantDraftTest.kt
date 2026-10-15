package com.example.plantcarecompanion

import com.example.plantcarecompanion.domain.PlantDraft
import com.example.plantcarecompanion.domain.toPlantOrNull
import com.example.plantcarecompanion.domain.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantDraftTest {

    private val valid = PlantDraft(
        speciesId = 1, speciesName = "Pothos", nickname = "Penny",
        wateringIntervalDays = "7", location = " Kitchen ", lightRequirement = ""
    )

    @Test
    fun validDraftBecomesPlant() {
        val plant = valid.toPlantOrNull()!!
        assertEquals("Penny", plant.nickname)
        assertEquals("Pothos", plant.speciesName)
        assertEquals(7, plant.wateringIntervalDays)
        assertEquals("Kitchen", plant.location)       // trimmed
        assertNull(plant.lightRequirement)            // blank -> null
    }

    @Test
    fun blankNicknameAndSpeciesAreRejected() {
        val errors = valid.copy(nickname = "  ", speciesName = "").validate()
        assertTrue(errors.nickname)
        assertTrue(errors.species)
        assertNull(valid.copy(nickname = "").toPlantOrNull())
    }

    @Test
    fun intervalMustBeBetween1And365() {
        assertTrue(valid.copy(wateringIntervalDays = "").validate().interval)
        assertTrue(valid.copy(wateringIntervalDays = "0").validate().interval)
        assertTrue(valid.copy(wateringIntervalDays = "366").validate().interval)
        assertFalse(valid.copy(wateringIntervalDays = "365").validate().interval)
        assertNotNull(valid.copy(wateringIntervalDays = "1").toPlantOrNull())
    }
}
