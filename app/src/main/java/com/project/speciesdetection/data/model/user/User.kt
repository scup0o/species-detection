package com.project.speciesdetection.data.model.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class User(
    val uid: String = "",
    val name: String? = null,
    val email: String? = null,
    @get:PropertyName("photoUrl") @set:PropertyName("photoUrl")
    var photoUrl: String? = null,
    @ServerTimestamp
    val dayCreated: Timestamp? = null,
    val source: String = "", // "google.com" hoặc "password"
    val fcmTokens: List<String> = emptyList()
) {
}