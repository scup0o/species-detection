package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import android.nfc.tech.MifareUltralight.PAGE_SIZE
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.core.services.remote_database.observation.ObservationPagingSource
import com.project.speciesdetection.core.services.storage.StorageService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        locationTempName : String
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
                locationTempName =locationTempName
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
        locationTempName : String
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
                locationTempName = locationTempName
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
                var state = firestore.collection("observations")
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
        var query: Query = firestore.collection("observations")

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

        var query: Query = firestore.collection("observations")
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
        var query: Query = firestore.collection("observations")
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

}