package com.project.speciesdetection.data.model.species.repository

import androidx.paging.PagingData
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow

interface SpeciesRepository {
    // Lấy tất cả species (có thể kèm searchQuery), trả về Flow của PagingData
    fun getAll(
        uid : String?,
        searchQuery: List<String>?, // List các token tìm kiếm
        languageCode: String        // Mã ngôn ngữ hiện tại
    ): Flow<PagingData<DisplayableSpecies>>

    // Lấy species theo classId (có thể kèm searchQuery), trả về Flow của PagingData
    fun getSpeciesByClassPaged(
        uid : String?,
        searchQuery: List<String>?,
        classIdValue: String,       // ID của class để lọc
        languageCode: String
    ): Flow<PagingData<DisplayableSpecies>>

    // Lấy danh sách species theo danh sách các ID (không phân trang)
    suspend fun getSpeciesById(
        uid : String?,
        idList: List<String>,
        languageCode: String
    ): List<DisplayableSpecies>

    suspend fun getSpeciesDetails(
        speciesDocId: String,
        languageCode: String
    ): DisplayableSpecies?
}