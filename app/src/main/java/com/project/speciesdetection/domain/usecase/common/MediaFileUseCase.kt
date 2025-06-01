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

    suspend fun saveImageToGallery(
        imageUri: Uri
    ): Uri {
        return withContext(Dispatchers.IO) {
            // Sử dụng applicationContext cho tất cả
            val resolver = context.contentResolver
            val appName = try {
                context.getString(R.string.app_name)
            } catch (e: Exception) {
                "SpeciesDetection"
            }

            val displayName = "EDITED_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val mimeType = "image/jpeg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$appName")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val imageOutUri = resolver.insert(collection, contentValues)
                ?: throw IOException("Không thể tạo bản ghi mới trong MediaStore cho việc lưu ảnh.")

            try {
                resolver.openOutputStream(imageOutUri).use { outStream ->
                    if (outStream == null) throw IOException("Không thể mở output stream cho ảnh đích: $imageOutUri")

                    // Sử dụng applicationContext để mở InputStream từ URI nguồn
                    context.contentResolver.openInputStream(imageUri).use { inStream ->
                        if (inStream == null) throw IOException("Không thể mở input stream từ ảnh nguồn: $imageUri")
                        inStream.copyTo(outStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageOutUri, contentValues, null, null)
                }
                //Log.i(TAG, "Image saved to gallery: $imageOutUri")
                imageOutUri
            } catch (e: Exception) {
                resolver.delete(imageOutUri, null, null)
                //Log.e(TAG, "Error during stream copy or IS_PENDING update, deleted temp entry $imageOutUri", e)
                throw e
            }
        }
    }
}
