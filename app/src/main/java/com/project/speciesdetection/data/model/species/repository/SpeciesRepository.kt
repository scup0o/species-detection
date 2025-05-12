package com.project.speciesdetection.data.model.species.repository

import androidx.paging.PagingData
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow

interface SpeciesRepository {
    /*fun getSpeciesByField(
        targetField: String,
        languageCode: String,
        value: String,
        sortByName: Boolean)
    : Flow<DataResult<List<DisplayableSpecies>>>*/

    fun getSpeciesByFieldPaged(
        searchQuery : List<String>?,
        targetField: String,
        languageCode: String,
        value: String,
        // orderBy và sortDirection sẽ được xử lý bởi FirestoreSpeciesService
        // sortByName (client-side sorting) không áp dụng trực tiếp với PagingData
        // vì PagingData đã được sắp xếp từ server. Nếu cần sắp xếp lại
        // sau khi map, sẽ phức tạp hơn và thường không khuyến khích.
    ): Flow<PagingData<DisplayableSpecies>>
}