package com.project.speciesdetection.domain.provider.language

import android.content.SharedPreferences
import android.util.Log
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import com.project.speciesdetection.domain.model.LanguageItem

@Singleton
class DeviceLanguageProvider @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : LanguageProvider {
    companion object {
        // Để public để có thể truy cập từ MyApplication nếu cần đọc trực tiếp
        const val KEY_SELECTED_LANGUAGE = "selected_language"
    }

    override fun getCurrentLanguageCode(): String {
        val userSelected = getUserSelectedLanguage()
        val current = userSelected ?: Locale.getDefault().language
        Log.d("DeviceLanguageProvider", "getCurrentLanguageCode: UserSelected='$userSelected', DefaultSystem='${Locale.getDefault().language}', Returning='$current'")
        return current
    }

    override fun getUserSelectedLanguage(): String? {
        val lang = sharedPreferences.getString(KEY_SELECTED_LANGUAGE, null)
        Log.d("DeviceLanguageProvider", "getUserSelectedLanguage: Fetched '$lang' from SharedPreferences with key '$KEY_SELECTED_LANGUAGE'")
        return lang
    }

    /*override fun setUserSelectedLanguage(languageCode: String) {
        Log.d("DeviceLanguageProvider", "setUserSelectedLanguage: Attempting to save language code '$languageCode' with key '$KEY_SELECTED_LANGUAGE'")
        sharedPreferences.edit(commit = true) { // Sử dụng commit = true để đảm bảo lưu ngay lập tức cho việc debug
            putString(KEY_SELECTED_LANGUAGE, languageCode)
        }
        Log.d("DeviceLanguageProvider", "setUserSelectedLanguage: Saved. Verifying: '${sharedPreferences.getString(KEY_SELECTED_LANGUAGE, "ERROR_READING_AFTER_SAVE")}'")
    }*/

    override fun getSupportedLanguages(): List<LanguageItem> {
        return listOf(
            LanguageItem("en", "English"),
            LanguageItem("vi", "Tiếng Việt"),
        )
    }
}