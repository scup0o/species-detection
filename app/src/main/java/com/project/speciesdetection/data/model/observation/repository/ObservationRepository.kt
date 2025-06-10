package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.flow.Flow

sealed class ObservationChange {
    data class Modified(val observation: Observation) : ObservationChange()
    data class Added(val observation: Observation) : ObservationChange()
    data class Removed(val observationId: String) : ObservationChange()
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
        locationTempName : String
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
        locationTempName : String
    ): Result<Unit>

    fun getObservationChangesForUser(userId: String): Flow<Unit>
    suspend fun checkUserObservationState(uid: String, speciesId: String, onDataChanged: (Timestamp?) -> Unit): ListenerRegistration
    fun getObservationChanges(uid: String?, speciesId: String?, queryByDesc : Boolean? = true): Flow<Unit>
    fun getObservationPager(uid: String?, speciesId: String?, queryByDesc : Boolean? = true): Flow<PagingData<Observation>>
    suspend fun getObservationsStateForSpeciesList(
        species: List<DisplayableSpecies>,
        uid : String
    ) : MutableMap<String, Timestamp>
    fun listenToObservationChanges(speciesId: String, uid: String?, queryByDesc: Boolean?=true): Flow<List<ObservationChange>>
    fun getAllObservationsAsList(speciesId: String, uid: String?): Flow<List<Observation>>
}