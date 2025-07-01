package com.project.speciesdetection.data.model.notification.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.project.speciesdetection.data.model.notification.Notification
import kotlinx.coroutines.tasks.await

class NotificationPagingSource(
    private val baseQuery: Query
) : PagingSource<DocumentSnapshot, Notification>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Notification> {
        return try {
            val currentQuery = if (params.key != null) {
                baseQuery.startAfter(params.key!!).limit(params.loadSize.toLong())
            } else {
                baseQuery.limit(params.loadSize.toLong())
            }

            val snapshot = currentQuery.get().await()
            Log.d("NotificationSnapshot", "Snapshot: ${snapshot.documents}")

            val notifications = snapshot.documents.mapIndexed { index, document ->
                val isRead = document.getBoolean("isRead") ?: false  // Lấy giá trị isRead từ Firestore, mặc định là false nếu không có
                val notification = document.toObject(Notification::class.java) ?: Notification()
                notification.copy(
                    id = document.id,
                    isRead = isRead  // Gán giá trị isRead thủ công
                )
            }

            val lastVisibleDocument = snapshot.documents.lastOrNull()
            Log.i("noti",notifications.toString())
            LoadResult.Page(
                data = notifications,
                prevKey = null, // không hỗ trợ scroll lên
                nextKey = lastVisibleDocument
            )


        } catch (e: Exception) {
            Log.e("NotificationPagingSource", "Lỗi khi tải dữ liệu: ", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Notification>): DocumentSnapshot? {
        return null // Có thể cải tiến thêm sau
    }
}
