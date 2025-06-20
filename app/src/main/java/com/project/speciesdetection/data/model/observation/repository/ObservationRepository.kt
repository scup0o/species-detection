package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.flow.Flow

sealed class ObservationChange {
    data class Modified(val observation: Observation) : ObservationChange()
    data class Added(val observation: Observation) : ObservationChange()
    data class Removed(val observationId: String) : ObservationChange()
}

sealed class ListUpdate<T> {
    data class Added<T>(val item: T) : ListUpdate<T>()
    data class Modified<T>(val item: T) : ListUpdate<T>()
    data class Removed<T>(val item: T) : ListUpdate<T>()
}

interface ObservationRepository {
    suspend fun createObservation(
        user: User,
        speciesId: String,
        content: String,
        imageUris: List<Uri>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?,
        locationTempName : String,
        speciesName : Map<String, String>,
    ): Result<Unit>

    suspend fun updateObservation(
        user : User,
        observationId: String,
        speciesId: String,
        content: String,
        images: List<Any>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?,
        dateCreated: Timestamp?,
        commentCount : Int,
        point : Int,
        likeUserIds : List<String>,
        dislikeUserIds : List<String>,
        locationTempName : String,
        speciesName : Map<String, String>,
        baseObservation : Observation
    ): Result<Unit>

    fun getObservationChangesForUser(userId: String): Flow<ObservationChange>
    suspend fun checkUserObservationState(uid: String, speciesId: String, onDataChanged: (Timestamp?) -> Unit): ListenerRegistration
    fun getObservationChanges(uid: String?, speciesId: String?, queryByDesc : Boolean? = true): Flow<Unit>
    fun getObservationPager(uid: String?, speciesId: String?, queryByDesc : Boolean? = true): Flow<PagingData<Observation>>
    suspend fun getObservationsStateForSpeciesList(
        species: List<DisplayableSpecies>,
        uid : String
    ) : MutableMap<String, Timestamp>
    fun listenToObservationChanges(speciesId: String, uid: String?, queryByDesc: Boolean?=true): Flow<List<ObservationChange>>
    fun getAllObservationsAsList(speciesId: String, uid: String?): Flow<List<Observation>>
    suspend fun postComment(
        observationId: String,
        comment: Comment,
    ): Result<Comment>

    suspend fun toggleLikeObservation(observationId: String, userId: String)
    suspend fun toggleDislikeObservation(observationId: String, userId: String)

    suspend fun deleteComment(observationId: String, commentId: String): Result<Unit>
    suspend fun toggleSaveObservation(observationId: String, userId: String): Result<Unit>

    //detail-view
    fun observeObservationWithComments(observationId: String, sortDescending: Boolean = true): Flow<Pair<Observation?, List<Comment>>>
    fun observeObservationWithoutComments(observationId: String): Flow<Observation?>
    suspend fun deleteObservation(observationId: String)

    fun observeObservation(observationId: String): Flow<Observation?>
    fun observeCommentUpdates(observationId: String): Flow<ListUpdate<Comment>>

    suspend fun toggleDislikeComment(observationId: String, commentId: String, userId: String)
    suspend fun toggleLikeComment(observationId: String, commentId: String, userId: String)
}