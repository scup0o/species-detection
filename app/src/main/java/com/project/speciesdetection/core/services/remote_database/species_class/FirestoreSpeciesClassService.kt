package com.project.speciesdetection.core.services.remote_database.species_class

import android.util.Log
import androidx.paging.PagingData
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.SpeciesClassDatabaseService
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSpeciesClassService @Inject constructor() :
    SpeciesClassDatabaseService<SpeciesClass, String>
{
    private val firestore = Firebase.firestore
    private val speciesClassCollection = firestore.collection("speciesClass")
    private val TAG = "FirestoreSpeciesClassService"

    override fun getAllSpeciesClass(options: Map<String, Any>?): Flow<DataResult<List<SpeciesClass>>> = callbackFlow {

        trySend(DataResult.Loading)
        try {
            val querySnapshot = speciesClassCollection.get().await()
            val speciesClassList = querySnapshot.documents.mapNotNull { document ->
                try { val speciesClass: SpeciesClass? = document.toObject(SpeciesClass::class.java)
                    speciesClass?.apply { id = document.id }}
                catch (e: Exception) { Log.e(TAG, "Error converting document ${document.id}", e); null }
            }
            Log.d(TAG, "Fetched ${speciesClassList.size}")
            trySend(DataResult.Success(speciesClassList)).isSuccess
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching speciesClass", e)
            trySend(DataResult.Error(e)).isSuccess
            close(e)
        }
        awaitClose { }
    }

    override suspend fun getAll(): List<SpeciesClass> {
        val querySnapshot = speciesClassCollection.get().await()
        val speciesClassList = querySnapshot.documents.mapNotNull { document ->
            try { val speciesClass: SpeciesClass? = document.toObject(SpeciesClass::class.java)
                speciesClass?.apply { id = document.id }
                }
            catch (e: Exception) { Log.e(TAG, "Error converting document ${document.id}", e); null }
        }
        return speciesClassList
    }


}