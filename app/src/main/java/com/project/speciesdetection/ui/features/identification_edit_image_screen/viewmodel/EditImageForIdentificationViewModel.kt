package com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.core.net.toUri

data class EditImageUiState(
    val originalImageUri: Uri? = null,
    val currentImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val saveSuccess: Boolean? = null, // null: chưa lưu, true: thành công, false: thất bại
    val error: String? = null,
    val showAnalysisPopup: Boolean = false, // State để điều khiển popup
    val imageForPopup: Uri? = null          // Uri cho ảnh trong popup
)

@HiltViewModel
class EditImageForIdentificationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditImageUiState())
    val uiState: StateFlow<EditImageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val imageUriString = savedStateHandle.get<String>("imageUri")
            if (imageUriString != null) {
                try {
                    val imageUri = Uri.decode(imageUriString).toUri()
                    _uiState.value = _uiState.value.copy(
                        originalImageUri = imageUri,
                        currentImageUri = imageUri
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = "Invalid image URI format.")
                    Log.e("EditImageViewModel", "Error parsing URI: $imageUriString", e)
                }
            } else {
                _uiState.value = _uiState.value.copy(error = "No image URI provided.")
                Log.e("EditImageViewModel", "No image URI found in SavedStateHandle")
            }
        }
    }

    fun onImageCropped(croppedUri: Uri?) {
        if (croppedUri != null) {
            _uiState.value = _uiState.value.copy(currentImageUri = croppedUri, error = null)
        } else {
            // Crop bị hủy hoặc thất bại
            Log.w("EditImageViewModel", "Image cropping cancelled or failed.")
            // Bạn có thể muốn hiển thị một thông báo cho người dùng ở đây nếu crop thất bại
            // _uiState.value = _uiState.value.copy(error = "Image cropping failed.")
        }
    }

    fun saveCurrentImageToGallery(context: Context) {
        val imageUriToSave = _uiState.value.currentImageUri ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, saveSuccess = null, error = null)

        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val displayName = "EDITED_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                val mimeType = "image/jpeg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SpeciesDetectionApp") // Thư mục lưu
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val imageOutUri = resolver.insert(collection, contentValues)
                if (imageOutUri == null) {
                    throw Exception("Failed to create new MediaStore record.")
                }

                resolver.openOutputStream(imageOutUri).use { outputStream ->
                    if (outputStream == null) {
                        throw Exception("Failed to get output stream.")
                    }
                    val inputStream = resolver.openInputStream(imageUriToSave)
                    if (inputStream == null) {
                        resolver.delete(imageOutUri, null, null) // Dọn dẹp nếu không mở được input stream
                        throw Exception("Failed to get input stream from source URI: $imageUriToSave")
                    }
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageOutUri, contentValues, null, null)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
                Log.i("EditImageViewModel", "Image saved to gallery: $imageOutUri")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = false, error = "Failed to save image: ${e.localizedMessage}")
                Log.e("EditImageViewModel", "Error saving image", e)
            }
        }
    }

    fun showImageInPopup() {
        val imageToShow = _uiState.value.currentImageUri
        if (imageToShow != null) {
            _uiState.value = _uiState.value.copy(
                showAnalysisPopup = true,
                imageForPopup = imageToShow,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(error = "No image to display in popup.")
            Log.w("EditImageViewModel", "No currentImageUri to show in popup.")
        }
    }

    fun dismissAnalysisPopup() {
        _uiState.value = _uiState.value.copy(
            showAnalysisPopup = false
            // Không cần clear imageForPopup ngay, vì nó có thể hữu ích nếu popup được mở lại nhanh chóng
            // Hoặc bạn có thể clear: imageForPopup = null
        )
    }

    fun resetSaveStatus() {
        _uiState.value = _uiState.value.copy(saveSuccess = null, error = null) // Clear cả error khi reset
    }
}