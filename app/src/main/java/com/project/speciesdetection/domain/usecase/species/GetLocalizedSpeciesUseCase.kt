package com.project.speciesdetection.domain.usecase.species

import androidx.paging.PagingData
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.domain.provider.ProviderModule
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesUseCase @Inject constructor(
    private val remoteSpeciesRepository: SpeciesRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
) {

    fun getByClassPaged(
        classIdValue: String
        // sortByName không còn là tham số trực tiếp ở đây vì Paging sắp xếp từ server
    ): Flow<PagingData<DisplayableSpecies>> {
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesRepository.getSpeciesByFieldPaged(
            targetField = "classId", // Trường trong Firestore để query
            languageCode = languageCode, // Dùng để map sang DisplayableSpecies
            value = classIdValue
        )
    }

    fun getByFieldPaged(
        targetField: String, // Trường trong Firestore để query
        value: String
    ): Flow<PagingData<DisplayableSpecies>> {
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesRepository.getSpeciesByFieldPaged(
            targetField = targetField,
            languageCode = languageCode,
            value = value
        )
    }

    fun getByClass(
        value: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>> = getByField(
        "classId",
        value,
        sortByName
    )

    fun getByField(
        targetField: String,
        value: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>> {
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesRepository.getSpeciesByField(
            targetField,
            languageCode,
            value,
            sortByName
        )
    }
}