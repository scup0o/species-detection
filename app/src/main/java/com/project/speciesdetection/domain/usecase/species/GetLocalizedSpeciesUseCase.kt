package com.project.speciesdetection.domain.usecase.species

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.domain.provider.ProviderModule
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesUseCase @Inject constructor(
    private val remoteSpeciesRepository: SpeciesRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
) {

    fun getByClass(
        value: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>>{
        return getByField(
            "classId",
            value,
            sortByName
        )
    }

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