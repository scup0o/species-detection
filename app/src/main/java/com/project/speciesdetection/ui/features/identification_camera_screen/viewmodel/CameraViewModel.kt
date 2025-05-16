package com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel // Hoặc package của bạn

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.provider.camera.CameraProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

enum class FlashModeState { ON, OFF, AUTO } // AUTO có thể không dùng, nhưng để đó

data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: FlashModeState = FlashModeState.OFF,
    val cameraProvider: ProcessCameraProvider? = null,
    val isCameraReady: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @Named("camera_provider") private val cameraProviderSource: CameraProvider,
    @ApplicationContext private val applicationContext: Context
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
                _uiState.value = _uiState.value.copy(
                    cameraProvider = provider,
                    isCameraReady = true
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to get CameraProvider", e)
                setCameraError("Camera initialization failed.")
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
            val currentFlashModeOn = it.cameraInfo.torchState.value == TorchState.ON
            val newFlashState = if (currentFlashModeOn) FlashModeState.OFF else FlashModeState.ON
            try {
                it.cameraControl.enableTorch(!currentFlashModeOn)
                _uiState.value = _uiState.value.copy(flashMode = newFlashState)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Toggle flash failed", e)
                setCameraError("Failed to toggle flash.")
            }
        } ?: run {
            setCameraError("Camera not available to toggle flash.")
        }
    }

    fun flipCamera() {
        val newLensFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(
            lensFacing = newLensFacing,
            flashMode = FlashModeState.OFF
        )
    }

    suspend fun takePhoto(
        imageCapture: ImageCapture, // Nhận ImageCapture instance
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val photoFile = createPhotoFile(applicationContext)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Không thể đọc trực tiếp crop AR từ imageCapture ở đây với API mới
        Log.i("CameraViewModel", "Attempting to take photo. ImageCapture received. Crop will be handled by ViewPort in UseCaseGroup.")

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    // Log kích thước ảnh ở đây vẫn rất quan trọng
                    try {
                        applicationContext.contentResolver.openFileDescriptor(savedUri, "r")?.use { pfd ->
                            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, options)
                            Log.d("CameraViewModel", "Photo saved to: $savedUri")
                            Log.i("CameraViewModel", "SAVED IMAGE Actual Dimensions: ${options.outWidth}x${options.outHeight}, AspectRatio: ${if (options.outHeight != 0) options.outWidth.toFloat() / options.outHeight.toFloat() else "N/A"}")
                        } ?: Log.e("CameraViewModel", "Could not open ParcelFileDescriptor for $savedUri")
                    } catch (e: Exception) {
                        Log.e("CameraViewModel", "Error getting saved image dimensions", e)
                    }
                    onImageCaptured(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Photo capture error: ${exception.message}, Code: ${exception.imageCaptureError}", exception)
                    onError(exception)
                }
            }
        )
    }

    private fun createPhotoFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = context.cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    fun setCameraError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(error = errorMessage)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}