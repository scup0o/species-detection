package com.project.speciesdetection.data.model.notification.repository

import androidx.paging.Pager
import androidx.paging.PagingData
import com.google.firebase.firestore.Query
import com.project.speciesdetection.data.model.notification.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun listenToNotificationState(userId : String): Flow<Boolean>
    fun getNotificationsFlow(userId: String, sortByDesc: Boolean=true): Flow<PagingData<Notification>>
}