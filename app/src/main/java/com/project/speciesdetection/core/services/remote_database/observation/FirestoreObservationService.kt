package com.project.speciesdetection.core.services.remote_database.observation

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ListUpdate
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreObservationService @Inject constructor(
    private val firestore: FirebaseFirestore
) : ObservationDatabaseService {

    private val observationCollection = firestore.collection("observations")

    override suspend fun createObservation(observation: Observation): Result<String> {
        return try {
            // Lấy speciesName từ collection "species"
            val speciesSnapshot = FirebaseFirestore.getInstance()
                .collection("species")
                .document(observation.speciesId)
                .get()
                .await()

            val speciesNameMap = speciesSnapshot.get("name") as? Map<String, String>
            val updatedObservation = observation.copy(
                speciesName = speciesNameMap ?: observation.speciesName
            )

            val documentReference = observationCollection.add(updatedObservation).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateObservation(observation: Observation): Result<Unit> {
        return try {
            requireNotNull(observation.id) { "Observation ID cannot be null for an update." }

            val speciesSnapshot = FirebaseFirestore.getInstance()
                .collection("species")
                .document(observation.speciesId)
                .get()
                .await()

            val speciesNameMap = speciesSnapshot.get("name") as? Map<String, String>
            val updatedObservation = observation.copy(
                speciesName = speciesNameMap ?: observation.speciesName
            )

            observationCollection
                .document(observation.id!!)
                .set(updatedObservation, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUserObservationState(uid: String, speciesId: String, onDataChanged: (Timestamp?) -> Unit): ListenerRegistration {
        val query: Query = observationCollection.whereEqualTo("state", "normal")
            .whereEqualTo("uid", uid)
            .whereEqualTo("speciesId", speciesId) // Chỉ lấy dữ liệu của 'uid' này
            .orderBy("dateFound", Query.Direction.ASCENDING)  // Sắp xếp theo 'dateFound'
            .limit(1)  // Lấy 1 document đầu tiên

        // Lắng nghe thay đổi real-time
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreObservationService", "Error fetching data", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                // Lấy document đầu tiên từ snapshot
                val document = snapshot.documents[0]
                val dateFound = document.getTimestamp("dateFound")
                onDataChanged(dateFound)
            } else {
                Log.i("obs", "null")
                onDataChanged(null)  // Không có dữ liệu
            }
        }
    }

    override fun listenToUserObservations(uid: String, onDataChanged: (ObservationChange) -> Unit): ListenerRegistration {
        val query: Query = observationCollection.whereEqualTo("uid", uid).whereEqualTo("state", "normal")

        // addSnapshotListener sẽ được kích hoạt cho lần đầu tiên và mỗi khi có thay đổi.
        // Chúng ta không cần dữ liệu (snapshot), chỉ cần biết là nó đã được kích hoạt.
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Bạn có thể log lỗi ở đây nếu muốn
                return@addSnapshotListener
            }



            if (snapshot != null && !snapshot.isEmpty) {
                for (docChange in snapshot.documentChanges) {
                    val change = when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val observation = docChange.document.toObject(Observation::class.java)
                            ObservationChange.Added(observation)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val observation = docChange.document.toObject(Observation::class.java)
                            ObservationChange.Modified(observation)
                        }
                        DocumentChange.Type.REMOVED -> {
                            val observation = docChange.document.toObject(Observation::class.java)
                            var result: ObservationChange? = null

                            // Vì Firestore `.get().await()` là suspend -> dùng coroutine ở đây
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val snap = observationCollection
                                        .whereEqualTo("uid", uid)
                                        .whereEqualTo("speciesId", observation.speciesId)
                                        .orderBy("dateFound", Query.Direction.ASCENDING)
                                        .limit(1)
                                        .get()
                                        .await()

                                    result = if (snap.isEmpty) {
                                        ObservationChange.Removed(observation.speciesId)
                                    } else {
                                        ObservationChange.Removed(docChange.document.id)
                                    }

                                    result?.let { onDataChanged(it) }

                                } catch (e: Exception) {
                                    // Log lỗi nếu cần
                                }
                            }

                            null // Trả null ngay vì xử lý async sẽ gọi callback sau
                        }

                        else -> null
                    }

                    change?.let { onDataChanged(it) }
                }
            }
        }
    }

    override fun listenObservation(observationId: String): Flow<Observation?> = callbackFlow {
        val docRef = firestore.collection("observations").document(observationId)

        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreService", "Observation listen failed", error)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val observation = snapshot.toObject(Observation::class.java)
                trySend(observation)
            } else {
                trySend(null)
            }
        }

        awaitClose{ listenerRegistration.remove() }
    }

    override fun listenComments(
        observationId: String,
        sortDescending: Boolean // Mặc định là tăng dần
    ): Flow<List<Comment>> = callbackFlow {
        val commentsRef = firestore.collection("observations")
            .document(observationId)
            .collection("comments")
            .whereEqualTo("state", "normal")
            .orderBy("timestamp", if (sortDescending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)

        val listenerRegistration = commentsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("FirestoreService", "Comments listen failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val comments = snapshots?.toObjects(Comment::class.java) ?: emptyList()
            trySend(comments)
        }

        awaitClose { listenerRegistration.remove() }
    }

    override fun listenCommentChanges(observationId: String): Flow<ListUpdate<Comment>> = callbackFlow {
        if (observationId.isEmpty()) {
            close(IllegalArgumentException("Observation ID cannot be empty."))
            return@callbackFlow
        }

        val listener = firestore.collection("observations").document(observationId)
            .collection("comments")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // Đóng flow và báo lỗi
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (change in snapshots.documentChanges) {
                    // Cố gắng parse document thành object Comment
                    val comment = try {
                        change.document.toObject(Comment::class.java).copy(id = change.document.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreService", "Failed to parse comment document", e)
                        continue // Bỏ qua document bị lỗi và xử lý cái tiếp theo
                    }
                    if (comment.state != "normal") {
                        // Gửi đi sự kiện "Removed" nếu state != "normal"
                        trySend(ListUpdate.Removed(comment))
                    } else {
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            // Gửi đi sự kiện "Added"
                            trySend(ListUpdate.Added(comment))
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // Gửi đi sự kiện "Modified"
                            trySend(ListUpdate.Modified(comment))
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Gửi đi sự kiện "Removed"
                            trySend(ListUpdate.Removed(comment))
                        }
                    }}
                }
            }

        // Khi flow bị hủy (ViewModel bị onCleared), gỡ bỏ listener
        awaitClose { listener.remove() }
    }
}