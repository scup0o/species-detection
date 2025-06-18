package com.project.speciesdetection.core.services.message

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.project.speciesdetection.core.helpers.NotificationHelper
import com.project.speciesdetection.data.model.user.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebaseAuth: com.google.firebase.auth.FirebaseAuth

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: " + remoteMessage.data)

            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            val postId = remoteMessage.data["postId"]

            if (title != null && body != null) {
                notificationHelper.showNotification(title, body, postId)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        // Nếu người dùng đã đăng nhập, tự động cập nhật token mới.
        firebaseAuth.currentUser?.uid?.let { userId ->
            userRepository.addCurrentUserFcmToken(userId, token)
        }
    }
}