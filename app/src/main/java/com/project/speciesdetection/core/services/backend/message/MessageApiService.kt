package com.project.speciesdetection.core.services.backend.message

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MessageApiService {
    @POST("send-notification")
    suspend fun sendNotificationTrigger(@Body body: NotificationTriggerRequest)

    @PUT("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String,
        @Body body: MarkAsReadRequest
    )
}

data class NotificationTriggerRequest(
    val actorUserId: String,
    val actorUserName : String,
    val targetUserId: String,
    val postId: String,
    val actionType: String // ví dụ: "like", "comment"
)

data class MarkAsReadRequest(val userId: String)