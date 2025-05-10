package com.project.speciesdetection.data.model.species.repository

import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow

interface SpeciesRepository {
    fun getSpeciesByClass(
        targetClassName: String,
        languageCodeOfTargetClass: String,
        displayLanguageCode: String,
        sortByName: Boolean)
    : Flow<DataResult<List<DisplayableSpecies>>>
}