package com.project.speciesdetection.domain.usecase.species

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.domain.provider.ProviderModule
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

class GetLocalizedSpeciesUseCase @Inject constructor(
    private val speciesRepository: SpeciesRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
) {
    fun getByClass(
        targetClassName: String,
        languageCodeOfTargetClass: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>> {
        val displayLangCode = languageProvider.getCurrentLanguageCode()
        return speciesRepository.getSpeciesByClass(
            targetClassName,
            languageCodeOfTargetClass,
            displayLangCode,
            sortByName
        )
    }
}