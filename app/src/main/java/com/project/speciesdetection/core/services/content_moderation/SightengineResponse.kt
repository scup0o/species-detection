package com.project.speciesdetection.core.services.content_moderation

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class chính để hứng response từ API của Sightengine.
 * Nó được thiết kế để xử lý cả kết quả kiểm duyệt hình ảnh và văn bản.
 */
@Serializable
data class SightengineResponse(
    val status: String,
    val request: RequestInfo? = null,
    val error: Error? = null,

    // --- TRƯỜNG DÀNH CHO KIỂM DUYỆT VĂN BẢN ---
    @SerialName("moderation_classes")
    val moderationClasses: ModerationClasses? = null,

    // --- CÁC TRƯỜNG DÀNH CHO KIỂM DUYỆT HÌNH ẢNH ---
    val nudity: NudityContent? = null,
    val weapon: Float? = null,
    val alcohol: Float? = null,
    val drugs: Float? = null,
    val scam: ScamContent? = null,
    @SerialName("offensive")
    val offensive: OffensiveContent? = null,
    val gore: GoreContent? = null,
) {
    fun isTextInappropriate(threshold: Float = 0.85f): Boolean {
        if (status != "success") return true
        if (moderationClasses == null) {
            // Nếu không có moderation_classes, có thể API đã trả về lỗi hoặc một định dạng khác
            // Log lại để kiểm tra. Tạm thời coi là không vi phạm để tránh chặn nhầm.
            Log.w("ModerationCheck", "moderation_classes object is null in the response.")
            return false
        }

        Log.d("ModerationCheck", "Text Scores - " +
                "Sexual: ${moderationClasses.sexual}, " +
                "Discriminatory: ${moderationClasses.discriminatory}, " +
                "Insulting: ${moderationClasses.insulting}, " +
                "Violent: ${moderationClasses.violent}, " +
                "Toxic: ${moderationClasses.toxic}, " +
                "Self-harm: ${moderationClasses.selfHarm}")

        return moderationClasses.sexual > threshold ||
                moderationClasses.discriminatory > threshold ||
                moderationClasses.insulting > threshold ||
                moderationClasses.violent > threshold ||
                moderationClasses.toxic > threshold ||
                moderationClasses.selfHarm > threshold
    }

    fun isImageInappropriate(threshold: Float = 0.5f): Boolean {
        if (status != "success") return true
        return nudity?.isNudity(threshold) == true ||
                (weapon ?: 0f) > threshold ||
                (alcohol ?: 0f) > threshold ||
                (drugs ?: 0f) > threshold ||
                (gore?.prob ?: 0f) > threshold ||
                (offensive?.prob ?: 0f) > threshold
    }

    @Serializable
    data class RequestInfo(val id: String? = null, val timestamp: Double? = null, val operations: Int? = null)
    @Serializable
    data class Error(val type: String? = null, val code: Int? = null, val message: String? = null)
}

@Serializable
data class ModerationClasses(
    val available: List<String> = emptyList(),
    val sexual: Float = 0.0f,
    val discriminatory: Float = 0.0f,
    val insulting: Float = 0.0f,
    val violent: Float = 0.0f,
    val toxic: Float = 0.0f,
    @SerialName("self-harm")
    val selfHarm: Float = 0.0f
)

@Serializable
data class NudityContent(val raw: Float, val partial: Float, val safe: Float) {
    fun isNudity(threshold: Float): Boolean = raw > threshold || partial > threshold
}
@Serializable data class OffensiveContent(val prob: Float)
@Serializable data class GoreContent(val prob: Float)
@Serializable data class ScamContent(val prob: Float)