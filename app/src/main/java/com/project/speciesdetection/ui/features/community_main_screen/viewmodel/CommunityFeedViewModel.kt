package com.project.speciesdetection.ui.features.community_main_screen.viewmodel

import androidx.lifecycle.ViewModel
import com.project.speciesdetection.data.model.post.Post
import com.project.speciesdetection.data.model.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val remoteUserRepository: UserRepository,
) : ViewModel(){
    sealed class UiState(){
        object Loading : UiState()
        data class Success(val postList: List<Post>) : UiState()
        data class Error(val message: String) : UiState()
    }
}