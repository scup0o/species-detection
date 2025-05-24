package com.project.speciesdetection.domain.provider.image_classifier

import android.graphics.Bitmap

interface ImageClassifierProvider {
    suspend fun setup(): Boolean // suspend cho phép chạy trên background thread
    suspend fun classify(bitmap: Bitmap): List<Recognition>
    fun close()
}

// Data class cho kết quả nhận dạng
data class Recognition(val id: String, val title: String, val confidence: Float) {
    override fun toString(): String {
        // Định dạng chuỗi hiển thị đẹp hơn
        return "Title: $title, Confidence: ${String.format("%.2f%%", confidence * 100.0f)}"
    }
}