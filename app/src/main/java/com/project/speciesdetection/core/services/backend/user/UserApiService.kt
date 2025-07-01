package com.project.speciesdetection.core.services.backend.user

import com.project.speciesdetection.core.services.backend.message.NotificationTriggerRequest
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApiService {
    @Serializable
    data class UpdateRequest(
        val userId: String,
        val updates: UserUpdates
    )

    @Serializable
    data class UserUpdates(
        val name: String?,
        val photoUrl: String?
    )

    @POST("api/v1/notifications/propagate-update")
    suspend fun updateUserDenormalize(
        @Body request: UpdateRequest
    )


}