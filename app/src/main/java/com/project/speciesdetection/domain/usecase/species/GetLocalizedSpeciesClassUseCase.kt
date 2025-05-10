package com.project.speciesdetection.domain.usecase.species

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import com.project.speciesdetection.data.model.species_class.repository.RemoteSpeciesClassRepository
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesClassUseCase @Inject constructor(
    @Named("remote_species_class_repo") private val remoteSpeciesClassRepository: SpeciesClassRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
){
    fun getAll() : Flow<DataResult<List<DisplayableSpeciesClass>>> {
        return remoteSpeciesClassRepository.getAllSpeciesClass(languageProvider.getCurrentLanguageCode())
    }
}