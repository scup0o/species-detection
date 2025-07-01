package com.project.speciesdetection.ui.features.community_main_screen.viewmodel.notification

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.project.speciesdetection.core.services.backend.message.MessageApiService
import com.project.speciesdetection.data.model.notification.Notification
import com.project.speciesdetection.data.model.notification.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    data class MoveToPost(val observationId: String) : UiEvent()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val messageApiService: MessageApiService,
) : ViewModel() {

    private val _currentUserId = MutableStateFlow("")
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _sortByDesc = MutableStateFlow(true)
    val sortByDesc = _sortByDesc.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notificationPaging: Flow<PagingData<Notification>> =
        combine(_currentUserId, _sortByDesc) { userId, sortDirection ->
            Pair(userId, sortDirection)
        }
            .filterNotNull()
            .flatMapLatest { (userId, sortDirection) ->
                notificationRepository.getNotificationsFlow(userId, sortDirection)
            }
            .cachedIn(viewModelScope)

    fun updateUserId(uid: String) {
        if (uid.isNotEmpty()) _currentUserId.value = uid
    }

    fun updateSortDirection() {
        _sortByDesc.value = !_sortByDesc.value
    }

    fun markNotificationAsRead(notification: Notification, uid: String) {
        viewModelScope.launch {
            try {
                messageApiService.markNotificationAsRead(notification.id, uid)
                _eventFlow.emit(UiEvent.MoveToPost(notification.postId ?: ""))
            } catch (e: Exception) {
                Log.i("e", e.message.toString())

            }
        }
    }


}