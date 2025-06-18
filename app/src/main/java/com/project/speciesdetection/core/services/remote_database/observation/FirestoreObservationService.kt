package com.project.speciesdetection.core.services.remote_database.observation

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreObservationService @Inject constructor(
    private val firestore: FirebaseFirestore
) : ObservationDatabaseService {

    private val observationCollection = firestore.collection("observations")

    override suspend fun createObservation(observation: Observation): Result<String> {
        return try {
            val documentReference = observationCollection.add(observation).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateObservation(observation: Observation): Result<Unit> {
        return try {
            requireNotNull(observation.id) { "Observation ID cannot be null for an update." }
            observationCollection.document(observation.id!!).set(observation, SetOptions.merge()).await()
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
}