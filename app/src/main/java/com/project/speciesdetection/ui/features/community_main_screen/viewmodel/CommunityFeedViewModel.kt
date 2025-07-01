package com.project.speciesdetection.ui.features.community_main_screen.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.data.model.notification.repository.NotificationRepository
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val remoteUserRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
    private val notificationRepository: NotificationRepository
) : ViewModel(){
    sealed class UiState(){
        object Loading : UiState()
        data class Success(val postList: List<Observation>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _notificationState = MutableStateFlow(false)
    val notificationState = _notificationState.asStateFlow()

    fun checkUserNotificationState(uid : String){
        if (uid.isNotEmpty()){
            viewModelScope.launch(Dispatchers.IO){
                notificationRepository.listenToNotificationState(uid).collect{
                        state -> _notificationState.value=state
                }
            }
            //Log.i("a",_notificationState.value.toString())
        }
    }
}