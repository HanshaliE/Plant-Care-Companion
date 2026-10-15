package com.example.plantcarecompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.plantcarecompanion.data.local.entity.CachedSpecies

@Dao
interface SpeciesDao {
    @Upsert
    suspend fun upsertAll(species: List<CachedSpecies>)

    @Upsert
    suspend fun upsert(species: CachedSpecies)

    @Query("SELECT * FROM cached_species WHERE id = :id")
    suspend fun getById(id: Int): CachedSpecies?

    @Query(
        "SELECT * FROM cached_species " +
            "WHERE commonName LIKE '%' || :query || '%' OR scientificName LIKE '%' || :query || '%' " +
            "ORDER BY commonName LIMIT :limit"
    )
    suspend fun search(query: String, limit: Int): List<CachedSpecies>
}
