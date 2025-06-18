package com.project.speciesdetection.data.model.observation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import com.project.speciesdetection.core.helpers.GeoPointSerializer
import com.project.speciesdetection.core.helpers.TimestampSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Observation(
    @DocumentId
    var id: String? = null, // ID sẽ được Firestore tự gán hoặc ta tự gán khi update

    // User Info
    val uid: String = "",
    val userName: String = "",
    val userImage: String = "",

    // Species Info
    var speciesId: String = "",
    var speciesName: String = "",
    var speciesScientificName: String = "",

    val point: Int = 0,
    val commentCount: Int = 0,
    val content: String = "",
    val imageURL: List<String> = emptyList(),
    val privacy: String = "Public", // "Public" hoặc "Private"
    @Serializable(with = GeoPointSerializer::class)
    val location: GeoPoint? = null,
    val locationTempName : String ="",
    val locationName : String ="",
    val locationDisplayName : String ="",
    val likeUserIds : List<String> = emptyList(),
    val dislikeUserIds : List<String> = emptyList(),

    val state: String = "normal",

    @ServerTimestamp
    @Serializable(with = TimestampSerializer::class)
    val dateCreated: Timestamp? = null,
    @ServerTimestamp
    @Serializable(with = TimestampSerializer::class)
    val dateUpdated: Timestamp? = null,
    @Serializable(with = TimestampSerializer::class)
    val dateFound: Timestamp? = null
)