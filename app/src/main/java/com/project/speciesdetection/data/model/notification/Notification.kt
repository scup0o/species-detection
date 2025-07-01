package com.project.speciesdetection.data.model.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId
    val id : String = "",
    val recipientId: String ="",
    val senderId: String ="",
    val senderUsername: String ="",
    val senderImage: String? ="",
    val content: String = "",
    val type: String ="",
    val postId: String? ="",
    val commentId : String? ="",
    var isRead:Boolean=false,
    val dateCreated: Timestamp? = null,
    val count: Int? = 0,
) {
}