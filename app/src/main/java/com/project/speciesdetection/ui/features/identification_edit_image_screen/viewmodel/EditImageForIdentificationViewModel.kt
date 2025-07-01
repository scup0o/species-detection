package com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel

import android.app.Application // Sử dụng Application cho MediaFileUseCase nếu cần
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.usecase.common.MediaFileUseCase // Giả sử bạn có UseCase này
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

data class EditImageUiState(
    val originalImageUri: Uri? = null,
    val currentImageUri: Uri? = null,
    val isLoading: Boolean = true,
    val saveSuccess: Boolean? = null,
    val error: String? = null,
    val showAnalysisPopup: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class EditImageForIdentificationViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val savedStateHandle: SavedStateHandle,
    private val mediaFileUseCase: MediaFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditImageUiState())
    val uiState: StateFlow<EditImageUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "EditImageVM" }

    init {
        val encodedImageUriString = savedStateHandle.get<String>("imageUri")
        //Log.d(TAG, "Received encodedImageUriString: $encodedImageUriString")
        if (encodedImageUriString != null) {
            try {
                val receivedUri = Uri.decode(encodedImageUriString).toUri()
                _uiState.value = EditImageUiState(
                    originalImageUri = receivedUri,
                    currentImageUri = receivedUri,
                    isLoading = false
                )
            } catch (e: Exception) {
                //Log.e(TAG, "Error parsing received URI: $encodedImageUriString", e)
                _uiState.value = EditImageUiState(isLoading = false, error = "Invalid image link.")
            }
        } else {
            //Log.e(TAG, "Image URI is null. This screen needs an image URI.")
            _uiState.value = EditImageUiState(isLoading = false, error = "No image provided.")
        }
    }

    fun onImageCropped(croppedUri: Uri?) {
        if (croppedUri != null) {
            _uiState.value = _uiState.value.copy(currentImageUri = croppedUri, error = null)
            //Log.d(TAG, "Image cropped. New URI: $croppedUri")
        } else {
            //Log.w(TAG, "Image cropping cancelled or failed.")
        }
    }

    fun saveCurrentImageToGallery() {
        val imageUriToSave = _uiState.value.currentImageUri ?: run {
            _uiState.value = _uiState.value.copy(error = "No image to save.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = null, error = null)
        viewModelScope.launch {
            try {
                val savedUri = mediaFileUseCase.saveMediaToGallery(imageUriToSave)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
                //Log.i(TAG, "Image saved via UseCase. Output URI: $savedUri")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = false, error = "Save failed: ${e.localizedMessage}")
                //Log.e(TAG, "Error saving image via UseCase", e)
            }
        }
    }

    fun showAnalysisPopup() {
        if (_uiState.value.currentImageUri != null) {
            _uiState.value = _uiState.value.copy(showAnalysisPopup = true, error = null)
        } else {
            _uiState.value = _uiState.value.copy(error = "No image selected to analyze.")
        }
    }

    fun dismissAnalysisPopup() {
        _uiState.value = _uiState.value.copy(showAnalysisPopup = false)
    }

    fun resetSaveStatus() {
        _uiState.value = _uiState.value.copy(saveSuccess = null) // Chỉ reset saveSuccess
    }

    fun clearGeneralError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "EditImageViewModel cleared.")
    }
}