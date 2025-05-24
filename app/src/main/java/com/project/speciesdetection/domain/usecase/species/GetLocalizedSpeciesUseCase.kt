package com.project.speciesdetection.domain.usecase.species

import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.paging.PagingData
import androidx.paging.map
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.services.remote_database.DataResult
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
    private val remoteSpeciesRepository: SpeciesRepository,
    @Named("remote_species_class_repo") private val remoteSpeciesClassRepository: SpeciesClassRepository,
    @Named("language_provider") private val languageProvider: LanguageProvider
) {
    fun getAll(searchQuery: String): Flow<PagingData<DisplayableSpecies>>{
        val languageCode = languageProvider.getCurrentLanguageCode()
        return remoteSpeciesRepository.getAll(
            searchQuery =
                if (searchQuery!="") searchQuery
                    .lowercase(Locale.getDefault())
                    .trim()
                    .split("\\s-+".toRegex())
                else null,
            languageCode = languageCode
        ).map { pagingDataSpecies ->
            pagingDataSpecies.map { species ->
                val updatedSpecies = species.copy(
                    imageURL = species.imageURL?.let { CloudinaryImageURLHelper.getSquareImageURL(it) }
                )
                updatedSpecies.toDisplayable(languageCode)
            }
        }
    }

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
                                                .split("\\s-+".toRegex())
                else null,
            targetField = "classId", // Trường trong Firestore để query
            languageCode = languageCode, // Dùng để map sang DisplayableSpecies
            value = classIdValue
        ).map { pagingDataSpecies ->
            pagingDataSpecies.map { species ->
                val updatedSpecies = species.copy(
                    imageURL = species.imageURL?.let { CloudinaryImageURLHelper.getSquareImageURL(it) }
                )

                //Log.i("a", updatedSpecies.copy().toString())
                updatedSpecies.toDisplayable(languageCode)
            }
        }
    }

    suspend fun getById(idList : List<String>) : List<DisplayableSpecies>{
        val languageCode = languageProvider.getCurrentLanguageCode()
        val classResult = remoteSpeciesClassRepository.getAll().map { it.toDisplayable(languageCode) }

        val classMap: Map<String, String> =
            classResult.associate {it.id to it.localizedName }

        return remoteSpeciesRepository.getSpeciesById(idList).map { species ->
            val updatedSpecies = species.toDisplayable(languageCode).copy(
                imageURL = species.imageURL?.let { CloudinaryImageURLHelper.getSquareImageURL(it) }
            )
            val scientificList =
                updatedSpecies.scientific + mapOf(
                    "class" to updatedSpecies.localizedClass.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    })
            val className = classMap[updatedSpecies.localizedClass] ?: ""
            Log.i("a",updatedSpecies.copy().toString())
            updatedSpecies.copy(
                localizedClass = className,
                scientific = scientificList)
        }
    }

}