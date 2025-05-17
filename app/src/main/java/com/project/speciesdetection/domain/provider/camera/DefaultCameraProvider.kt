package com.project.speciesdetection.domain.provider.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.project.speciesdetection.domain.model.camera.CameraState
import com.project.speciesdetection.domain.model.camera.ImageCaptureResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton // ProcessCameraProvider is a singleton, so this source can be too.
class DefaultCameraProvider @Inject constructor(
    private val camera2Source: Camera2Source

) : CameraProvider {
    override val cameraState: StateFlow<CameraState>
        get() = camera2Source.cameraState

    override val imageCaptureResult: SharedFlow<ImageCaptureResult>
        get() = camera2Source.imageCaptureResult

    override val optimalPreviewSize: StateFlow<Size?>
        get() = camera2Source.optimalPreviewSize

    override fun openCamera(lensFacing: Int, surface: Surface, screenRotationValue: Int, viewWidth: Int, viewHeight: Int) {
        camera2Source.openCamera(lensFacing, surface, screenRotationValue, viewWidth, viewHeight)
    }

    override fun captureImage() {
        camera2Source.captureImage()
    }

    override fun setFlashEnabled(enabled: Boolean) {
        camera2Source.setFlashEnabled(enabled)
    }

    override fun updateScreenRotation(newScreenRotationValue: Int) {
        camera2Source.updateScreenRotation(newScreenRotationValue)
    }

    override fun releaseCamera() {
        camera2Source.releaseCamera()
    }

    override fun setZoom(zoomRatio: Float){
        camera2Source.setZoom(zoomRatio)
    }
}