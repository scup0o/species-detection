package com.project.speciesdetection.domain.provider.camera

import android.content.Context
import android.util.Size
import android.view.Surface
import androidx.camera.lifecycle.ProcessCameraProvider
import com.project.speciesdetection.domain.model.camera.CameraState
import com.project.speciesdetection.domain.model.camera.ImageCaptureResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CameraProvider {
    val cameraState: StateFlow<CameraState>
    val imageCaptureResult: SharedFlow<ImageCaptureResult>
    val optimalPreviewSize: StateFlow<Size?>


    fun openCamera(lensFacing: Int, surface: Surface, screenRotationValue: Int, viewWidth: Int, viewHeight: Int)
    fun captureImage()
    fun setFlashEnabled(enabled: Boolean)
    fun updateScreenRotation(newScreenRotationValue: Int)
    fun releaseCamera()
    fun setZoom(zoomRatio: Float)
}