package com.project.speciesdetection.domain.usecase.species

import android.util.Log
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import com.project.speciesdetection.data.model.species_class.repository.RemoteSpeciesClassRepository
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GetLocalizedSpeciesClassUseCase @Inject constructor(
    @Named("remote_species_class_repo") private val remoteSpeciesClassRepository: SpeciesClassRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
){
    fun getAll() : Flow<DataResult<List<DisplayableSpeciesClass>>> {
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesClassRepository.getAllSpeciesClass().map { result ->
            when (result) {
                is DataResult.Success -> {

                    DataResult.Success(result.data.map {
                        it.toDisplayable(languageCode)

                    }

                    )
                }
                is DataResult.Error -> DataResult.Error(result.exception)
                is DataResult.Loading -> DataResult.Loading
            }
        }

    }


}