package com.project.speciesdetection.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudinaryUploadResponse(
    @SerialName("secure_url")
    val secureUrl: String,

    // Thêm các trường khác nếu bạn cần
    // @SerialName("public_id")
    // val publicId: String,
    // @SerialName("resource_type")
    // val resourceType: String
)