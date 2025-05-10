package com.project.speciesdetection.core.services.remote_database.firestore

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.DatabaseService
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FirestoreSpeciesService @Inject constructor(
) : DatabaseService<Species, String> {
    private val firestore = Firebase.firestore

    private val speciesCollection = firestore.collection("species")
    private val TAG = "FirestoreSpeciesService"
    override fun getByFieldValue(
        fieldPath: String,
        value: Any,
        options: Map<String, Any>?
    ): Flow<DataResult<List<Species>>> = callbackFlow {
        trySend(DataResult.Loading)
        var query: Query = speciesCollection.whereEqualTo(fieldPath, value)
        val orderByField = options?.get("orderBy") as? String
        val sortDirectionOption = options?.get("sortDirection") as? String
        val direction = if (sortDirectionOption == "DESC") Query.Direction.DESCENDING else Query.Direction.ASCENDING

        if (orderByField != null) {
            query = query.orderBy(orderByField, direction)
        } else {
            query = query.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
        }
        try {
            val querySnapshot = query.get().await()
            val speciesList = querySnapshot.documents.mapNotNull { document ->
                try { val species: Species? = document.toObject(Species::class.java)
                    species?.apply { id = document.id }}
                catch (e: Exception) { Log.e(TAG, "Error converting document ${document.id}", e); null }
            }
            Log.d(TAG, "Fetched ${speciesList.size} species by field '$fieldPath' = '$value'")
            trySend(DataResult.Success(speciesList)).isSuccess
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching species by field '$fieldPath' = '$value'", e)
            trySend(DataResult.Error(e)).isSuccess
            close(e)
        }
        awaitClose { }
    }

}