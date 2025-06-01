package com.project.speciesdetection.core.services.remote_database.species

import android.util.Log // Hoặc Timber nếu bạn dùng
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot // THAY ĐỔI QUAN TRỌNG
import com.google.firebase.firestore.Query
// import com.google.firebase.firestore.QuerySnapshot // Không cần trực tiếp làm kiểu Key nữa
import com.project.speciesdetection.data.model.species.Species // Đảm bảo import đúng model
import kotlinx.coroutines.tasks.await
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


class DecryptedSpeciesPagingSource(
    private val baseQuery: Query,
    private val pageSize: Int,
    private val searchQuery: List<String>? = null,
    private val lastQuery: String = "",
    private val languageCode: String
) : PagingSource<DocumentSnapshot, Species>() {

    companion object {
        // Prefetch buffer: lưu lại các Species đã lọc nhưng chưa dùng hết
        private val prefetchBuffer = mutableListOf<Species>()

        // Hàm clear buffer
        fun clearBuffer() {
            prefetchBuffer.clear()
        }
    }

    init {
        // Clear buffer khi tạo instance mới của PagingSource (khi người dùng thay đổi searchQuery hoặc lastQuery)
        clearBuffer()
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Species> {
        try {
            val finalSpeciesList = mutableListOf<Species>()
            var currentKey = params.key
            var lastDocumentFetched: DocumentSnapshot? = null

            // 👉 1. Ưu tiên dùng dữ liệu còn trong buffer trước
            if (prefetchBuffer.isNotEmpty()) {
                val takeFromBuffer = prefetchBuffer.take(pageSize)
                finalSpeciesList.addAll(takeFromBuffer)
                prefetchBuffer.subList(0, takeFromBuffer.size).clear() // Xoá sau khi dùng

                // Nếu đã đủ rồi thì return luôn
                if (finalSpeciesList.size == pageSize) {
                    return LoadResult.Page(
                        data = finalSpeciesList,
                        prevKey = null,
                        nextKey = currentKey // vẫn giữ currentKey vì chưa fetch mới
                    )
                }
            }

            // 👉 2. Nếu buffer chưa đủ, tiếp tục fetch từ Firestore
            val desiredSize = pageSize
            val fetchMultiplier = 3

            while (finalSpeciesList.size < desiredSize) {
                val currentQuery = currentKey?.let {
                    baseQuery.startAfter(it).limit((
                            if (!searchQuery.isNullOrEmpty() || lastQuery.isNotEmpty()) pageSize * fetchMultiplier
                            else pageSize).toLong())
                } ?: baseQuery.limit((
                        if (!searchQuery.isNullOrEmpty() || lastQuery.isNotEmpty()) pageSize * fetchMultiplier
                        else pageSize).toLong())

                val querySnapshot = currentQuery.get().await()
                val documents = querySnapshot.documents

                if (documents.isEmpty()) break // hết dữ liệu

                lastDocumentFetched = documents.lastOrNull()
                currentKey = lastDocumentFetched

                // Convert & lọc
                val mappedSpecies = documents.mapNotNull { doc ->
                    try {
                        val species = doc.toObject(Species::class.java)
                        species?.apply { id = doc.id }
                    } catch (e: Exception) {
                        Log.e(PAGING_SOURCE_TAG, "Error converting document ${doc.id}", e)
                        null
                    }
                }
                var filteredSpecies = if (!searchQuery.isNullOrEmpty() || lastQuery.isNotEmpty()){
                    mappedSpecies.filter { species ->
                        val nameTokens = species.nameTokens?.get(languageCode) ?: emptyList()
                        val sciTokens = species.scientificNameToken ?: emptyList()
                        val combined = nameTokens + sciTokens

                        val matchesToken = searchQuery?.all { combined.contains(it) } ?: true
                        val matchesText = species.name[languageCode]?.contains(lastQuery, ignoreCase = true) == true ||
                                species.scientificName.contains(lastQuery, ignoreCase = true)

                        matchesToken && matchesText
                    }
                } else mappedSpecies

                val remaining = desiredSize - finalSpeciesList.size
                finalSpeciesList.addAll(filteredSpecies.take(remaining))

                // 👉 3. Lưu lại phần dư vào buffer
                if (filteredSpecies.size > remaining) {
                    prefetchBuffer.addAll(filteredSpecies.drop(remaining))
                }

                if (documents.size < pageSize * fetchMultiplier) break // Không còn batch mới
            }

            return LoadResult.Page(
                data = finalSpeciesList,
                prevKey = null,
                nextKey = lastDocumentFetched.takeIf { finalSpeciesList.isNotEmpty() }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(PAGING_SOURCE_TAG, "Error loading species with buffer", e)
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Species>): DocumentSnapshot? {
        // Clear buffer nếu refresh
        clearBuffer()
        return null
    }
}