package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import android.util.Log
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class RemoteObservationRepository @Inject constructor(
    @Named("observation_db") private val databaseService: ObservationDatabaseService,
    private val storageService: StorageService,
    private val firestore: FirebaseFirestore,
    private val mapService : GeocodingService
) : ObservationRepository {

    override suspend fun createObservation(
        user: User,
        speciesId: String,
        content: String,
        imageUris: List<Uri>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?,
        locationTempName : String,
        speciesName : Map<String, String>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Tải tất cả ảnh lên song song
            val imageUrls = imageUris.map { uri ->
                async { storageService.uploadImage(uri) }
            }.awaitAll().mapNotNull { it.getOrNull() }

            val newObservation = Observation(
                uid = user.uid,
                userName = user.name?:"",
                userImage = user.photoUrl?:"",
                speciesId = speciesId,
                content = content,
                imageURL = imageUrls,
                privacy = privacy,
                location = location,
                dateFound = dateFound,
                point = 0, // Mới tạo,
                locationTempName =locationTempName,
                speciesName = speciesName
            )

            databaseService.createObservation(newObservation).map { } // Convert Result<String> to Result<Unit>
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
        commentCount : Int,
        point : Int,
        likeUserIds : List<String>,
        dislikeUserIds : List<String>,
        locationTempName : String,
        speciesName : Map<String, String>,
        baseObservation : Observation,
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
                userName = user.name?:"",
                userImage = user.photoUrl?:"",
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


            )

            databaseService.updateObservation(observationToUpdate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getObservationsStateForSpeciesList(
        species: List<DisplayableSpecies>,
        uid : String
        ) : MutableMap<String, Timestamp>{
        var stateMap = mutableMapOf<String, Timestamp>()
        Log.i("i", species.toString())
        species.forEach {
            species ->
                var state = firestore.collection("observations").whereEqualTo("state", "normal")
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("speciesId", species.id)
                    .orderBy("dateFound", Query.Direction.ASCENDING)
                    .limit(1).get().await()
                Log.i("i", state.toString())

                if (state!=null && !state.isEmpty){
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
        return databaseService.checkUserObservationState(uid,speciesId,onDataChanged)

    }

    override fun getObservationChangesForUser(userId: String): Flow<ObservationChange> = callbackFlow {
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
    override fun getObservationPager(uid: String?, speciesId: String?, queryByDesc : Boolean?, ): Flow<PagingData<Observation>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false // Tắt placeholders để giao diện mượt hơn
            ),
            pagingSourceFactory = {
                // Pager sẽ tự động gọi factory này để tạo PagingSource mới khi cần
                ObservationPagingSource(firestore, uid, speciesId, queryByDesc)
            }
        ).flow
    }

    /**
     * Đây là Flow "lắng nghe". Nó sẽ phát ra một giá trị Unit mỗi khi
     * có sự thay đổi trong collection 'observations' phù hợp với các bộ lọc.
     * Bạn sẽ dùng Flow này để biết khi nào cần refresh PagingDataAdapter.
     */
    override fun getObservationChanges(uid: String?, speciesId: String?, queryByDesc : Boolean?): Flow<Unit> = callbackFlow {
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

    override fun listenToObservationChanges(speciesId: String, uid: String?, queryByDesc: Boolean? ): Flow<List<ObservationChange>> = callbackFlow {
        // Xây dựng câu truy vấn tương tự như trong PagingSource
        val sortBy = if (queryByDesc!!) Query.Direction.DESCENDING else Query.Direction.ASCENDING

        var query: Query = firestore.collection("observations").whereEqualTo("state", "normal")
            .whereEqualTo("speciesId", speciesId)
        Log.i("aaaa", sortBy.toString())

        if (uid != null) {
            query = query.whereEqualTo("uid", uid)
        }else{
            query = query.whereEqualTo("privacy", "Public")

            Log.i("aaaa", "privacy")
        }
        query = query.orderBy("dateCreated", sortBy)


        val listener = query.addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null) {
                // Có thể đóng flow với lỗi nếu muốn
                return@addSnapshotListener
            }

            // Lấy danh sách các thay đổi từ snapshot
            val changes = snapshots.documentChanges.mapNotNull { docChange ->
                when (docChange.type) {
                    DocumentChange.Type.ADDED -> {
                        val observation = docChange.document.toObject(Observation::class.java)
                        ObservationChange.Added(observation)
                    }
                    DocumentChange.Type.MODIFIED -> {
                        val observation = docChange.document.toObject(Observation::class.java)
                        ObservationChange.Modified(observation)
                    }
                    DocumentChange.Type.REMOVED -> {
                        ObservationChange.Removed(docChange.document.id)
                    }
                }
            }

            if (changes.isNotEmpty()) {
                // Gửi danh sách các thay đổi vào Flow
                trySend(changes)
            }
        }

        awaitClose { listener.remove() }
    }

    override fun getAllObservationsAsList(speciesId: String, uid: String?): Flow<List<Observation>> = callbackFlow {
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

    override fun observeObservationWithComments(observationId: String, sortDescending: Boolean): Flow<Pair<Observation?, List<Comment>>> {
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
                storageService.uploadImage(Uri.decode(comment.imageUrl).toUri()).getOrNull().toString()
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

            transaction.update(docRef, mapOf(
                "likeUserIds" to likes,
                "dislikeUserIds" to dislikes,
                "point" to point,
                "dateUpdated" to Timestamp.now()
            ))
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

            transaction.update(docRef, mapOf(
                "likeUserIds" to likes,
                "dislikeUserIds" to dislikes,
                "point" to point,
                "dateUpdated" to Timestamp.now()
            ))
        }.await()
    }

    override suspend fun toggleSaveObservation(observationId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("observations").document(observationId)
            val snapshot = docRef.get().await()

            val currentMap = snapshot.get("saveUserIds") as? Map<String, String> ?: emptyMap()
            val newMap = if (currentMap.containsKey(userId)) {
                currentMap - userId
            } else {
                currentMap + (userId to System.currentTimeMillis().toString())
            }

            docRef.update("saveUserIds", newMap).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLikeComment(observationId: String, commentId: String, userId: String) {
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
            transaction.update(docRef, mapOf(
                "likeUserIds" to likes,
                "dislikeUserIds" to dislikes,
                "likeCount" to point
            ))
        }.await() // Chờ transaction hoàn tất
    }

    override suspend fun toggleDislikeComment(observationId: String, commentId: String, userId: String) {
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

            transaction.update(docRef, mapOf(
                "likeUserIds" to likes,
                "dislikeUserIds" to dislikes,
                "likeCount" to point
            ))
        }.await()
    }


}