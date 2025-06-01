package com.project.speciesdetection.data.model.species_class.repository

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import kotlinx.coroutines.flow.Flow

interface SpeciesClassRepository {
    fun getAllSpeciesClass() : Flow<DataResult<List<SpeciesClass>>>

    suspend fun getAll() : List<SpeciesClass>
}