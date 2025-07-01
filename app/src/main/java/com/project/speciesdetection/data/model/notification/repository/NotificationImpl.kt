package com.project.speciesdetection.data.model.notification.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.speciesdetection.data.model.notification.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class NotificationImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository{
    override fun getNotificationsFlow(userId: String, sortByDesc: Boolean): Flow<PagingData<Notification>> {

        val sortDirection = if (sortByDesc) Query.Direction.DESCENDING else Query.Direction.ASCENDING

        val query = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("state", "show")
            .orderBy("dateCreated", sortDirection)

        return Pager(
            config = PagingConfig(
                pageSize = 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { NotificationPagingSource(query) }
        ).flow
    }

    override fun listenToNotificationState(userId : String): Flow<Boolean> = callbackFlow{
        val query = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("state","show")
            .whereEqualTo("isRead",false)
            .limit(1)
        val listener = query.addSnapshotListener{ snapshot, error ->
            if (error!=null){
                close(error)
                return@addSnapshotListener
            }

            if (snapshot!=null){
                val hasUnread = !snapshot.isEmpty
                trySend(hasUnread).isSuccess
            }
        }
        awaitClose {
            listener.remove()
        }
    }
}