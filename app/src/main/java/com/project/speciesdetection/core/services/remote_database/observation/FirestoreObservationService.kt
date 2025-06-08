package com.project.speciesdetection.core.services.remote_database.observation

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.data.model.observation.Observation
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
        val query: Query = observationCollection
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
                onDataChanged(null)  // Không có dữ liệu
            }
        }
    }
}