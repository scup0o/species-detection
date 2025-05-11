package com.project.speciesdetection.core.services.remote_database.firestore.species

import android.util.Log // Hoặc Timber nếu bạn dùng
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot // THAY ĐỔI QUAN TRỌNG
import com.google.firebase.firestore.Query
// import com.google.firebase.firestore.QuerySnapshot // Không cần trực tiếp làm kiểu Key nữa
import com.project.speciesdetection.data.model.species.Species // Đảm bảo import đúng model
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

// Hằng số cho class PagingSource
private const val PAGING_SOURCE_TAG = "SpeciesPagingSource"

class SpeciesPagingSource(
    private val baseQuery: Query, // Query đã bao gồm whereEqualTo và orderBy
    private val pageSize: Int
) : PagingSource<DocumentSnapshot, Species>() { // <<<< THAY ĐỔI Ở ĐÂY: QuerySnapshot -> DocumentSnapshot

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Species> { // <<<< THAY ĐỔI Ở ĐÂY
        return try {
            //delay to test skeleton loading
            val delayMillis = if (params.key == null) {
                2000L // Độ trễ 2 giây cho lần tải đầu tiên (initial load)
            } else {
                1000L // Độ trễ 1 giây cho các lần tải thêm (append/prepend)
            }
            delay(delayMillis) // <<<<< THÊM DELAY Ở ĐÂY


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

            val speciesList = querySnapshot.documents.mapNotNull { document ->
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