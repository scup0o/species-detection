package com.project.speciesdetection.core.services.remote_database.observation

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.project.speciesdetection.data.model.observation.Observation
import kotlinx.coroutines.tasks.await
import java.util.Date

private const val PAGE_SIZE = 20

class ObservationPagingSource(
    private val firestore: FirebaseFirestore,
    private val uid: String?,
    private val speciesId: String?,
    private val queryByDesc: Boolean? = true,
    private val userRequested: String = "",
    private val state: String = "normal"
) : PagingSource<QuerySnapshot, Observation>() {

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Observation> {
        return try {
            val sortBy = if (queryByDesc == true) Query.Direction.DESCENDING else Query.Direction.ASCENDING

            // =========================================================================
            // === XỬ LÝ RIÊNG BIỆT CHO TAB "SAVE" ===
            // =========================================================================
            if (state == "save") {
                if (uid.isNullOrEmpty()) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                // BƯỚC 1: Truy vấn Collection Group trên "saves"
                var savesQuery: Query = firestore.collectionGroup("saves")
                    .whereEqualTo("userId", uid)
                    .orderBy("savedAt", sortBy)

                // BƯỚC 2: Lấy trang hiện tại của các document "save"
                val currentPageSaves = params.key ?: savesQuery.limit(PAGE_SIZE.toLong()).get().await()

                if (currentPageSaves.isEmpty) {
                    return LoadResult.Page(emptyList(), null, null)
                }

                // BƯỚC 3: Lấy ID của các observation cha
                val observationIds = currentPageSaves.documents.map { it.reference.parent.parent!!.id }

                if (observationIds.isEmpty()){
                    return LoadResult.Page(emptyList(), null, null)
                }

                // BƯỚC 4: Lấy dữ liệu thô của các observation đó
                val observationsSnapshot = firestore.collection("observations")
                    .whereIn(FieldPath.documentId(), observationIds)
                    .get()
                    .await()

                val observationsMap = observationsSnapshot.documents.associateBy(
                    { it.id },
                    { it.toObject(Observation::class.java) }
                )

                // BƯỚC 5: Tạo danh sách ban đầu và LỌC theo quyền riêng tư
                val initialObservations = observationIds.mapNotNull { id -> observationsMap[id] }

                val finalObservations = initialObservations.filter { observation ->
                    // Giữ lại observation nếu nó là "Public" HOẶC nó là của chính người dùng này
                    observation.privacy == "Public" || observation.uid == uid
                }
                Log.d("PagingSource[Save]", "Initial: ${initialObservations.size}, Filtered: ${finalObservations.size}")

                // BƯỚC 6: Xác định key cho trang tiếp theo một cách hiệu quả
                val nextKey = if (currentPageSaves.size() >= PAGE_SIZE) {
                    currentPageSaves // Nếu trang hiện tại đầy, có khả năng còn trang sau
                } else {
                    null // Nếu không đầy, chắc chắn đã hết
                }

                return LoadResult.Page(
                    data = finalObservations,
                    prevKey = null,
                    nextKey = nextKey
                )
            }

            // =========================================================================
            // === LOGIC CHO CÁC TAB KHÁC (normal, deleted) --- (Giữ nguyên, đã được tối ưu) ===
            // =========================================================================
            var query: Query = firestore.collection("observations")

            // Áp dụng các bộ lọc
            query = query.whereEqualTo("state", state)

            if (!uid.isNullOrEmpty()) {
                query = query.whereEqualTo("uid", uid)
                if (userRequested.isNotEmpty() && userRequested != uid) {
                    query = query.whereEqualTo("privacy", "Public")
                }
            } else {
                query = query.whereEqualTo("privacy", "Public")
            }

            if (!speciesId.isNullOrEmpty()) {
                query = query.whereEqualTo("speciesId", speciesId)
            }

            if (state == "deleted") {
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                val timestampThirtyDaysAgo = com.google.firebase.Timestamp(Date(thirtyDaysAgo))
                query = query.whereGreaterThanOrEqualTo("dateUpdated", timestampThirtyDaysAgo)
            }

            // Áp dụng sắp xếp
            if (state == "deleted") {
                query = query.orderBy("dateUpdated", sortBy)
            } else {
                query = query.orderBy("dateCreated", sortBy)
            }

            // Phân trang
            val currentPage = params.key ?: query.limit(PAGE_SIZE.toLong()).get().await()

            val observations = currentPage.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Observation::class.java)
                } catch (e: Exception) {
                    Log.e("PagingSource", "Failed to parse document ${doc.id}", e)
                    null
                }
            }

            // Cải tiến logic nextKey ở đây
            val nextKey = if (observations.size >= PAGE_SIZE) {
                currentPage
            } else {
                null
            }

            Log.i("PagingSource", "Loaded ${observations.size} items for state '$state'")

            return LoadResult.Page(
                data = observations,
                prevKey = null,
                nextKey = nextKey
            )

        } catch (e: Exception) {
            Log.e("PagingSourceError", "Failed to load data for state '$state'", e)
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Observation>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
                ?: state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}