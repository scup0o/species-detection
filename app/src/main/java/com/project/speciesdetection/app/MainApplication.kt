package com.project.speciesdetection.app

import android.app.Application
import com.project.speciesdetection.core.helpers.LocaleHelper
import com.project.speciesdetection.core.helpers.LocaleHelper.setLanguage
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication(): Application(){
    override fun onCreate() {
        super.onCreate()

        // Lấy ngôn ngữ đã lưu từ SharedPreferences
        val languageCode = LocaleHelper.getLanguagePreference(this)

        // Áp dụng ngôn ngữ cho ứng dụng khi nó khởi động
        setLanguage(this, languageCode)
    }
}