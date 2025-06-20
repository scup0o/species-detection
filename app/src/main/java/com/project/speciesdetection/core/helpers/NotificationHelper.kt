package com.project.speciesdetection.core.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationCompat
import com.project.speciesdetection.R
import com.project.speciesdetection.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "social_app_channel_id"
        private const val CHANNEL_NAME = "Thông báo Mạng xã hội"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo về lượt thích, bình luận và các tương tác khác."
                // Bạn có thể cấu hình thêm đèn, rung,... tại đây
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, body: String, postId: String?) {
        val pendingIntent = createDeepLinkIntent(postId)

        var newTitle = ""
        var newBody =""
        when (title){
            "lock" -> {
                newTitle = context.getString(R.string.noti_title_lock)
                newBody = context.getString(R.string.noti_body_lock)
            }
            "warning" ->{
                newTitle = context.getString(R.string.noti_title_warning)
                newBody = context.getString(R.string.noti_body_warning) + " '${body}'"
            }
            "like_observation" -> {
                val parts = body.split(" / ")
                newTitle = context.getString(R.string.noti_title_like_observation)
                newBody =  parts[0] +" " + context.getString(R.string.noti_body_like_observation) + " '${parts[1]}'"
            }
            "dislike_observation" -> {
                val parts = body.split(" / ")
                newTitle = context.getString(R.string.noti_title_dislike_observation)
                newBody =  parts[0] +" "+ context.getString(R.string.noti_body_dislike_observation) + " '${parts[1]}'"
            }
            "comment" ->{
                val parts = body.split(" / ")
                newTitle = context.getString(R.string.noti_title_comment)
                newBody =  parts[0] +" "+ context.getString(R.string.noti_body_comment) + " '${parts[1]}' " + context.getString(R.string.noti_body_to_your_post)
            }
            "like_comment" ->{
                val parts = body.split(" / ")
                newTitle = context.getString(R.string.noti_title_like_comment)
                newBody =  parts[0] +" "+ context.getString(R.string.noti_body_like_comment) + " '${parts[1]}'"
            }
            "dislike_comment" ->{
                val parts = body.split(" / ")
                newTitle = context.getString(R.string.noti_title_dislike_comment)
                newBody =  parts[0] +" "+ context.getString(R.string.noti_body_dislike_comment) + " '${parts[1]}'"
            }
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(newBody)


        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // *** THAY BẰNG ICON CỦA BẠN ***
            .setContentTitle(newTitle)
            .setContentText(newBody)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Tự động xóa notification khi người dùng nhấn vào

        // Sử dụng một ID duy nhất (dựa trên thời gian) để các thông báo không ghi đè lên nhau
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createDeepLinkIntent(postId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Đính kèm dữ liệu để MainActivity có thể xử lý deep link
            putExtra("notification_post_id", postId)
        }

        // Dùng request code duy nhất và FLAG_IMMUTABLE
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}