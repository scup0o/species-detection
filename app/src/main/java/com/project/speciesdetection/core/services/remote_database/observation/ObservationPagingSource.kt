package com.project.speciesdetection.core.services.remote_database.observation

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.tasks.await

private const val PAGE_SIZE = 20

class ObservationPagingSource(
    private val firestore: FirebaseFirestore,
    private val uid: String?,
    private val speciesId: String?,
    private val queryByDesc : Boolean? = true
) : PagingSource<QuerySnapshot, Observation>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Observation> {
        return try {
            // Xây dựng câu truy vấn cơ bản
            val sortBy = if (queryByDesc!!) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            Log.i("l", sortBy.toString())

            var query: Query = firestore.collection("observations").whereEqualTo("state", "normal")

            // Thêm điều kiện lọc nếu uid được cung cấp
            Log.i("now1","$uid, $speciesId")
            if (uid!=null && uid!="") {
                query = query.whereEqualTo("uid", uid)
            }
            else{
                query = query.whereEqualTo("privacy", "Public")
                Log.i("now1", "privacy")
            }

            // Thêm điều kiện lọc nếu speciesId được cung cấp
            if (!speciesId.isNullOrEmpty()) {
                query = query.whereEqualTo("speciesId", speciesId)
            }

            query = query.orderBy("dateCreated", sortBy)




            // Dùng cursor (key) để lấy trang tiếp theo
            val currentPage = params.key ?: query.limit(PAGE_SIZE.toLong()).get().await()

            // Lấy trang tiếp theo dựa trên document cuối cùng của trang hiện tại
            val lastVisibleDocument = currentPage.documents.lastOrNull()
            val nextPage = if (lastVisibleDocument != null) {
                query.limit(PAGE_SIZE.toLong()).startAfter(lastVisibleDocument).get().await()
            } else {
                null
            }


            // Chuyển đổi documents thành danh sách các đối tượng Observation
            val observations = currentPage.documents.mapNotNull { doc ->
                try {
                    val observation = doc.toObject(Observation::class.java)
                    observation?.apply { id = doc.id }
                } catch (e: Exception) {
                    null
                }
            }
            Log.i("now1","$observations")
            LoadResult.Page(
                data = observations,
                prevKey = null, // Chỉ phân trang về phía trước
                nextKey = nextPage
            )
        } catch (e: Exception) {
            // Xử lý lỗi
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Observation>): QuerySnapshot? {
        // Cố gắng tìm key gần nhất với vị trí neo (anchorPosition) khi refresh
        // Trả về null sẽ khiến PagingSource tải lại từ đầu
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
                ?: state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}