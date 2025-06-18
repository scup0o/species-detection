package com.project.speciesdetection.domain.usecase.common

// Android Core & Support Libraries
import android.content.Context // Để truy cập cacheDir và packageName
import android.net.Uri // Đại diện cho URI của file ảnh
import android.util.Log

// androidx.core cho FileProvider
import androidx.core.content.FileProvider // Để tạo content URI an toàn cho việc chia sẻ với camera
import com.project.speciesdetection.R

// Dagger Hilt cho Dependency Injection
import dagger.hilt.android.qualifiers.ApplicationContext // Để inject ApplicationContext

// Kotlin Coroutines
import kotlinx.coroutines.Dispatchers // Để chỉ định CoroutineDispatcher cho các tác vụ IO
import kotlinx.coroutines.withContext // Để chuyển đổi context của coroutine

// Java IO cho thao tác file
import java.io.File // Để làm việc với đối tượng File
import java.io.IOException // Để bắt các ngoại lệ liên quan đến IO

// Java Utility cho Date và Formatting
import java.text.SimpleDateFormat // Để định dạng timestamp cho tên file
import java.util.Date // Để lấy thời gian hiện tại
import java.util.Locale // Để định dạng Locale cho SimpleDateFormat

// javax.inject (thường đi kèm với Hilt)
import javax.inject.Inject // Annotation để Hilt biết cách tạo instance của class này
import javax.inject.Singleton

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore

@Singleton
class MediaFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun createImageFileAndUriInCache(): Pair<Uri, File>? {
        return withContext(Dispatchers.IO) {
            try {
                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFileName = "JPEG_CACHE_${timeStamp}_"
                val imageCacheDir = File(context.cacheDir, "images")
                if (!imageCacheDir.exists() && !imageCacheDir.mkdirs()) {
                    throw IOException("Failed to create cache subdirectory for images.")
                }
                val imageFile = File.createTempFile(imageFileName, ".jpg", imageCacheDir)
                //Log.d("CreateImageUseCase", "Created file: ${imageFile.absolutePath}, Exists: ${imageFile.exists()}")
                val imageUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )

                Pair(imageUri, imageFile)
            } catch (ex: IOException) {
                null
            }
        }
    }

    suspend fun saveMediaToGallery(inputUri: Uri): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val appName = try {
            context.getString(R.string.app_name)
        } catch (e: Exception) {
            "SpeciesDetection"
        }

        // Detect MIME type
        var mimeType: String? = null
        var fileExtension = ".bin"
        var inputStream: java.io.InputStream? = null

        if (inputUri.scheme == "http" || inputUri.scheme == "https") {
            // Tải từ URL
            val connection = java.net.URL(inputUri.toString()).openConnection()
            connection.connect()
            mimeType = connection.contentType ?: "application/octet-stream"
            inputStream = connection.getInputStream()
        } else {
            // Local URI
            mimeType = resolver.getType(inputUri)
            inputStream = resolver.openInputStream(inputUri)
        }

        // MIME fallback nếu không xác định
        mimeType = mimeType ?: "application/octet-stream"

        // Xác định phần mở rộng từ MIME
        fileExtension = when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
            mimeType.contains("png") -> ".png"
            mimeType.contains("webp") -> ".webp"
            mimeType.contains("gif") -> ".gif"
            mimeType.contains("mp4") -> ".mp4"
            mimeType.contains("webm") -> ".webm"
            mimeType.contains("3gp") -> ".3gp"
            else -> ".bin"
        }

        val isImage = mimeType.startsWith("image/")
        val isVideo = mimeType.startsWith("video/")

        val displayName = "MEDIA_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}$fileExtension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val basePath = if (isVideo) "Movies" else "Pictures"
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$basePath/$appName")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = when {
            isImage -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            isVideo -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            else -> throw IllegalArgumentException("Không hỗ trợ loại MIME này: $mimeType")
        }

        val outputUri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("Không thể tạo MediaStore entry")

        try {
            resolver.openOutputStream(outputUri).use { outStream ->
                if (outStream == null || inputStream == null)
                    throw IOException("Không thể mở stream để sao chép media.")
                inputStream.copyTo(outStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(outputUri, contentValues, null, null)
            }

            outputUri
        } catch (e: Exception) {
            resolver.delete(outputUri, null, null)
            throw e
        }
    }
}
