package com.example.plantcarecompanion.data.remote

import com.example.plantcarecompanion.data.remote.dto.SpeciesDto
import com.example.plantcarecompanion.data.remote.dto.SpeciesListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PerenualApi {

    @GET("species-list")
    suspend fun searchSpecies(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<SpeciesListResponse>

    @GET("species/details/{id}")
    suspend fun getSpeciesDetails(
        @Path("id") id: Int
    ): Response<SpeciesDto>
}