package com.project.speciesdetection.core.helpers

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun setLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(languageCode)
        }else{
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
        saveLanguagePreference(context, languageCode)
    }

    fun saveLanguagePreference(context: Context, languageCode: String) {
        val pref = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        pref.edit().putString("selected_language", languageCode).apply()
    }
}