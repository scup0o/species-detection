package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.core.services.remote_database.observation.ObservationPagingSource
import com.project.speciesdetection.core.services.storage.StorageService
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class RemoteObservationRepository @Inject constructor(
    @Named("observation_db") private val databaseService: ObservationDatabaseService,
    private val storageService: StorageService,
    private val firestore: FirebaseFirestore,
    private val mapService: GeocodingService,
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase

    ) : ObservationRepository {

    companion object {
        private const val OBSERVATIONS_COLLECTION = "observations"
        private const val COMMENTS_COLLECTION = "comments"
        private const val PAGE_SIZE = 10
    }

    override suspend fun restoreObservation(observationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(OBSERVATIONS_COLLECTION).document(observationId)

            // Cập nhật trường 'state' về "normal" và cập nhật 'dateUpdated'
            val updates = mapOf(
                "state" to "normal",
                "dateUpdated" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            //Log.e("RestoreObservation", "Failed to restore observation $observationId", e)
            Result.failure(e)
        }
    }

    override fun listenToUserObservationCount(uid: String): Flow<Int> = callbackFlow {
        // Query chỉ các observation hợp lệ của người dùng
        val query = firestore.collection("observations")
            .whereEqualTo("uid", uid)
            .whereEqualTo("state", "normal")

        val listener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                close(error) // Đóng flow với lỗi
                return@addSnapshotListener
            }
            // snapshots.size() là số lượng document khớp với query
            trySend(snapshots?.size() ?: 0)
        }
        awaitClose { listener.remove() }
    }

    override fun listenToUserObservedSpecies(uid: String): Flow<List<DisplayableSpecies>> = callbackFlow {
        // Bước 1: Lắng nghe tất cả các observation của người dùng
        val observationsQuery = firestore.collection("observations")
            .whereEqualTo("uid", uid)
            .whereEqualTo("state", "normal")

        val listener = observationsQuery.addSnapshotListener { observationsSnapshot, error ->
            if (error != null || observationsSnapshot == null) {
                close(error)
                return@addSnapshotListener
            }

            // Bước 2: Từ danh sách observations, rút ra danh sách các speciesId duy nhất
            val speciesIds = observationsSnapshot.documents
                .mapNotNull { it.getString("speciesId") }
                .filter { it.isNotBlank() }
                .toSet() // .toSet() để loại bỏ các ID trùng lặp
                .toList()

            if (speciesIds.isEmpty()) {
                trySend(emptyList()) // Gửi danh sách rỗng nếu không có species nào
                return@addSnapshotListener
            }

            // =============================================================
            // === BƯỚC 3 MỚI: SỬ DỤNG USE CASE ĐỂ LẤY DỮ LIỆU SPECIES ===
            // =============================================================
            // UseCase là một suspend function, nên cần gọi trong một coroutine scope
            launch {
                try {
                    // Gọi hàm getById từ use case
                    val displayableSpeciesList = getLocalizedSpeciesUseCase.getById(
                        idList = speciesIds,
                        uid = uid // Truyền uid vào để UseCase có thể xử lý logic liên quan đến người dùng (nếu có)
                    )

                    // Gửi danh sách DisplayableSpecies hoàn chỉnh vào Flow
                    trySend(displayableSpeciesList)
                } catch (e: Exception) {
                    Log.e("ObservedSpecies", "Error fetching species via UseCase", e)
                    close(e) // Đóng flow nếu có lỗi khi gọi UseCase
                }
            }
        }
        awaitClose { listener.remove() }
    }

    override fun getHotObservationsPager(): Pager<Query, Observation> {
        val query = firestore.collection(OBSERVATIONS_COLLECTION)
            .whereEqualTo("privacy", "Public")
            .whereEqualTo("state", "normal") // Chỉ hiển thị các observation hợp lệ
            .orderBy("hotScore", Query.Direction.DESCENDING)

        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false // Tắt placeholders để tránh các item null trong danh sách
            ),
            pagingSourceFactory = { ObservationHotScorePagingSource(query) }
        )
    }

    override suspend fun createObservation(
        user: User,
        speciesId: String,
        content: String,
        imageUris: List<Uri>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?,
        locationTempName: String,
        speciesName: Map<String, String>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Tải tất cả ảnh lên song song
            val imageUrls = imageUris.map { uri ->
                async { storageService.uploadImage(uri) }
            }.awaitAll().mapNotNull { it.getOrNull() }

            val newObservation = Observation(
                uid = user.uid,
                userName = user.name ?: "",
                userImage = user.photoUrl ?: "",
                speciesId = speciesId,
                content = content,
                imageURL = imageUrls,
                privacy = privacy,
                location = location,
                dateFound = dateFound,
                point = 0, // Mới tạo,
                locationTempName = locationTempName,
                speciesName = speciesName
            )

            databaseService.createObservation(newObservation)
                .map { } // Convert Result<String> to Result<Unit>
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateObservation(
        user: User,
        observationId: String,
        speciesId: String,
        content: String,
        images: List<Any>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?,
        dateCreated: Timestamp?,
        commentCount: Int,
        point: Int,
        likeUserIds: List<String>,
        dislikeUserIds: List<String>,
        locationTempName: String,
        speciesName: Map<String, String>,
        baseObservation: Observation,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Phân loại ảnh cũ và ảnh mới cần upload
            val existingImageUrls = images.filterIsInstance<String>()
            val newImageUris = images.filterIsInstance<Uri>()

            // Tải ảnh mới lên
            val newImageUrls = newImageUris.map { uri ->
                async { storageService.uploadImage(uri) }
            }.awaitAll().mapNotNull { it.getOrNull() }

            val finalImageUrls = existingImageUrls + newImageUrls

            // Tạo đối tượng observation để update
            // Lưu ý: chỉ set các trường cần update, các trường khác sẽ được giữ nguyên nhờ SetOptions.merge()
            val observationToUpdate = Observation(
                uid = user.uid,
                id = observationId,
                userName = user.name ?: "",
                userImage = user.photoUrl ?: "",
                speciesId = speciesId,
                content = content,
                imageURL = finalImageUrls,
                privacy = privacy,
                location = location,
                dateFound = dateFound,
                dateCreated = dateCreated,
                commentCount = commentCount,
                point = point,
                likeUserIds = likeUserIds,
                dislikeUserIds = dislikeUserIds,
                locationTempName = locationTempName,
                speciesName = speciesName,
                saveUserIds = baseObservation.saveUserIds,
                hotScore = baseObservation.hotScore


            )

            databaseService.updateObservation(observationToUpdate, baseObservation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getObservationsStateForSpeciesList(
        species: List<DisplayableSpecies>,
        uid: String
    ): MutableMap<String, Timestamp> {
        var stateMap = mutableMapOf<String, Timestamp>()
        Log.i("i", species.toString())
        species.forEach { species ->
            var state = firestore.collection("observations").whereEqualTo("state", "normal")
                .whereEqualTo("uid", uid)
                .whereEqualTo("speciesId", species.id)
                .orderBy("dateFound", Query.Direction.ASCENDING)
                .limit(1).get().await()
            Log.i("i", state.toString())

            if (state != null && !state.isEmpty) {
                stateMap[species.id] = state.documents[0].getTimestamp("dateFound")!!
            }
        }
        return stateMap
        // Lấy 1 document đầu tiên
    }


    override suspend fun checkUserObservationState(
        uid: String,
        speciesId: String,
        onDataChanged: (Timestamp?) -> Unit
    ): ListenerRegistration {
        return databaseService.checkUserObservationState(uid, speciesId, onDataChanged)

    }

    override fun getObservationChangesForUser(userId: String): Flow<ObservationChange> =
        callbackFlow {
            // Sử dụng service để đăng ký listener.
            // Callback `onDataChanged` của chúng ta sẽ gọi `trySend(Unit)` để
            // phát tín hiệu ra cho Flow.
            val listener = databaseService.listenToUserObservations(userId) { it ->
                trySend(it) // Phát tín hiệu ra Flow
            }

            // awaitClose rất quan trọng. Khối lệnh này sẽ được thực thi khi
            // Flow bị hủy (ví dụ: khi coroutine scope của ViewModel bị hủy).
            // Nó đảm bảo listener được gỡ bỏ để tránh memory leak.
            awaitClose {
                listener.remove()
            }
        }

    /**
     * Lấy một Flow của PagingData. Flow này sẽ phát ra dữ liệu phân trang.
     */
    override fun getObservationPager(
        uid: String?,
        speciesId: String?,
        queryByDesc: Boolean?,
        userRequested: String,
        state: String
    ): Flow<PagingData<Observation>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false // Tắt placeholders để giao diện mượt hơn
            ),
            pagingSourceFactory = {
                // Pager sẽ tự động gọi factory này để tạo PagingSource mới khi cần
                ObservationPagingSource(
                    firestore,
                    uid,
                    speciesId,
                    queryByDesc,
                    userRequested,
                    state
                )
            }
        ).flow
    }


    /**
     * Đây là Flow "lắng nghe". Nó sẽ phát ra một giá trị Unit mỗi khi
     * có sự thay đổi trong collection 'observations' phù hợp với các bộ lọc.
     * Bạn sẽ dùng Flow này để biết khi nào cần refresh PagingDataAdapter.
     */
    override fun getObservationChanges(
        uid: String?,
        speciesId: String?,
        queryByDesc: Boolean?
    ): Flow<Unit> = callbackFlow {
        // Xây dựng câu truy vấn tương tự như trong PagingSource
        var query: Query = firestore.collection("observations").whereEqualTo("state", "normal")

        if (uid != null) {
            query = query.whereEqualTo("uid", uid)
        }
        if (speciesId != null) {
            query = query.whereEqualTo("speciesId", speciesId)
        }

        // Đăng ký listener
        val listenerRegistration = query.addSnapshotListener(MetadataChanges.INCLUDE) { _, _ ->
            // Khi có bất kỳ thay đổi nào, gửi một tín hiệu Unit
            // Chúng ta không quan tâm đến dữ liệu cụ thể, chỉ cần biết là có thay đổi.
            trySend(Unit)
        }

        // Khi Flow bị hủy, gỡ bỏ listener để tránh memory leak
        awaitClose {
            listenerRegistration.remove()
        }
    }

    // Trong file RemoteObservationRepository.kt

    override fun listenToObservationChanges(
        speciesId: String,
        uid: String?,
        queryByDesc: Boolean?,
        userRequested: String,
        state: String
    ): Flow<List<ObservationChange>> = callbackFlow {
        val listeners = mutableListOf<ListenerRegistration>()

        // =========================================================================
        // === XỬ LÝ RIÊNG BIỆT CHO TAB "SAVE" ===
        // =========================================================================
        if (state == "save") {
            if (uid.isNullOrEmpty()) {
                close()
                return@callbackFlow
            }

            val savesQuery = firestore.collectionGroup("saves").whereEqualTo("userId", uid)

            val savesListener = savesQuery.addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    close(error)
                    return@addSnapshotListener
                }

                snapshots.documentChanges.forEach { docChange ->
                    val observationId = docChange.document.reference.parent.parent!!.id

                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            // Một observation vừa được LƯU.
                            // Lấy dữ liệu và kiểm tra quyền xem trước khi gửi tín hiệu.
                            launch {
                                try {
                                    val observationDoc = firestore.collection("observations").document(observationId).get().await()
                                    if (observationDoc.exists()) {
                                        val observation = observationDoc.toObject(Observation::class.java)
                                        if (observation != null) {
                                            // ================================================
                                            // === ÁP DỤNG LOGIC LỌC PRIVACY NGAY TẠI ĐÂY ===
                                            // ================================================
                                            val isVisible = observation.privacy == "Public" || observation.uid == uid

                                            if (isVisible) {
                                                // Nếu được phép xem, gửi tín hiệu để thêm/cập nhật vào UI
                                                trySend(listOf(ObservationChange.Added(observation)))
                                            }
                                            // Nếu không được phép xem (isPrivate = false), không làm gì cả.
                                            // UI sẽ không nhận được tín hiệu và sẽ không hiển thị item này.
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ListenChanges[Save]", "Error fetching added observation $observationId", e)
                                }
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Một observation vừa được BỎ LƯU.
                            // Luôn gửi tín hiệu xóa vì nó chắc chắn đã có trên UI trước đó.
                            trySend(listOf(ObservationChange.Removed(observationId)))
                        }
                        else -> { /* Bỏ qua MODIFIED của collection 'saves' */ }
                    }
                }
            }
            listeners.add(savesListener)

            // --- Lắng nghe thay đổi nội dung của observation đã lưu ---
            // Phần này phức tạp và có thể cần thiết nếu bạn muốn cập nhật real-time
            // khi một bài viết đã lưu bị chủ nhân của nó chuyển từ Public sang Private.
            // Giải pháp đơn giản là Paging sẽ tự refresh khi người dùng vào lại màn hình.
            // Nếu cần, chúng ta có thể implement sau.

        } else {
            // =========================================================================
            // === LOGIC CHO CÁC TAB KHÁC (normal, deleted) - GIỮ NGUYÊN ===
            // =========================================================================
            // (Toàn bộ code trong khối else này không thay đổi)
            val sortBy = if (queryByDesc == true) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            var query: Query = firestore.collection("observations")
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
            if (state == "deleted") {
                query = query.orderBy("dateUpdated", sortBy)
            } else {
                query = query.orderBy("dateCreated", sortBy)
            }
            val primaryListener = query.addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    close(error)
                    return@addSnapshotListener
                }
                val changes = snapshots.documentChanges.mapNotNull { docChange ->
                    try {
                        val observation = docChange.document.toObject(Observation::class.java)
                        when (docChange.type) {
                            DocumentChange.Type.ADDED -> ObservationChange.Added(observation)
                            DocumentChange.Type.MODIFIED -> ObservationChange.Modified(observation)
                            DocumentChange.Type.REMOVED -> ObservationChange.Removed(docChange.document.id)
                        }
                    } catch (e: Exception) {
                        Log.e("ListenChanges", "Failed to parse observation ${docChange.document.id}", e)
                        null
                    }
                }
                if (changes.isNotEmpty()) {
                    trySend(changes)
                }
            }
            listeners.add(primaryListener)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    override fun getAllObservationsAsList(
        speciesId: String,
        uid: String?
    ): Flow<List<Observation>> = callbackFlow {
        var query: Query = firestore.collection("observations").whereEqualTo("state", "normal")
            .whereEqualTo("speciesId", speciesId)
            .orderBy("dateCreated", Query.Direction.DESCENDING)

        if (uid != null) {
            query = query.whereEqualTo("uid", uid)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val observations = snapshot?.toObjects(Observation::class.java) ?: emptyList()

            trySend(observations)
        }

        awaitClose { listener.remove() }
    }

    override fun observeObservationWithComments(
        observationId: String,
        sortDescending: Boolean
    ): Flow<Pair<Observation?, List<Comment>>> {
        val observationFlow = databaseService.listenObservation(observationId)
        val commentsFlow = databaseService.listenComments(observationId, sortDescending)

        // Kết hợp 2 flow để trả về cả observation và comments cùng lúc
        return combine(observationFlow, commentsFlow) { observation, comments ->
            observation to comments
        }
    }

    override fun observeObservationWithoutComments(observationId: String): Flow<Observation?> {
        return databaseService.listenObservation(observationId)
    }

    override fun observeObservation(observationId: String): Flow<Observation?> {
        return databaseService.listenObservation(observationId)
    }

    // 2. Tạo hàm mới để lắng nghe thay đổi của Comment collection
    //    Hàm này sẽ trả về chính xác loại thay đổi (thêm/sửa/xóa)
    override fun observeCommentUpdates(observationId: String): Flow<ListUpdate<Comment>> {
        return databaseService.listenCommentChanges(observationId) // Giả sử bạn có hàm này trong service
    }

    override suspend fun postComment(
        observationId: String,
        comment: Comment
    ): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            // Validate nội dung
            if (comment.content.isBlank() && comment.speciesId.isEmpty() && comment.imageUrl.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Nội dung bình luận không được để trống"))
            }

            // Upload ảnh (nếu có)
            val imageUrl = if (comment.imageUrl.isNotEmpty()) {
                storageService.uploadImage(Uri.decode(comment.imageUrl).toUri()).getOrNull()
                    .toString()
            } else ""

            val commentToPost = comment.copy(
                imageUrl = imageUrl,
                timestamp = null,
                updated = null
            )

            // Thêm comment vào Firestore
            val documentRef = firestore.collection("observations")
                .document(observationId)
                .collection("comments")
                .add(commentToPost)
                .await()

            // Tăng commentCount trong document của observation
            firestore.collection("observations")
                .document(observationId)
                .update("commentCount", FieldValue.increment(1))
                .await()


            // Lấy comment vừa tạo, thêm ID
            val savedComment = commentToPost.copy(id = documentRef.id)

            Result.success(savedComment)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun deleteComment(observationId: String, commentId: String): Result<Unit> {
        return try {
            val commentRef = firestore.collection("observations")
                .document(observationId)
                .collection("comments")
                .document(commentId)

            commentRef.update("state", "deleted").await()

            firestore.collection("observations")
                .document(observationId)
                .update("commentCount", FieldValue.increment(-1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLikeObservation(observationId: String, userId: String) {
        val docRef = firestore.collection("observations").document(observationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = snapshot.toObject(Observation::class.java) ?: return@runTransaction

            val likes = current.likeUserIds.toMutableList()
            val dislikes = current.dislikeUserIds.toMutableList()

            var point = current.point

            val hasLiked = userId in likes
            val hasDisliked = userId in dislikes

            if (hasLiked) {
                likes.remove(userId)
                point -= 1
            } else {
                likes.add(userId)
                point += 1
                if (hasDisliked) {
                    dislikes.remove(userId)
                    point += 1 // undo previous -1 from dislike
                }
            }

            transaction.update(
                docRef, mapOf(
                    "likeUserIds" to likes,
                    "dislikeUserIds" to dislikes,
                    "point" to point,
                    "dateUpdated" to Timestamp.now()
                )
            )
        }.await()
    }

    override suspend fun deleteObservation(observationId: String) {
        val docRef = firestore.collection("observations").document(observationId)
        docRef.update(mapOf("state" to "deleted", "dateUpdated" to Timestamp.now())).await()
    }

    override suspend fun toggleDislikeObservation(observationId: String, userId: String) {
        val docRef = firestore.collection("observations").document(observationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = snapshot.toObject(Observation::class.java) ?: return@runTransaction

            val likes = current.likeUserIds.toMutableList()
            val dislikes = current.dislikeUserIds.toMutableList()

            var point = current.point

            val hasLiked = userId in likes
            val hasDisliked = userId in dislikes

            if (hasDisliked) {
                dislikes.remove(userId)
                point += 1
            } else {
                dislikes.add(userId)
                point -= 1
                if (hasLiked) {
                    likes.remove(userId)
                    point -= 1 // undo previous +1 from like
                }
            }

            transaction.update(
                docRef, mapOf(
                    "likeUserIds" to likes,
                    "dislikeUserIds" to dislikes,
                    "point" to point,
                    "dateUpdated" to Timestamp.now()
                )
            )
        }.await()
    }

    override suspend fun toggleSaveObservation(
        observationId: String,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Tạo một tham chiếu đến document cụ thể trong sub-collection 'saves'
            // Đường dẫn sẽ là: observations/{observationId}/saves/{userId}
            val saveDocRef = firestore.collection("observations")
                .document(observationId)
                .collection("saves") // Tên của sub-collection
                .document(userId)   // Dùng UID của người dùng làm ID cho document

            // 2. Kiểm tra xem document này đã tồn tại hay chưa
            val snapshot = saveDocRef.get().await()

            if (snapshot.exists()) {
                // 3a. Nếu đã tồn tại (đã save) -> Xóa document đi để "unsave"
                saveDocRef.delete().await()
            } else {
                // 3b. Nếu chưa tồn tại -> Tạo mới document với timestamp
                val saveData = mapOf(
                    "userId" to userId, // <-- TRƯỜNG NÀY RẤT QUAN TRỌNG
                    "savedAt" to FieldValue.serverTimestamp()
                )
                saveDocRef.set(saveData).await()
            }

            // Đồng thời, chúng ta cũng nên cập nhật một mảng trong document observation chính
            // để dễ dàng kiểm tra nhanh trạng thái "đã lưu" mà không cần query sub-collection.
            // Đây là một kỹ thuật denormalization phổ biến và hiệu quả.
            val observationDocRef = firestore.collection("observations").document(observationId)
            if (snapshot.exists()) {
                // Xóa userId khỏi mảng 'savedBy'
                observationDocRef.update("savedBy", FieldValue.arrayRemove(userId)).await()
            } else {
                // Thêm userId vào mảng 'savedBy'
                observationDocRef.update("savedBy", FieldValue.arrayUnion(userId)).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ToggleSaveError", "Failed to toggle save for obs $observationId by user $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleLikeComment(
        observationId: String,
        commentId: String,
        userId: String
    ) {
        // Đường dẫn tới document comment cụ thể
        val docRef = firestore.collection("observations").document(observationId)
            .collection("comments").document(commentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentComment = snapshot.toObject(Comment::class.java) ?: return@runTransaction

            val likes = currentComment.likeUserIds.toMutableList()
            val dislikes = currentComment.dislikeUserIds.toMutableList()
            var point = currentComment.likeCount

            val hasLiked = userId in likes
            val hasDisliked = userId in dislikes

            if (hasLiked) {
                // Nếu đã thích -> bỏ thích
                likes.remove(userId)
                point -= 1
            } else {
                // Nếu chưa thích -> thêm thích
                likes.add(userId)
                point += 1
                if (hasDisliked) {
                    // Nếu đang không thích -> bỏ không thích
                    dislikes.remove(userId)
                    point += 1 // +1 cho việc bỏ không thích
                }
            }

            // Cập nhật lại document
            transaction.update(
                docRef, mapOf(
                    "likeUserIds" to likes,
                    "dislikeUserIds" to dislikes,
                    "likeCount" to point
                )
            )
        }.await() // Chờ transaction hoàn tất
    }

    override suspend fun toggleDislikeComment(
        observationId: String,
        commentId: String,
        userId: String
    ) {
        val docRef = firestore.collection("observations").document(observationId)
            .collection("comments").document(commentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentComment = snapshot.toObject(Comment::class.java) ?: return@runTransaction

            val likes = currentComment.likeUserIds.toMutableList()
            val dislikes = currentComment.dislikeUserIds.toMutableList()
            var point = currentComment.likeCount

            val hasLiked = userId in likes
            val hasDisliked = userId in dislikes

            if (hasDisliked) {
                // Nếu đã không thích -> bỏ không thích
                dislikes.remove(userId)
                point += 1
            } else {
                // Nếu chưa không thích -> thêm không thích
                dislikes.add(userId)
                point -= 1
                if (hasLiked) {
                    // Nếu đang thích -> bỏ thích
                    likes.remove(userId)
                    point -= 1 // -1 cho việc bỏ thích
                }
            }

            transaction.update(
                docRef, mapOf(
                    "likeUserIds" to likes,
                    "dislikeUserIds" to dislikes,
                    "likeCount" to point
                )
            )
        }.await()
    }


}