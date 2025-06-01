package com.project.speciesdetection.core.services.backend.species

import com.project.speciesdetection.core.services.backend.dto.ApiPagedResponse
import com.project.speciesdetection.core.services.backend.dto.ApiSingleResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SpeciesApiService {
    @GET("api/v1/species")
    suspend fun getSpecies(
        @Query("pageSize") pageSize: Int,
        @Query("languageCode") languageCode: String,
        @Query("searchQuery") searchQuery: String? = null,
        @Query("classId") classId: String? = null,
        @Query("lastVisibleDocId") lastVisibleDocId: String? = null
    ): ApiPagedResponse

    @GET("api/v1/species/by-ids")
    suspend fun getSpeciesByIds(
        @Query("ids") ids: String,
        @Query("languageCode") languageCode: String
    ): ApiPagedResponse

    /*@GET("api/v1/species/classes")
    suspend fun getSpeciesClasses(): ApiSingleResponse<List<ApiSpeciesClassDto>>*/
}