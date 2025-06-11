package com.project.speciesdetection.core.helpers

object MediaHelper {
    fun isImageFile(path: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        return imageExtensions.any { path.endsWith(".$it", ignoreCase = true) }
    }

    fun isVideoFile(path: String): Boolean {
        val videoExtensions = listOf("mp4", "mov", "avi", "mkv", "flv", "wmv", "webm")
        return videoExtensions.any { path.endsWith(".$it", ignoreCase = true) }
    }
}