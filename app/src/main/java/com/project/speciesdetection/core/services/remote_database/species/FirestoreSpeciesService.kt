package com.project.speciesdetection.core.services.remote_database.species

import android.util.Log
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.Pager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.project.speciesdetection.core.services.remote_database.SpeciesDatabaseService
import com.project.speciesdetection.data.model.species.Species
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirestoreSpeciesService"
private const val FIRESTORE_SPECIES_SERVICE_TAG = "FirestoreSpeciesService"
const val DEFAULT_SPECIES_PAGE_SIZE = 5

@Singleton
class FirestoreSpeciesService @Inject constructor(
) : SpeciesDatabaseService<Species, String> {

    private val firestore = Firebase.firestore
    private val speciesCollection = firestore.collection("species")

    override fun getAll(
        languageCode: String,
        searchQuery: List<String>?,
        sortDirection: Query.Direction,
        pageSize: Int,
        orderByField: String?
    ): Flow<PagingData<Species>> {
        var baseQuery: Query = if (searchQuery!=null)
            speciesCollection.where(Filter.or(
                    Filter.arrayContainsAny("name_tokens.$languageCode",searchQuery),
                    Filter.arrayContainsAny("scientificNameToken",searchQuery)
                ))
        else speciesCollection


        if (orderByField != null) {
            baseQuery = baseQuery.orderBy(orderByField, sortDirection)
        } else {
            baseQuery = baseQuery.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
        }

        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                SpeciesPagingSource(
                    baseQuery = baseQuery,
                    pageSize = pageSize,
                    searchQuery = searchQuery,
                    languageCode = languageCode
                )
            }
        ).flow
    }

    override fun getByFieldValuePaged(
        languageCode : String,
        searchQuery : List<String>?,
        fieldPath: String,
        value: Any,
        pageSize: Int,
        orderByField: String?,
        sortDirection: Query.Direction
    ): Flow<PagingData<Species>> {
        Log.d(FIRESTORE_SPECIES_SERVICE_TAG, "Creating PagingStream for field '$fieldPath' = '$value', orderBy: $orderByField, direction: $sortDirection")

        var baseQuery: Query = if (searchQuery!=null)
                                speciesCollection.whereEqualTo(fieldPath, value)
                                    .where(Filter.or(
                                        Filter.arrayContainsAny("name_tokens.$languageCode",searchQuery),
                                        Filter.arrayContainsAny("scientificNameToken",searchQuery)
                                    ))
                                else speciesCollection.whereEqualTo(fieldPath, value)


        if (orderByField != null) {
            baseQuery = baseQuery.orderBy(orderByField, sortDirection)
        } else {
            // Mặc định sắp xếp theo ID document nếu không có orderBy nào được chỉ định
            // Điều này quan trọng để pagination ổn định với startAfter
            baseQuery = baseQuery.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
        }

        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                SpeciesPagingSource(
                    baseQuery = baseQuery,
                    pageSize = pageSize,
                    searchQuery = searchQuery,
                    languageCode = languageCode
                )
            }
        ).flow
    }

    /*override fun getByFieldValue(
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
    }*/

}