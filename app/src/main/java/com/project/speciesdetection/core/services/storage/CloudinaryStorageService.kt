package com.project.speciesdetection.core.services.storage

import android.content.Context
import android.net.Uri
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
            // Lấy inputStream từ Uri
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw IOException("Cannot open InputStream for an Uri: $imageUri")

            // Đọc dữ liệu ảnh thành byte array
            val imageBytes = inputStream.use { it.readBytes() }

            // Lấy kiểu media (ví dụ: "image/jpeg")
            val mediaType = context.contentResolver.getType(imageUri)

            // Tạo request body cho file
            val fileRequestBody = imageBytes.toRequestBody(mediaType?.toMediaTypeOrNull())

            // Tạo request body dạng multipart/form-data
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    // Đặt tên file ngẫu nhiên để tránh trùng lặp
                    "${UUID.randomUUID()}.jpg",
                    fileRequestBody
                )
                .build()

            // Tạo request POST
            val request = Request.Builder()
                .url(apiUrl)
                .post(multipartBody)
                .build()

            // Thực thi request
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Upload failed: ${response.code} ${response.message} - ${response.body?.string()}")
            }

            val responseBodyString = response.body?.string()
                ?: throw IOException("Empty response body from Cloudinary")

            // Parse JSON response để lấy secure_url
            val uploadResponse = json.decodeFromString<CloudinaryUploadResponse>(responseBodyString)

            Result.success(uploadResponse.secureUrl)

        } catch (e: Exception) {
            // Ghi log lỗi ở đây nếu cần
            // Log.e("CloudinaryUpload", "Error uploading image", e)
            Result.failure(e)
        }
    }
}