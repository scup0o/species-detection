package com.project.speciesdetection.domain.provider.language

import android.content.SharedPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class DeviceLanguageProvider @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : LanguageProvider {
    companion object {
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
    }

    // Lấy ngôn ngữ hiện tại từ SharedPreferences hoặc ngôn ngữ mặc định của thiết bị
    override fun getCurrentLanguageCode(): String {
        return getUserSelectedLanguage() ?: Locale.getDefault().language
    }

    // Lấy ngôn ngữ người dùng đã chọn từ SharedPreferences
    private fun getUserSelectedLanguage(): String? {
        return sharedPreferences.getString(KEY_SELECTED_LANGUAGE, null)
    }

    // Cập nhật ngôn ngữ người dùng đã chọn
    fun setUserSelectedLanguage(languageCode: String) {
        sharedPreferences.edit() { putString(KEY_SELECTED_LANGUAGE, languageCode) }
    }
}
