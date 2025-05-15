package com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.provider.camera.CameraProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext // Inject ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

enum class FlashModeState { ON, OFF, AUTO }

data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: FlashModeState = FlashModeState.OFF,
    val isRecording: Boolean = false,
    val cameraProvider: ProcessCameraProvider? = null, // Thêm cameraProvider vào state
    val isCameraReady: Boolean = false // Để biết khi nào camera sẵn sàng bind
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @Named("camera_provider") private val cameraProviderSource : CameraProvider, // Inject interface
    @ApplicationContext private val applicationContext: Context // Inject ApplicationContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()

    private val _galleryImageUri = MutableStateFlow<Uri?>(null)
    val galleryImageUri: StateFlow<Uri?> = _galleryImageUri.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val provider = cameraProviderSource.get()
                _uiState.value = _uiState.value.copy(cameraProvider = provider, isCameraReady = true)
            } catch (e: Exception) {
                // Xử lý lỗi không lấy được camera provider
                // Ví dụ: set một state lỗi để UI hiển thị
                _uiState.value = _uiState.value.copy(isCameraReady = false) // Hoặc một trạng thái lỗi cụ thể
                Log.e("CameraViewModel", "Failed to get CameraProvider", e)
            }
        }
    }


    fun onPhotoCaptured(uri: Uri) {
        _capturedImageUri.value = uri
    }

    fun onGalleryImageSelected(uri: Uri) {
        _galleryImageUri.value = uri
    }

    fun clearProcessedImageUris() {
        _capturedImageUri.value = null
        _galleryImageUri.value = null
    }

    fun toggleFlash(camera: Camera?) {
        camera?.let {
            val currentFlashMode = it.cameraInfo.torchState.value == TorchState.ON
            val newFlashState = if (currentFlashMode) FlashModeState.OFF else FlashModeState.ON
            try {
                it.cameraControl.enableTorch(!currentFlashMode)
                _uiState.value = _uiState.value.copy(flashMode = newFlashState)
            } catch (e: CameraInfoUnavailableException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun flipCamera() {
        val newLensFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(lensFacing = newLensFacing, flashMode = FlashModeState.OFF)
    }

    suspend fun takePhoto(
        // context không cần truyền vào nữa vì ViewModel đã có ApplicationContext
        imageCapture: ImageCapture,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val photoFile = createPhotoFile(applicationContext) // Sử dụng applicationContext
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(applicationContext), // Sử dụng applicationContext
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    onImageCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    private fun createPhotoFile(context: Context): File { // Giữ nguyên context ở đây cho rõ ràng
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = context.cacheDir
        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        )
    }
}