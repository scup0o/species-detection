package com.project.speciesdetection.data.model.species.repository

import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.firestore.Query
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.DatabaseService
import com.project.speciesdetection.core.services.remote_database.firestore.species.DEFAULT_SPECIES_PAGE_SIZE
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesRepository @Inject constructor(
    @Named("firestore_species_db") private val databaseService: DatabaseService<Species, String>,
):SpeciesRepository{

    override fun getSpeciesByFieldPaged(
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
        // Ví dụ đơn giản:
        if (targetField == "classId") {
            fieldPathForQuery = targetField
            // Mặc định sắp xếp theo một trường chung nào đó, ví dụ `scientificName` hoặc `id`
            // Hoặc để FirestoreSpeciesService tự quyết định (sắp xếp theo ID nếu null)
            //orderByFieldForQuery = "" // HOẶC null để FirestoreSpeciesService dùng default
        } else {
            // Đây là trường hợp phức tạp hơn nếu targetField là một map.
            // Giả sử targetField là tên của một trường map, ví dụ "commonName"
            // và bạn muốn lọc dựa trên giá trị bên trong map đó (ví dụ commonName.en == "Lion")
            // Firestore query sẽ là: .whereEqualTo("commonName.$languageCode", value)
            fieldPathForQuery = "$targetField.$languageCode"
            // Việc orderBy trên một sub-field của map (`commonName.$languageCode`) khi có `whereEqualTo`
            // trên cùng sub-field đó thường được hỗ trợ và cần index.
            //orderByFieldForQuery = fieldPathForQuery
        }


        return databaseService.getByFieldValuePaged(
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

    override fun getSpeciesByField(
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
    }
}