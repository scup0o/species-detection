package com.project.speciesdetection.ui.features.community_main_screen.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val SAVED_SEARCH_QUERY_KEY = "search_query"

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val remoteUserRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(){
    sealed class UiState(){
        object Loading : UiState()
        data class Success(val postList: List<Observation>) : UiState()
        data class Error(val message: String) : UiState()
    }

    /*private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<Boolean>(SAVED_SEARCH_QUERY_KEY) ?: false
    )*/

    private val _searchQuery = MutableStateFlow(false)

    // 2. Expose nó ra bên ngoài dưới dạng StateFlow (read-only)
    val searchQuery: StateFlow<Boolean> = _searchQuery.asStateFlow()

    // Phương thức để cập nhật giá trị (chỉ có thể gọi từ bên trong ViewModel)
    fun updateSearchQuery(query: Boolean) {
        _searchQuery.value = query // Cập nhật MutableStateFlow
        //savedStateHandle[SAVED_SEARCH_QUERY_KEY] = query // Lưu vào SavedStateHandle
    }

}