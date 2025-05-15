package com.project.speciesdetection.domain.provider.camera

import androidx.camera.lifecycle.ProcessCameraProvider

interface CameraProvider {
    suspend fun get(): ProcessCameraProvider
}