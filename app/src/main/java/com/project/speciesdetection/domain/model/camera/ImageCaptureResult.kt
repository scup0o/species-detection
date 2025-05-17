package com.project.speciesdetection.domain.model.camera

import android.net.Uri

sealed class ImageCaptureResult {
    data class Success(val uri: Uri) : ImageCaptureResult()
    data class Error(val message: String, val cause: Throwable? = null) : ImageCaptureResult()
}