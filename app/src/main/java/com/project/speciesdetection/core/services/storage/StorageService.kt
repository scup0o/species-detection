package com.project.speciesdetection.core.services.storage

import android.net.Uri

interface StorageService {
    suspend fun uploadImage(imageUri: Uri): Result<String>
}