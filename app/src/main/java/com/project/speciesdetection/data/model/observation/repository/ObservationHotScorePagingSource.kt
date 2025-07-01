package com.project.speciesdetection.data.model.observation.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.project.speciesdetection.data.model.observation.Observation
import kotlinx.coroutines.tasks.await

/**
 * PagingSource để tải các Observation từ Firestore.
 * - Key: Query - Con trỏ đến trang tiếp theo. Đối với Firestore, đây là cách tốt nhất
 *           để đại diện cho trạng thái phân trang, vì nó bao gồm cả 'startAfter' và 'limit'.
 * - Value: Observation - Loại dữ liệu chúng ta đang tải.
 */
class ObservationHotScorePagingSource(
    // Query cơ sở, ví dụ: collection("observations").orderBy("hotScore")
    private val baseQuery: Query
) : PagingSource<Query, Observation>() {

    override suspend fun load(params: LoadParams<Query>): LoadResult<Query, Observation> {
        return try {
            // Lấy query cho trang hiện tại.
            // Nếu params.key là null, đây là lần tải đầu tiên -> sử dụng baseQuery.
            // Nếu không, sử dụng query đã được chuẩn bị từ lần tải trước (làm nextKey).
            val currentPageQuery = params.key ?: baseQuery.limit(params.loadSize.toLong())

            val snapshot = currentPageQuery.get().await()

            // Chuyển đổi dữ liệu từ snapshot thành danh sách các đối tượng Observation.
            val observations = snapshot.toObjects(Observation::class.java).mapIndexed { index, observation ->
                // Gán ID từ document vào đối tượng để đảm bảo nó không bị thiếu.
                observation.copy(id = snapshot.documents[index].id)
            }

            // Xác định query cho trang tiếp theo (nextKey).
            val lastVisibleDocument = snapshot.documents.lastOrNull()
            val nextQuery = if (lastVisibleDocument != null) {
                baseQuery.startAfter(lastVisibleDocument).limit(params.loadSize.toLong())
            } else {
                null // Không còn trang nào nữa.
            }

            LoadResult.Page(
                data = observations,
                prevKey = null, // Chỉ phân trang về phía trước.
                nextKey = nextQuery
            )

        } catch (e: Exception) {
            Log.e("ObservationPagingSource", "Lỗi khi tải dữ liệu: ", e)
            LoadResult.Error(e)
        }
    }

    /**
     * Hàm này xác định key nào sẽ được sử dụng để tải lại dữ liệu khi PagingSource bị vô hiệu hóa.
     * Trả về null sẽ khiến Paging 3 bắt đầu lại từ đầu, đó là hành vi mong muốn cho việc làm mới.
     */
    override fun getRefreshKey(state: PagingState<Query, Observation>): Query? {
        return null
    }
}