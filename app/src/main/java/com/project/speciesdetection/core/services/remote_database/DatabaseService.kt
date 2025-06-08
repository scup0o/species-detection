package com.project.speciesdetection.core.services.remote_database

import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.project.speciesdetection.data.model.observation.Observation
import kotlinx.coroutines.flow.Flow

sealed class DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Error(val exception: Exception) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}

interface SpeciesDatabaseService<T : Any, ID> {
    fun getAll(languageCode : String,
               searchQuery : List<String>?=null,
               sortDirection: Query.Direction,
               pageSize: Int,
               orderByField: String?,): Flow<PagingData<T>>
    /*fun getById(id: ID): Flow<DataResult<T?>>
    fun getByFieldValue(fieldPath: String, value: Any, options: Map<String, Any>? = null): Flow<DataResult<List<T>>>
    //suspend fun add(item: T, documentId: String? = null): DataResult<String>
    //suspend fun update(id: ID, item: T): DataResult<Unit>
    //suspend fun delete(id: ID): DataResult<Unit>*/

    fun getByFieldValuePaged(
        languageCode : String,
        searchQuery : List<String>?=null,
        fieldPath: String,
        value: Any,
        pageSize: Int,
        orderByField: String?,
        sortDirection: Query.Direction
    ): Flow<PagingData<T>>

    suspend fun getById(
        idList : List<String>
    ) : List<T>
}

interface SpeciesClassDatabaseService<T : Any, ID> {
    fun getAllSpeciesClass(options: Map<String, Any>? = null): Flow<DataResult<List<T>>>
    suspend fun getAll() : List<T>
}

interface UserDatabaseService<T : Any, ID> {
}

interface ObservationDatabaseService {
    suspend fun createObservation(observation: Observation): Result<String>
    suspend fun updateObservation(observation: Observation): Result<Unit>
    suspend fun checkUserObservationState(uid: String, speciesId: String, onDataChanged: (Timestamp?) -> Unit): ListenerRegistration
}