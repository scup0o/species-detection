package com.project.speciesdetection.core.services.backend.message

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApiService {
    @POST("api/v1/notifications/send")
    suspend fun sendNotificationTrigger(@Body body: NotificationTriggerRequest)

    @PUT("api/v1/notifications/read")
    suspend fun markNotificationAsRead(
        @Query("notificationId") notiId: String,
        @Query("userId") userId:String
    )

    @PUT("api/v1/notifications/update-state")
    suspend fun updateNotificationState(
        @Query("postId") postId: String?=null,
        @Query("commentId") commentId: String?=null,
        @Query("state") state:String
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
