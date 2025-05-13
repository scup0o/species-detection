package com.project.speciesdetection.data.model.species.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.firestore.Query
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.SpeciesDatabaseService
import com.project.speciesdetection.core.services.remote_database.species.DEFAULT_SPECIES_PAGE_SIZE
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesRepository @Inject constructor(
    @Named("species_db") private val databaseService: SpeciesDatabaseService<Species, String>,
):SpeciesRepository{
    override fun getAll(
        searchQuery: List<String>?,
        languageCode: String
    ): Flow<PagingData<DisplayableSpecies>> {
        val orderByFieldForQuery: String? = "name.$languageCode"
        return databaseService.getAll(
            languageCode = languageCode,
            searchQuery = searchQuery,
            sortDirection = Query.Direction.ASCENDING,
            orderByField = orderByFieldForQuery,
            pageSize = DEFAULT_SPECIES_PAGE_SIZE
        ).map { pagingDataSpecies ->
            pagingDataSpecies.map { species ->
                val updatedSpecies = species.copy(
                    imageURL = species.imageURL?.let { CloudinaryImageURLHelper.getSquareImageURL(it) }
                )
                updatedSpecies.toDisplayable(languageCode)
            }
        }
    }

    override fun getSpeciesByFieldPaged(
        searchQuery : List<String>?,
        targetField: String,
        languageCode: String,
        value: String
    ): Flow<PagingData<DisplayableSpecies>> {
        // Xác định fieldPath và orderByField cho Firestore query
        // QUAN TRỌNG: Firestore orderBy phải dựa trên các trường thực tế trong document.
        // Nếu `targetField` là một map (ví dụ: `commonName: {en: "Lion", vi: "Sư tử"}`),
        // bạn không thể dễ dàng `orderBy` trên `commonName.en` nếu `whereEqualTo` là trên `classId`.
        // Bạn cần một trường riêng cho việc sắp xếp hoặc chấp nhận sắp xếp theo trường mặc định (ví dụ: id document).
        val fieldPathForQuery: String
        val orderByFieldForQuery: String? = "name.$languageCode"

        if (targetField == "classId") {
            fieldPathForQuery = targetField
            //orderByFieldForQuery = "" // HOẶC null để FirestoreSpeciesService dùng default
        } else {
            fieldPathForQuery = "$targetField.$languageCode"
        }

        return databaseService.getByFieldValuePaged(
            languageCode = languageCode,
            searchQuery = searchQuery,
            fieldPath = fieldPathForQuery,
            value = value,
            pageSize = DEFAULT_SPECIES_PAGE_SIZE, // Sử dụng hằng số từ service
            orderByField = orderByFieldForQuery, // ví dụ: "scientificName" hoặc fieldPathForQuery
            sortDirection = Query.Direction.ASCENDING // Hoặc DESCENDING tùy nhu cầu
        ).map { pagingDataSpecies: PagingData<Species> ->
            pagingDataSpecies.map { species ->

                // Tạo bản sao để tránh thay đổi species gốc
                val updatedSpecies = species.copy(
                    imageURL = species.imageURL?.let { CloudinaryImageURLHelper.getSquareImageURL(it) }
                )
                updatedSpecies.toDisplayable(languageCode)
            }
        }
    }

    /*override fun getSpeciesByField(
        targetField: String,
        languageCode: String,
        value: String,
        sortByName: Boolean
    ): Flow<DataResult<List<DisplayableSpecies>>> {
        val fieldPathForQuery = if (targetField=="classId") targetField
                                else "$targetField.$languageCode"
        val options = mutableMapOf<String, Any>()
        return databaseService.getByFieldValue(fieldPathForQuery, value, options).map { result ->
            when (result) {
                is DataResult.Success -> {
                    var displayableList = result.data.map {
                        it.imageURL = CloudinaryImageURLHelper.getSquareImageURL(it.imageURL!!)
                        it.toDisplayable(languageCode)
                    }
                    if (sortByName) {
                        displayableList = displayableList.sortedBy { it.localizedName }
                    }
                    DataResult.Success(displayableList)
                }
                is DataResult.Error -> DataResult.Error(result.exception)
                is DataResult.Loading -> DataResult.Loading
            }
        }
    }*/
}