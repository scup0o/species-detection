package com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel // Hoặc package của bạn

import android.app.Application
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.provider.camera.CameraHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class FlashModeState { ON, OFF, AUTO } // AUTO có thể không dùng, nhưng để đó

sealed class CameraState {
    object Idle : CameraState()
    object Opening : CameraState()
    data class Previewing(val previewSize: Size) : CameraState()
    object Capturing : CameraState()
    data class Error(val message: String, val cause: Throwable? = null) : CameraState()
    object Closed : CameraState() // Trạng thái khi camera đã được đóng hoàn toàn
}

sealed class ImageCaptureResult {
    data class Success(val uri: Uri) : ImageCaptureResult()
    data class Error(val message: String, val cause: Throwable? = null) : ImageCaptureResult()
}

sealed class CameraNavigationEffect {
    data class NavigateToEditScreen(val imageUri: Uri) : CameraNavigationEffect()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application
    // Bạn có thể inject Camera2Helper nếu muốn, hoặc tạo trực tiếp ở đây
    // private val cameraHelper: Camera2Helper // Nếu inject
) : AndroidViewModel(application) {

    // Tạo Camera2Helper trực tiếp, truyền context và viewModelScope
    // Hoặc inject nó nếu bạn thiết lập Hilt Module
    private val cameraHelper = CameraHelper(application.applicationContext, viewModelScope)

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
                    // Có thể log thêm hoặc xử lý lỗi cụ thể ở đây
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

    fun onCameraPermissionGranted(surface: Surface, screenRotation: Int, viewWidth: Int, viewHeight: Int) {
        if (_uiState.value.cameraState == CameraState.Idle || _uiState.value.cameraState is CameraState.Closed || _uiState.value.cameraState is CameraState.Error) {
            cameraHelper.openCamera(
                lensFacing = _uiState.value.lensFacing,
                surface = surface,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                screenRotationValue = screenRotation
            )
        } else {
            Log.w("CameraViewModel", "Camera not in idle/closed/error state for opening: ${_uiState.value.cameraState}")
        }
    }

    fun onSurfaceReady(surface: Surface, screenRotation: Int, viewWidth: Int, viewHeight: Int) {
        // Hàm này có thể được gọi khi surface đã sẵn sàng và permission đã được cấp
        // Hoặc khi lens facing thay đổi và cần mở lại camera với surface mới (nếu TextureView được tái tạo)
        Log.d("CameraViewModel", "Surface ready. Current state: ${uiState.value.cameraState}")
        if (uiState.value.cameraState == CameraState.Idle || uiState.value.cameraState is CameraState.Closed || uiState.value.cameraState is CameraState.Error) {
            cameraHelper.openCamera(
                lensFacing = _uiState.value.lensFacing,
                surface = surface,
                screenRotationValue = screenRotation,
                viewWidth = viewWidth,
                viewHeight = viewHeight
            )
        } else if (uiState.value.cameraState is CameraState.Previewing && (uiState.value.optimalPreviewSize == null || uiState.value.optimalPreviewSize?.width != viewWidth || uiState.value.optimalPreviewSize?.height != viewHeight)) {
            // Nếu đang preview nhưng kích thước view thay đổi (ví dụ xoay màn hình và view được tái tạo)
            // hoặc previewSurface của helper cần được cập nhật
            Log.d("CameraViewModel", "Surface changed or needs re-initialization while previewing. Re-opening camera.")
            cameraHelper.releaseCamera() // Đóng camera cũ trước
            cameraHelper.openCamera( // Mở lại với thông số mới
                lensFacing = _uiState.value.lensFacing,
                surface = surface,
                screenRotationValue = screenRotation,
                viewWidth = viewWidth,
                viewHeight = viewHeight
            )
        }
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
        // Quan trọng: phải đóng camera cũ trước khi mở camera mới
        cameraHelper.releaseCamera() // Đảm bảo tài nguyên cũ được giải phóng
        // Sau khi release, camera state sẽ chuyển sang Closed hoặc Idle
        // onSurfaceReady hoặc một cơ chế tương tự sẽ được gọi lại từ Composable để mở camera mới
        // Hoặc gọi trực tiếp ở đây nếu đảm bảo surface vẫn valid.
        // Để an toàn, ta có thể chờ Composable cung cấp lại surface nếu nó bị hủy và tạo lại.
        // Tuy nhiên, nếu surface không bị hủy, có thể mở lại ngay.
        // Tạm thời gọi openCamera trực tiếp, nhưng cần kiểm tra kỹ luồng tái tạo surface.
        cameraHelper.openCamera(
            lensFacing = newLensFacing,
            surface = surface,
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
        // Nếu đang preview, có thể cần cấu hình lại transform matrix của TextureView (nếu bạn tự quản lý)
        // Hoặc nếu optimalPreviewSize thay đổi đáng kể, có thể cần mở lại camera.
        // Hiện tại, Camera2Helper đã xử lý JPEG orientation dựa trên screenRotation.
    }

    override fun onCleared() {
        super.onCleared()
        cameraHelper.releaseCamera() // Quan trọng: giải phóng tài nguyên camera
        Log.d("CameraViewModel", "ViewModel cleared, camera released.")
    }
}

data class CameraUiState(
    val isFlashOn: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val cameraState: CameraState = CameraState.Idle,
    val isCameraReady: Boolean = false,
    val errorMessage: String? = null,
    val optimalPreviewSize: Size? = null
)