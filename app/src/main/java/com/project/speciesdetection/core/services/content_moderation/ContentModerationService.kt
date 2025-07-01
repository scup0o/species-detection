package com.project.speciesdetection.core.services.content_moderation

import android.content.Context
import android.net.Uri
import android.util.Log
import com.project.speciesdetection.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentModerationService @Inject constructor(
    private val sightengineApi: SightengineApi,
    @ApplicationContext private val context: Context
) {

    suspend fun isTextAppropriate(text: String, lang: String): Result<Boolean> {
        if (text.isBlank()) return Result.success(true)

        return try {
            val modelsToCheck = "general,self-harm"
            val modeToCheck = "ml"
            var languagesForDetection = "en,fr,it,pt,es,ru,tr"
            if (languagesForDetection.contains(lang)) languagesForDetection = lang else languagesForDetection="en"

            val textPlainType = "text/plain".toMediaTypeOrNull()
            val textBody = text.toRequestBody(textPlainType)
            val langBody = languagesForDetection.toRequestBody(textPlainType)
            val modeBody = modeToCheck.toRequestBody(textPlainType)
            val modelsBody = modelsToCheck.toRequestBody(textPlainType)
            val apiUserBody = BuildConfig.SIGHTENGINE_API_USER.toRequestBody(textPlainType)
            val apiSecretBody = BuildConfig.SIGHTENGINE_API_SECRET.toRequestBody(textPlainType)

            val response = sightengineApi.checkText(
                models = modelsBody,
                mode = modeBody,
                lang = langBody,
                text = textBody,
                apiUser = apiUserBody,
                apiSecret = apiSecretBody
            )

            if (response.status == "success") {
                if (response.isTextInappropriate(threshold = 0.85f)) {
                    Result.success(false)
                } else {
                    Result.success(true)
                }
            } else {
                Result.failure(Exception(response.error?.message ?: "Lỗi kiểm duyệt văn bản"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isImageAppropriate(mediaUri: Uri): Result<Boolean> {
        return try {
            val mimeType = context.contentResolver.getType(mediaUri)
                ?: return Result.failure(Exception("Không thể xác định loại media."))

            if (mimeType.startsWith("video/")) {
                Log.i("ModerationService", "Skipping moderation for video content.")
                return Result.success(true)
            }
            if (!mimeType.startsWith("image/")) {
                return Result.failure(Exception("Định dạng file không được hỗ trợ: $mimeType"))
            }

            val imageFile = uriToFile(mediaUri)
                ?: return Result.failure(Exception("Không thể tạo file tạm từ URI."))

            val apiUser = BuildConfig.SIGHTENGINE_API_USER.toRequestBody("text/plain".toMediaTypeOrNull())
            val apiSecret = BuildConfig.SIGHTENGINE_API_SECRET.toRequestBody("text/plain".toMediaTypeOrNull())
            val models = "nudity,wad,offensive,gore,scam".toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("media", imageFile.name, requestFile)
            val response = sightengineApi.checkMediaFromLocal(apiUser, apiSecret, models, body)

            imageFile.delete()

            if (response.status == "success") {
                if (response.isImageInappropriate(threshold = 0.5f)) {
                    Result.success(false)
                } else {
                    Result.success(true)
                }
            } else {
                Result.failure(Exception(response.error?.message ?: "Lỗi kiểm duyệt hình ảnh"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}