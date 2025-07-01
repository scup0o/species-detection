package com.project.speciesdetection.core.services.backend.species

import com.project.speciesdetection.core.services.backend.dto.ApiListResponse
import com.project.speciesdetection.core.services.backend.dto.ApiPagedResponse
import com.project.speciesdetection.core.services.backend.dto.ApiSingleResponse
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpeciesApiService {
    @GET("api/v1/species")
    suspend fun getSpecies(
        @Query("pageSize") pageSize: Int,
        @Query("languageCode") languageCode: String,
        @Query("sortByDesc") sortByDesc: Boolean = false,
    @Query("searchQuery") searchQuery: String? = null,
    @Query("classId") classId: String? = null,
    @Query("lastVisibleDocId") lastVisibleDocId: String? = null,
    @Query("uid") uid: String
    ): ApiPagedResponse

    @GET("api/v1/species/by-ids")
    suspend fun getSpeciesByIds(
        @Query("ids") ids: String,
        @Query("languageCode") languageCode: String,
        @Query("uid") uid : String
    ): ApiPagedResponse

    @GET("api/v1/species/detail/{speciesDocId}") // {speciesDocId} là path parameter
    suspend fun getSingleSpeciesById(
        @Path("speciesDocId") speciesDocId: String, // Giá trị này sẽ thay thế {speciesDocId} trong URL
        @Query("languageCode") languageCode: String
    ): ApiSingleResponse<DisplayableSpecies>

    @GET("api/v1/species/all") // <-- Thay "species/all" bằng đường dẫn API thực tế của bạn
    suspend fun getAllSpeciesForLanguage(
        @Query("languageCode") languageCode: String
    ): ApiListResponse<DisplayableSpecies> // Hoặc ApiListResponse<DisplayableSpecies>


    /*@GET("api/v1/species/classes")
    suspend fun getSpeciesClasses(): ApiSingleResponse<List<ApiSpeciesClassDto>>*/
}