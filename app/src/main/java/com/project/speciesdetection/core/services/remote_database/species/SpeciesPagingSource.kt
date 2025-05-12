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
    private val languageCode : String
) : PagingSource<DocumentSnapshot, Species>() { // <<<< THAY ĐỔI Ở ĐÂY: QuerySnapshot -> DocumentSnapshot

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Species> { // <<<< THAY ĐỔI Ở ĐÂY

        return try {

            /*
            //delay to test skeleton loading
            val delayMillis = if (params.key == null) {
                2000L // Độ trễ 2 giây cho lần tải đầu tiên (initial load)
            } else {
                1000L // Độ trễ 1 giây cho các lần tải thêm (append/prepend)
            }
            delay(delayMillis) // <<<<< THÊM DELAY Ở ĐÂY*/

            // params.key giờ là DocumentSnapshot? (document cuối cùng của trang trước)
            val currentPageQuery = params.key?.let { lastVisibleDoc ->
                Log.d(PAGING_SOURCE_TAG, "Loading next page after document: ${lastVisibleDoc.id}")
                baseQuery.startAfter(lastVisibleDoc).limit(pageSize.toLong())
            } ?: run {
                Log.d(PAGING_SOURCE_TAG, "Loading initial page")
                baseQuery.limit(pageSize.toLong())
            }

            Log.d(PAGING_SOURCE_TAG, "Executing query for Paging")
            val querySnapshot = currentPageQuery.get().await() // querySnapshot vẫn là QuerySnapshot
            Log.d(PAGING_SOURCE_TAG, "Query returned ${querySnapshot.size()} documents.")

            var filteredSpecies = mutableListOf<DocumentSnapshot>()
            if(!searchQuery.isNullOrEmpty()){
                querySnapshot.documents.forEach{ document ->
                    val nameTokens = document.get("name_tokens.$languageCode") as? List<String> ?: emptyList()
                    val sciTokens = document.get("scientificNameToken") as? List<String> ?: emptyList()
                    val combined = nameTokens + sciTokens
                    if (searchQuery.all { combined.contains(it) }) {
                        filteredSpecies.add(document)
                    }
                    Log.d(PAGING_SOURCE_TAG, "Returning ${filteredSpecies.size} docs for this page")
                }
            }
            else{
                filteredSpecies = querySnapshot.documents
            }




            val speciesList = filteredSpecies.mapNotNull { document ->
                    try {
                        val species = document.toObject(Species::class.java)
                        species?.apply { id = document.id }
                        species
                    } catch (e: Exception) {
                        Log.e(PAGING_SOURCE_TAG, "Error converting document ${document.id} to Species", e)
                        null
                    }

            }
            // Lấy DocumentSnapshot của item cuối cùng trong danh sách hiện tại để làm nextKey
            val lastVisibleDocument: DocumentSnapshot? = if (querySnapshot.documents.isNotEmpty()) {
                querySnapshot.documents.last() // DocumentSnapshot cuối cùng từ kết quả query
            } else {
                null
            }
            Log.d(PAGING_SOURCE_TAG, "Loaded ${speciesList.size} species. Last visible doc ID: ${lastVisibleDocument?.id}")

            // Nếu querySnapshot rỗng (không có document nào) hoặc số lượng speciesList ít hơn pageSize,
            // hoặc lastVisibleDocument là null, thì không còn trang tiếp theo.
            val nextKey: DocumentSnapshot? = if (querySnapshot.isEmpty || speciesList.size < pageSize || lastVisibleDocument == null) {
                null
            } else {
                lastVisibleDocument
            }

            LoadResult.Page(
                data = speciesList,
                prevKey = null, // Chỉ hỗ trợ cuộn xuống
                nextKey = nextKey // <<<< nextKey giờ là DocumentSnapshot?
            )
        } catch (e: Exception) {
            Log.e(PAGING_SOURCE_TAG, "Error loading species for Paging", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Species>): DocumentSnapshot? { // <<<< THAY ĐỔI Ở ĐÂY
        // Logic này thường trả về null để Paging 3 tải lại từ đầu khi refresh.
        // Nếu bạn muốn cố gắng tìm vị trí anchor, bạn có thể triển khai logic phức tạp hơn ở đây,
        // nhưng với startAfter, null thường là đủ và đơn giản nhất.
        return null
    }
}