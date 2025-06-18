package com.project.speciesdetection.core.services.storage

import android.content.Context
import android.net.Uri
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.domain.model.CloudinaryUploadResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class CloudinaryStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @Named("CloudinaryCloudName") private val cloudName: String,
    @Named("CloudinaryUploadPreset") private val uploadPreset: String
) : StorageService {

    private val apiUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    override suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw IOException("Cannot open InputStream for Uri: $imageUri")

            val fileBytes = inputStream.use { it.readBytes() }

            val mediaType = context.contentResolver.getType(imageUri) ?: "application/octet-stream"

            // Chọn apiUrl dựa trên mediaType
            val apiUrl = if (mediaType.startsWith("video/")) {
                "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
            } else if (mediaType.startsWith("image/")) {
                "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            } else {
                throw IOException("Unsupported media type: $mediaType")
            }

            // Đặt extension file phù hợp theo loại (ảnh hoặc video)
            val extension = when {
                mediaType.startsWith("video/") -> ".mp4"  // hoặc lấy extension thật nếu bạn cần
                mediaType.startsWith("image/") -> ".jpg"
                else -> ""
            }

            val fileRequestBody = fileBytes.toRequestBody(mediaType.toMediaTypeOrNull())

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    "${UUID.randomUUID()}$extension",
                    fileRequestBody
                )
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(multipartBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Upload failed: ${response.code} ${response.message} - ${response.body?.string()}")
            }

            val responseBodyString = response.body?.string()
                ?: throw IOException("Empty response body from Cloudinary")

            val uploadResponse = json.decodeFromString<CloudinaryUploadResponse>(responseBodyString)

            Result.success(CloudinaryImageURLHelper.resizeCloudinaryUrl(uploadResponse.secureUrl, "w_100,ar_1:1,c_fill,g_auto").replace(".mp4", ".jpg"))

        } catch (e: Exception) {
            // Ghi log lỗi ở đây nếu cần
            // Log.e("CloudinaryUpload", "Error uploading image", e)
            Result.failure(e)
        }
    }
}