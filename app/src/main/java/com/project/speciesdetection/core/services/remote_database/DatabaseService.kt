package com.project.speciesdetection.core.services.remote_database

import kotlinx.coroutines.flow.Flow

sealed class DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Error(val exception: Exception) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}

interface DatabaseService<T, ID> {
    fun getAll(options: Map<String, Any>? = null): Flow<DataResult<List<T>>>
    //fun getById(id: ID): Flow<DataResult<T?>>
    fun getByFieldValue(fieldPath: String, value: Any, options: Map<String, Any>? = null): Flow<DataResult<List<T>>>
    //suspend fun add(item: T, documentId: String? = null): DataResult<String>
    //suspend fun update(id: ID, item: T): DataResult<Unit>
    //suspend fun delete(id: ID): DataResult<Unit>
}