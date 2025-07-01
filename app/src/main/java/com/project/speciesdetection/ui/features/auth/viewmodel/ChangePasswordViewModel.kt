package com.project.speciesdetection.ui.features.auth.viewmodel // Hoặc package khác

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.data.model.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

sealed class ChangePasswordUiEvent {
    data class ShowToast(val message: String) : ChangePasswordUiEvent()
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChangePasswordUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _uiState.update { it.copy(error = "empty") }
            return
        }
        if (newPass.length < 6) {
            _uiState.update { it.copy(error = "invalid") }
            return
        }
        if (newPass != confirmPass) {
            _uiState.update { it.copy(error = "mismatch") }
            return
        }
        if (newPass == currentPass) {
            _uiState.update { it.copy(error = "same") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val reauthResult = repository.reauthenticateUser(currentPass)

            if (reauthResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = "incorrect") }
                return@launch
            }

            val updateResult = repository.updatePassword(newPass)
            if (updateResult.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _uiEvent.emit(ChangePasswordUiEvent.ShowToast("success"))
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = updateResult.exceptionOrNull()?.message ?: "unknown"
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = ChangePasswordUiState()
    }
}