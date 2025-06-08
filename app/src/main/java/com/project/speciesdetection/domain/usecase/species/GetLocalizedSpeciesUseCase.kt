package com.project.speciesdetection.domain.usecase.species

import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.paging.PagingData
import androidx.paging.map
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.data.model.species_class.repository.RemoteSpeciesClassRepository
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesUseCase @Inject constructor(
    private val speciesRepository: SpeciesRepository,

    @Named("language_provider") private val languageProvider: LanguageProvider
) {
    fun getAll(searchQuery: String): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguageCode = languageProvider.getCurrentLanguageCode()
        // Xử lý searchQuery: chuyển về chữ thường, bỏ khoảng trắng thừa, tách thành list token
        val processedSearchQuery = if (searchQuery.isNotBlank()) {
            searchQuery.lowercase(Locale.getDefault()).trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        } else {
            null // Nếu searchQuery rỗng, không gửi gì lên API
        }
        return speciesRepository.getAll(
            searchQuery = processedSearchQuery,
            languageCode = currentLanguageCode // Repository có thể dùng hoặc PagingSource tự lấy
        )
    }

    fun getByClassPaged(

        searchQuery: String,
        classIdValue: String,
    ): Flow<PagingData<DisplayableSpecies>> {
        val currentLanguageCode = languageProvider.getCurrentLanguageCode()
        val processedSearchQuery = if (searchQuery.isNotBlank()) {
            searchQuery.lowercase(Locale.getDefault()).trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        } else {
            null
        }
        return speciesRepository.getSpeciesByClassPaged(
            searchQuery = processedSearchQuery,
            classIdValue = classIdValue,
            languageCode = currentLanguageCode,
            //uid = uid?:"",
        )
    }

    suspend fun getById(idList: List<String>): List<DisplayableSpecies> {
        val currentLanguageCode = languageProvider.getCurrentLanguageCode()
        return speciesRepository.getSpeciesById(idList, currentLanguageCode, )
    }

    suspend fun getDetailsByDocId(speciesDocId: String): DisplayableSpecies? {
        val currentLanguageCode = languageProvider.getCurrentLanguageCode()
        return speciesRepository.getSpeciesDetails(
            speciesDocId = speciesDocId,
            languageCode = currentLanguageCode
        )
    }
}

