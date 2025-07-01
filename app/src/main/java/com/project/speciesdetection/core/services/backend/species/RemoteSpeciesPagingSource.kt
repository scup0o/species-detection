package com.project.speciesdetection.core.services.backend.species

import android.util.Log // Sử dụng Log của Android
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import java.io.IOException     // Cho các lỗi mạng cụ thể
import retrofit2.HttpException // Cho các lỗi HTTP cụ thể (4xx, 5xx)

class RemoteSpeciesPagingSource(
    private val apiService: SpeciesApiService,
    private val languageProvider: LanguageProvider,
    private val searchQuery: String?,
    private val classId: String?,
    private val uid : String?,
    private val sortByDesc : Boolean = false,
) : PagingSource<String, DisplayableSpecies>() { // Key là String (lastVisibleDocId), Value là DisplayableSpecies

    companion object {
        private const val TAG = "RemoteSpeciesPagingSrc" // Tag cho Log
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, DisplayableSpecies> {
        // `params.key` là lastVisibleDocId của trang trước, null cho lần load đầu tiên.
        val lastDocId = params.key
        val currentLanguageCode = languageProvider.getCurrentLanguageCode()
        // `params.loadSize` là số lượng item mà Paging 3 muốn load cho trang này.
        val pageSizeToLoad = params.loadSize

        return try {
            Log.d(TAG, "Loading page: lastDocId=$lastDocId, pageSizeToLoad=$pageSizeToLoad, lang=$currentLanguageCode, query='$searchQuery', classId=$classId")
            Log.i("sort",sortByDesc.toString())

            // Gọi API
            val response = apiService.getSpecies(
                sortByDesc = sortByDesc,
                pageSize = pageSizeToLoad,
                languageCode = currentLanguageCode,
                searchQuery = if (searchQuery.isNullOrBlank()) null else searchQuery,
                classId = if (classId == "0" || classId.isNullOrBlank()) null else classId, // "0" hoặc rỗng nghĩa là lấy tất cả
                lastVisibleDocId = lastDocId,
                uid = uid?:"",

            )

            // Kiểm tra response từ API
            if (!response.success) {
                Log.e(TAG, "API call failed: ${response.message}")
                // Bạn có thể parse lỗi cụ thể hơn từ response.message nếu API cung cấp
                return LoadResult.Error(Exception("API Error from server: ${response.message}"))
            }

            val speciesList = response.data
            // `nextKey` là `lastVisibleDocId` của trang hiện tại, sẽ được dùng để load trang tiếp theo.
            // Nếu không có trang tiếp theo (hasNextPage = false), nextKey là null.
            val nextKey = if (response.pagination.hasNextPage) response.pagination.lastVisibleDocId else null

            Log.d(TAG, "Load successful: Loaded ${speciesList.size} items. NextKey for next page: $nextKey. API reported total items in page: ${response.pagination.totalItems}, hasNextPage: ${response.pagination.hasNextPage}")

            LoadResult.Page(
                data = speciesList,
                prevKey = null, // Không dùng prevKey với cursor-based pagination trong Paging 3
                nextKey = nextKey
            )
        } catch (e: IOException) {
            // Lỗi mạng cụ thể (ví dụ: không có internet, không thể kết nối tới host)
            Log.e(TAG, "Network error (IOException) loading species from API: ${e.message}", e)
            LoadResult.Error(e)
        } catch (e: HttpException) {
            // Lỗi HTTP cụ thể từ server (ví dụ: 401 Unauthorized, 404 Not Found, 500 Internal Server Error)
            Log.e(TAG, "HTTP error loading species from API: ${e.code()} - ${e.message()}", e)
            LoadResult.Error(e)
        } catch (e: Exception) {
            // Bắt các lỗi khác không lường trước được
            Log.e(TAG, "Generic error loading species from API", e)
            LoadResult.Error(e)
        }
    }

    // getRefreshKey được gọi khi Paging 3 cần refresh dữ liệu (ví dụ: khi invalidate PagingSource).
    // Với cursor-based pagination, việc trả về null thường có nghĩa là Paging 3 sẽ bắt đầu lại từ đầu
    // (gọi `load` với `params.key = null`).
    override fun getRefreshKey(state: PagingState<String, DisplayableSpecies>): String? {
        // Một cách tiếp cận phổ biến là không cố gắng tìm key gần nhất mà load lại từ đầu.
        // Paging 3 sẽ tự quản lý việc hiển thị dữ liệu đã có trong khi refresh.
        return null
    }
}