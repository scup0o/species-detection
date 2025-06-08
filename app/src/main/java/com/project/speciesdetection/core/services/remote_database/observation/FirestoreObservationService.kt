package com.project.speciesdetection.core.services.remote_database.observation

import com.google.firebase.firestore.FirebaseFirestore
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
}