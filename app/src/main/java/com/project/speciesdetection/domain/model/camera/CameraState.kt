package com.project.speciesdetection.domain.model.camera

import android.util.Size

sealed class CameraState {
    object Idle : CameraState()
    object Opening : CameraState()
    data class Previewing(val previewSize: Size) : CameraState()
    object Capturing : CameraState()
    data class Error(val message: String, val cause: Throwable? = null) : CameraState()
    object Closed : CameraState() // Trạng thái khi camera đã được đóng hoàn toàn
}