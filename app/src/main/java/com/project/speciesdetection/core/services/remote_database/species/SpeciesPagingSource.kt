package com.project.speciesdetection.core.services.remote_database.species

import android.util.Log // Hoặc Timber nếu bạn dùng
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot // THAY ĐỔI QUAN TRỌNG
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
// import com.google.firebase.firestore.QuerySnapshot // Không cần trực tiếp làm kiểu Key nữa
import com.project.speciesdetection.data.model.species.Species // Đảm bảo import đúng model
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

// Hằng số cho class PagingSource
private const val PAGING_SOURCE_TAG = "SpeciesPagingSource"

/*class SpeciesPagingSource(
    private val baseQuery: Query,
    private val pageSize: Int,
    private val searchQuery: List<String>?,
    private val languageCode: String
) : PagingSource<DocumentSnapshot, Species>() {

    companion object {
        private const val TAG = "SpeciesPagingSource"
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Species> {
        try {
            val overFetchSize = pageSize * 3L

            Log.d(TAG, "Loading page, over-fetch size: $overFetchSize")

            // Truy vấn với DocumentSnapshot hoặc lấy trang đầu tiên nếu không có
            val currentPage = params.key?.let {
                baseQuery.startAfter(it).limit(overFetchSize).get().await()
            } ?: baseQuery.limit(overFetchSize).get().await()

            val totalFetched = currentPage.documents.size
            var filteredOutCount = 0
            val filteredSpecies = mutableListOf<Species>()

            for (doc in currentPage.documents) {
                val species = doc.toObject(Species::class.java)
                if (species != null) {
                    species.apply { id = doc.id }
                    if (!searchQuery.isNullOrEmpty()) {
                        val nameTokens = doc.get("name_tokens.$languageCode") as? List<String> ?: emptyList()
                        val sciTokens = doc.get("scientificNameToken") as? List<String> ?: emptyList()
                        val combined = nameTokens + sciTokens

                        if (searchQuery.all { combined.contains(it) }) {
                            filteredSpecies.add(species)
                        } else {
                            filteredOutCount++
                        }
                    } else {
                        filteredSpecies.add(species)
                    }

                    if (filteredSpecies.size == pageSize) break
                }
            }

            Log.d(TAG, "Fetched $totalFetched docs from Firestore")
            Log.d(TAG, "Filtered out $filteredOutCount docs not matching all tokens")
            Log.d(TAG, "Returning ${filteredSpecies.size} docs for this page")

            // Xác định nextPage
            val nextPage = if (currentPage.documents.size < overFetchSize) {
                null
            } else {
                baseQuery
                    .startAfter(currentPage.documents.last())
                    .limit(overFetchSize)
                    .get()
                    .await()
            }

            return LoadResult.Page(
                data = filteredSpecies,
                prevKey = null,
                nextKey = nextPage?.documents?.lastOrNull() // Trả về DocumentSnapshot của tài liệu cuối
            )

        } catch (e: Exception) {
            Log.e(TAG, "Load failed: ${e.message}", e)
            return LoadResult.Error(e)
        }
    }

    // Đoạn này trả về key là DocumentSnapshot
    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Species>): DocumentSnapshot? {
        // Trả về tài liệu gần nhất của state hoặc null nếu không có
        return null
    }
}*/


class SpeciesPagingSource(
    private val baseQuery: Query, // Query đã bao gồm whereEqualTo và orderBy
    private val pageSize: Int,
    private val searchQuery : List<String>?=null,
    private val lastQuery : String = "",
    private val languageCode : String
) : PagingSource<DocumentSnapshot, Species>() { // <<<< THAY ĐỔI Ở ĐÂY: QuerySnapshot -> DocumentSnapshot

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Species> { // <<<< THAY ĐỔI Ở ĐÂY

        return try {
            val currentPageQuery = params.key?.let { lastVisibleDoc ->
                baseQuery.startAfter(lastVisibleDoc).limit(pageSize.toLong())
            } ?: baseQuery.limit(pageSize.toLong())

            val querySnapshot = currentPageQuery.get().await()
            Log.d(PAGING_SOURCE_TAG, "Firestore query returned ${querySnapshot.size()} documents.")

            val documentsToProcess = querySnapshot.documents


            val speciesListFromSnapshot = documentsToProcess.mapNotNull { document ->
                try {
                    val species = document.toObject(Species::class.java)
                    species?.apply { id = document.id }
                    species
                } catch (e: Exception) {
                    Log.e(PAGING_SOURCE_TAG, "Error converting document ${document.id} to Species", e)
                    null
                }
            }

            // Thực hiện lọc SAU KHI đã convert sang Species object
            var finalSpeciesList = if (!searchQuery.isNullOrEmpty()) {
                speciesListFromSnapshot.filter { species ->
                    val nameTokens = species.nameTokens?.get(languageCode) ?: emptyList()
                    val sciTokens = species.scientificNameToken ?: emptyList()
                    val combined = nameTokens + sciTokens
                    searchQuery.all { combined.contains(it) }

                }
            } else {
                speciesListFromSnapshot
            }
            Log.d("a", lastQuery)

            finalSpeciesList = finalSpeciesList.filter {
                Log.d("a", it.name[languageCode]!!)
                Log.d("a", it.name[languageCode]!!.contains(lastQuery, true).toString())
                it.name[languageCode]!!.contains(lastQuery, true) || it.scientificName.contains(lastQuery, true)
            }
            Log.d(PAGING_SOURCE_TAG, "After client-side filtering, returning ${finalSpeciesList.size} species for this page.")


            // nextKey dựa trên kết quả *trước khi* lọc client-side để đảm bảo cursor của Firestore là đúng
            val lastFetchedDocument = querySnapshot.documents.lastOrNull()

            val nextKey = if (querySnapshot.documents.size < pageSize || lastFetchedDocument == null) {
                // Nếu Firestore trả về ít hơn pageSize, hoặc không trả về gì, thì không còn trang tiếp theo TỪ FIRESTORE
                null
            } else {
                lastFetchedDocument
            }
            // Lưu ý: Nếu sau khi lọc client-side, finalSpeciesList rỗng nhưng nextKey vẫn có,
            // Paging sẽ tiếp tục gọi load() với nextKey đó. Điều này có thể ổn nếu bạn chấp nhận
            // một số trang có thể trống sau khi lọc.

            LoadResult.Page(
                data = finalSpeciesList,
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            // Quan trọng: Nếu e là CancellationException, hãy rethrow nó
            if (e is CancellationException) {
                throw e
            }
            Log.e(PAGING_SOURCE_TAG, "Error loading species for Paging", e)
            LoadResult.Error(e) // Các lỗi khác sẽ được xử lý bởi Paging Library
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Species>): DocumentSnapshot? { // <<<< THAY ĐỔI Ở ĐÂY
        // Logic này thường trả về null để Paging 3 tải lại từ đầu khi refresh.
        // Nếu bạn muốn cố gắng tìm vị trí anchor, bạn có thể triển khai logic phức tạp hơn ở đây,
        // nhưng với startAfter, null thường là đủ và đơn giản nhất.
        return null
    }
}