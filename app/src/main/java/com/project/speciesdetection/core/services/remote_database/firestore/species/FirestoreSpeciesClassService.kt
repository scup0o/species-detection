package com.project.speciesdetection.core.services.remote_database.firestore.species

import android.util.Log
import androidx.paging.PagingData
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.core.services.remote_database.DatabaseService
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSpeciesClassService @Inject constructor() : DatabaseService<SpeciesClass, String>
{
    private val firestore = Firebase.firestore
    private val speciesClassCollection = firestore.collection("speciesClass")
    private val TAG = "FirestoreSpeciesClassService"

    override fun getAll(options: Map<String, Any>?): Flow<DataResult<List<SpeciesClass>>> = callbackFlow {

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

        /*val speciesList = mutableListOf<SpeciesClass>()
        for (document in querySnapshot.documents) {
            try {
                val species = SpeciesClass(
                    id = document.id,
                    nameVietnamese = document.getString("nameVietnamese"),
                    nameScientific = document.getString("nameScientific"),
                    description = document.getString("description")
                )
                speciesList.add(species)

                // Hoặc dùng toObject nếu data class có @DocumentId
                // val species = document.toObject(SpeciesClass::class.java)
                // if (species != null) {
                //     speciesList.add(species)
                // }
            } catch (e: Exception) {
                Log.e("Firestore", "Lỗi khi parse document ${document.id}: ${e.message}", e)
            }
        }*/
    }

    override fun getByFieldValue(
        fieldPath: String,
        value: Any,
        options: Map<String, Any>?
    ): Flow<DataResult<List<SpeciesClass>>> {
        TODO("Not yet implemented")
    }

    override fun getByFieldValuePaged(
        fieldPath: String,
        value: Any,
        pageSize: Int,
        orderByField: String?,
        sortDirection: Query.Direction
    ): Flow<PagingData<SpeciesClass>> {
        TODO("Not yet implemented")
    }
}