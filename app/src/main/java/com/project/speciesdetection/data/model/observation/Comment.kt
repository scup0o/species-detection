package com.project.speciesdetection.data.model.observation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.project.speciesdetection.core.helpers.TimestampSerializer
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class Comment(
    @DocumentId
    val id: String? = null,
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    var content: String = "",
    val likeUserIds : List<String> = emptyList(),
    val dislikeUserIds : List<String> = emptyList(),
    val speciesId : String ="",
    val imageUrl: String ="",
    val state : String = "normal",
    val likeCount : Int = 0,
    @ServerTimestamp
    @Serializable(with = TimestampSerializer::class)
    val timestamp: Timestamp? = null,
    @ServerTimestamp
    @Serializable(with = TimestampSerializer::class)
    val updated: Timestamp? = null
)