package com.project.speciesdetection.data.model.species_class.repository

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.SpeciesClassDatabaseService
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesClassRepository @Inject constructor(
    @Named("species_class_db") private val databaseService: SpeciesClassDatabaseService<SpeciesClass, String>
) : SpeciesClassRepository {

    override fun getAllSpeciesClass(): Flow<DataResult<List<SpeciesClass>>> {
        return databaseService.getAllSpeciesClass()
    }

    override suspend fun getAll(): List<SpeciesClass> {
        return databaseService.getAll()
    }

}