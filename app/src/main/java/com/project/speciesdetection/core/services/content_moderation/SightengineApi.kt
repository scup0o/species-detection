package com.project.speciesdetection.core.services.content_moderation

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface để giao tiếp với API của Sightengine.
 */
interface SightengineApi {

    @Multipart
    @POST("1.0/check.json")
    suspend fun checkMediaFromLocal(
        @Part("api_user") apiUser: RequestBody,
        @Part("api_secret") apiSecret: RequestBody,
        @Part("models") models: RequestBody,
        @Part media: MultipartBody.Part
    ): SightengineResponse

    /**
     * Kiểm tra văn bản bằng cách gửi dữ liệu dưới dạng multipart/form-data.
     */
    @Multipart
    @POST("1.0/text/check.json")
    suspend fun checkText(
        @Part("models") models: RequestBody,
        @Part("mode") mode: RequestBody,
        @Part("lang") lang: RequestBody,
        @Part("text") text: RequestBody,
        @Part("api_user") apiUser: RequestBody,
        @Part("api_secret") apiSecret: RequestBody
    ): SightengineResponse
}