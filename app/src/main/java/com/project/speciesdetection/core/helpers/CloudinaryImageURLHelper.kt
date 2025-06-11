package com.project.speciesdetection.core.helpers

import com.project.speciesdetection.core.services.remote_database.CloudinaryConstants
import javax.inject.Inject

object CloudinaryImageURLHelper{
    fun getBaseImageURL(image : String): String {
        return CloudinaryConstants.BASE_URL+CloudinaryConstants.VERSION+image
    }

    fun getSquareImageURL(image : String): String {
        return CloudinaryConstants.BASE_URL+CloudinaryConstants.TRANSFORMATION_SQUARE+CloudinaryConstants.VERSION+image
    }

    fun resizeCloudinaryUrl(originalUrl: String, transformation: String): String {
        val uploadIndex = originalUrl.indexOf("/upload/")
        return if (uploadIndex != -1) {
            val prefix = originalUrl.substring(0, uploadIndex + "/upload/".length)
            val suffix = originalUrl.substring(uploadIndex + "/upload/".length)
            "$prefix$transformation/$suffix"
        } else {
            originalUrl // Trả về nguyên gốc nếu không tìm thấy /upload/
        }
    }

    fun restoreCloudinaryOriginalUrl(url: String): String {
        val uploadIndex = url.indexOf("/upload/")
        if (uploadIndex == -1) return url

        val prefix = url.substring(0, uploadIndex + "/upload/".length)
        val suffix = url.substring(uploadIndex + "/upload/".length)

        // Tách phần transform (nếu có)
        val parts = suffix.split("/")
        val firstPart = parts.firstOrNull()

        // Kiểm tra nếu phần đầu là transform thì bỏ nó đi
        val hasTransform = firstPart?.contains("w_") == true || firstPart?.contains("c_") == true || firstPart?.contains("q_") == true
        val newSuffix = if (hasTransform) parts.drop(1).joinToString("/") else suffix

        var resultUrl = "$prefix$newSuffix"

        // Nếu là thumbnail video (.jpg) và là từ /video/, đổi .jpg về .mp4
        if (resultUrl.contains("/video/") && resultUrl.endsWith(".jpg")) {
            resultUrl = resultUrl.replace(".jpg", ".mp4")
        }

        return resultUrl
    }

}