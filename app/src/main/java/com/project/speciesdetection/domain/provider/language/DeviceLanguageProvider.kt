package com.project.speciesdetection.domain.provider.language

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceLanguageProvider @Inject constructor() : LanguageProvider {
    override fun getCurrentLanguageCode(): String {
        return Locale.getDefault().language
    // Hoặc nếu bạn có cơ chế lưu trữ lựa chọn ngôn ngữ của người dùng (ví dụ: trong SharedPreferences),
    // bạn sẽ lấy từ đó trước, sau đó mới fallback về Locale.getDefault().language.
    }
}
