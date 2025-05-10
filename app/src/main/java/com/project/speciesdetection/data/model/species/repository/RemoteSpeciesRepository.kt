package com.project.speciesdetection.data.model.species.repository

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.DatabaseService
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesRepository @Inject constructor(
    @Named("firestore_species_db") private val databaseService: DatabaseService<Species, String>
):SpeciesRepository{

    override fun getSpeciesByClass(
        targetClassName: String,
        languageCode: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>> {
        val fieldPathForQuery = "class.$languageCode"
        val options = mutableMapOf<String, Any>()
        return databaseService.getByFieldValue(fieldPathForQuery, targetClassName, options).map { result ->
            when (result) {
                is DataResult.Success -> {
                    var displayableList = result.data.map { it.toDisplayable(languageCode) }
                    if (sortByName) {
                        displayableList = displayableList.sortedBy { it.localizedName }
                    }
                    DataResult.Success(displayableList)
                }
                is DataResult.Error -> DataResult.Error(result.exception)
                is DataResult.Loading -> DataResult.Loading
            }
        }
    }
}