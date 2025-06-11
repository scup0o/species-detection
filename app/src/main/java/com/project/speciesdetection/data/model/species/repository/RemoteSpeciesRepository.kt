package com.project.speciesdetection.data.model.species.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.google.firebase.firestore.Query
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.services.backend.species.RemoteSpeciesPagingSource
import com.project.speciesdetection.core.services.backend.species.SpeciesApiService
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.SpeciesDatabaseService
import com.project.speciesdetection.core.services.remote_database.species.DEFAULT_SPECIES_PAGE_SIZE
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.Species
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class RemoteSpeciesRepository @Inject constructor(
    private val apiService: SpeciesApiService,
    @Named("language_provider") private val languageProvider: LanguageProvider // Inject LanguageProvider để PagingSource có thể lấy ngôn ngữ hiện tại
):SpeciesRepository{
    companion object {
        private const val TAG = "RemoteSpeciesRepoImpl" // Tag cho Log
        const val DEFAULT_PAGE_SIZE = 10 // Số item PagingConfig sẽ yêu cầu PagingSource load ban đầu và các lần sau
        const val PREFETCH_DISTANCE = 3   // Số lượng trang Paging 3 sẽ cố gắng load trước khi người dùng cuộn tới
        // Ví dụ: nếu pageSize=10, prefetchDistance=3, Paging 3 có thể load trước 30 items.
    }

    override fun getAll(
        uid : String?,
        searchQuery: List<String>?,
        languageCode: String // languageCode này có thể không cần thiết nếu PagingSource tự lấy từ LanguageProvider
    ): Flow<PagingData<DisplayableSpecies>> {
        val queryStr = searchQuery?.joinToString(" ")?.trim() // Chuyển List<String> thành một chuỗi query
        Log.d(TAG, "getAll called. Query: '$queryStr', Effective Lang (from PagingSource): ${languageProvider.getCurrentLanguageCode()}")
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false, // Thường là false cho PagingSource chỉ lấy từ mạng
                prefetchDistance = PREFETCH_DISTANCE
                // initialLoadSize = DEFAULT_PAGE_SIZE * 2 // Tùy chọn: số item load lần đầu, mặc định là pageSize * 3
            ),
            pagingSourceFactory = {
                RemoteSpeciesPagingSource(
                    apiService = apiService,
                    languageProvider = languageProvider, // Truyền LanguageProvider cho PagingSource
                    searchQuery = queryStr,
                    classId = null, // Đối với getAll, classId là null (không lọc theo class)
                    uid = uid?:"",
                )
            }
        ).flow // Trả về Flow của PagingData
    }

    override fun getSpeciesByClassPaged(
        uid: String?,
        searchQuery: List<String>?,
        classIdValue: String,
        languageCode: String // languageCode này có thể không cần thiết nếu PagingSource tự lấy
    ): Flow<PagingData<DisplayableSpecies>> {
        val queryStr = searchQuery?.joinToString(" ")?.trim()
        Log.d(TAG, "getSpeciesByClassPaged called. Query: '$queryStr', ClassId: '$classIdValue', Effective Lang (from PagingSource): ${languageProvider.getCurrentLanguageCode()}")
        return Pager(
            config = PagingConfig(
                pageSize = DEFAULT_PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE
            ),
            pagingSourceFactory = {
                RemoteSpeciesPagingSource(
                    uid = uid?:"",
                    apiService = apiService,
                    languageProvider = languageProvider,
                    searchQuery = queryStr,
                    classId = if (classIdValue == "0") null else classIdValue // Nếu classIdValue là "0" (Tất cả), coi như không lọc (null)
                )
            }
        ).flow
    }

    override suspend fun getSpeciesById(
        uid: String?,
        idList: List<String>,
        languageCode: String,
    ): List<DisplayableSpecies> {
        if (idList.isEmpty()) {
            Log.d(TAG, "getSpeciesById called with empty idList.")
            return emptyList()
        }
        Log.d(TAG, "getSpeciesById called with ids: ${idList.joinToString(",")}, lang: $languageCode")
        return try {
            val response = apiService.getSpeciesByIds(
                ids = idList.joinToString(","), // API nhận chuỗi ID cách nhau bởi dấu phẩy
                languageCode = languageCode,
                uid = uid?:"",
            )
            if (response.success) {
                Log.d(TAG, "getSpeciesById successful, fetched ${response.data.size} items.")
                response.data // API trả về ApiPagedResponse, lấy data từ đó
            } else {
                Log.e(TAG, "API Error in getSpeciesById: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getSpeciesById", e)
            emptyList()
        }
    }

    override suspend fun getSpeciesDetails(
        speciesDocId: String,
        languageCode: String
    ): DisplayableSpecies? {
        Log.d(TAG, "getSpeciesDetails called for ID: $speciesDocId, lang: $languageCode")
        return try {
            val response = apiService.getSingleSpeciesById(
                speciesDocId = speciesDocId,
                languageCode = languageCode
            )
            if (response.success) {
                Log.d(TAG, "Successfully fetched details for species ID: $speciesDocId. Name: ${response.data.localizedName}")
                response.data // API trả về ApiSingleResponse<DisplayableSpecies>, lấy data từ đó
            } else {
                Log.e(TAG, "API Error fetching details for species ID $speciesDocId: ${response.message}")
                null // Trả về null nếu API báo lỗi không thành công
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching details for species ID $speciesDocId", e)
            null // Trả về null nếu có exception
        }
    }
}