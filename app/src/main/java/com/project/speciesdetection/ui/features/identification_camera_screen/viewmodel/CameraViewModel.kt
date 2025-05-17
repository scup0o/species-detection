package com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel // Hoặc package của bạn

import android.app.Application
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.model.camera.ImageCaptureResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.project.speciesdetection.domain.model.camera.CameraState
import com.project.speciesdetection.domain.provider.camera.CameraProvider
import javax.inject.Named

enum class FlashModeState { ON, OFF, AUTO } // AUTO có thể không dùng, nhưng để đó

sealed class CameraNavigationEffect {
    data class NavigateToEditScreen(val imageUri: Uri) : CameraNavigationEffect()
}


data class CameraUiState(
    val isFlashOn: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val cameraState: CameraState = CameraState.Idle,
    val isCameraReady: Boolean = false,
    val errorMessage: String? = null,
    val optimalPreviewSize: Size? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    @Named("camera_provider") private val cameraHelper : CameraProvider
) : AndroidViewModel(application) {

    // Tạo Camera2Helper trực tiếp, truyền context và viewModelScope
    // Hoặc inject nó nếu bạn thiết lập Hilt Module
    //private val cameraHelper = CameraProvider

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEffect = MutableSharedFlow<CameraNavigationEffect>()
    val navigationEffect = _navigationEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            cameraHelper.cameraState.collect { state ->
                _uiState.update {
                    it.copy(
                        cameraState = state,
                        isCameraReady = state is CameraState.Previewing,
                        errorMessage = if (state is CameraState.Error) state.message else null
                    )
                }
                if (state is CameraState.Error) {
                    Log.e("CameraViewModel", "Camera Error: ${state.message}", state.cause)
                }
            }
        }
        viewModelScope.launch {
            cameraHelper.imageCaptureResult.collect { result ->
                when (result) {
                    is ImageCaptureResult.Success -> {
                        _navigationEffect.emit(CameraNavigationEffect.NavigateToEditScreen(result.uri))
                    }
                    is ImageCaptureResult.Error -> {
                        _uiState.update { it.copy(errorMessage = result.message) }
                        Log.e("CameraViewModel", "Image Capture Error: ${result.message}", result.cause)
                    }
                }
            }
        }
        viewModelScope.launch {
            cameraHelper.optimalPreviewSize.collect { size ->
                _uiState.update { it.copy(optimalPreviewSize = size) }
            }
        }
    }

    // onCameraPermissionGranted is not strictly needed if onSurfaceReady handles all cases
    // fun onCameraPermissionGranted(...)

    fun onSurfaceReady(surface: Surface, screenRotation: Int, viewWidth: Int, viewHeight: Int) {
        Log.d("CameraViewModel", "Surface ready. Current state: ${uiState.value.cameraState}")
        // Always (re)open camera when surface is ready to ensure correct state,
        // especially after rotation or app coming to foreground.
        // Camera2Source's openCamera will handle if it's already open or in a non-openable state.
        // Release first if it was somehow open with a different surface or config
        if (uiState.value.cameraState !is CameraState.Idle &&
            uiState.value.cameraState !is CameraState.Closed &&
            uiState.value.cameraState !is CameraState.Error) {
            // If already previewing, but surface details changed, a full reopen is safer
            // Or if somehow stuck in Opening/Capturing
            Log.d("CameraViewModel", "Releasing camera before reopening on surface ready/changed.")
            cameraHelper.releaseCamera() // This will set state to Closed
        }

        // Now open with new/current surface
        cameraHelper.openCamera(
            lensFacing = _uiState.value.lensFacing,
            surface = surface,
            screenRotationValue = screenRotation,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )
    }


    fun toggleFlash() {
        val newFlashState = !_uiState.value.isFlashOn
        _uiState.update { it.copy(isFlashOn = newFlashState) }
        cameraHelper.setFlashEnabled(newFlashState)
    }

    fun flipCamera(surface: Surface, screenRotation: Int, viewWidth: Int, viewHeight: Int) {
        val newLensFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update { it.copy(lensFacing = newLensFacing) }
        cameraHelper.releaseCamera() // Release existing camera
        // The openCamera will be called again via onSurfaceReady or similar mechanism if surface is re-created.
        // Or, call it directly here if surface is guaranteed to be valid.
        // For simplicity and robustness for now, let onSurfaceReady (triggered by TextureView) handle it.
        // If TextureView isn't re-created, we might need to call openCamera here.
        // Let's try calling openCamera directly to ensure it reopens.
        cameraHelper.openCamera(
            lensFacing = newLensFacing,
            surface = surface, // This surface might be stale if TextureView is recreated.
            // Better to rely on onSurfaceAvailable for the new surface.
            // The current logic in CameraScreen where onSurfaceAvailable calls
            // viewModel.onSurfaceReady is good.
            screenRotationValue = screenRotation,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )
    }

    fun takePicture() {
        cameraHelper.captureImage()
    }

    fun onGalleryImageSelected(uri: Uri) {
        viewModelScope.launch {
            _navigationEffect.emit(CameraNavigationEffect.NavigateToEditScreen(uri))
        }
    }

    fun onScreenRotationChanged(newRotation: Int) {
        cameraHelper.updateScreenRotation(newRotation)
        // The TextureView's onSurfaceTextureSizeChanged or onSurfaceTextureAvailable
        // should trigger viewModel.onSurfaceReady which will re-configure the camera
        // with new view dimensions if necessary.
    }

    // --- ZOOM METHOD ---
    fun setZoomRatio(zoomRatio: Float) {
        if (uiState.value.isCameraReady) { // Only if camera is in a state to accept zoom
            cameraHelper.setZoom(zoomRatio)
        }
    }
    // --- END ZOOM METHOD ---

    override fun onCleared() {
        super.onCleared()
        cameraHelper.releaseCamera()
        Log.d("CameraViewModel", "ViewModel cleared, camera released.")
    }
}
