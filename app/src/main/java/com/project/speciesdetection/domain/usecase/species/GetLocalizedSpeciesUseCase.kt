package com.project.speciesdetection.domain.usecase.species

import androidx.compose.ui.text.toLowerCase
import androidx.paging.PagingData
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.domain.provider.ProviderModule
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesUseCase @Inject constructor(
    private val remoteSpeciesRepository: SpeciesRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
) {
    fun getByClassPaged(
        searchQuery : String,
        classIdValue: String,
        // sortByName không còn là tham số trực tiếp ở đây vì Paging sắp xếp từ server
    ): Flow<PagingData<DisplayableSpecies>> {
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesRepository.getSpeciesByFieldPaged(
            searchQuery =
                if (searchQuery!="") searchQuery.lowercase(Locale.getDefault())
                                                .trim()
                                                .split("\\s+".toRegex())
                else null,
            targetField = "classId", // Trường trong Firestore để query
            languageCode = languageCode, // Dùng để map sang DisplayableSpecies
            value = classIdValue
        )
    }
}