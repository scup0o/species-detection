package com.project.speciesdetection.core.services.backend.message

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MessageApiService {
    @POST("api/v1/notifications/send")
    suspend fun sendNotificationTrigger(@Body body: NotificationTriggerRequest)

    @PUT("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String,
        @Body body: MarkAsReadRequest
    )
}

@Serializable
data class NotificationTriggerRequest(
    val actorUserId: String,
    val actorUsername : String,
    val targetUserId: String,
    val postId: String,
    val commentId : String,
    val content : String,
    val actionType: String // ví dụ: "like", "comment"
)

data class MarkAsReadRequest(val userId: String)