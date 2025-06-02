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
            val config = context.resources.configuration
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)        }
        saveLanguagePreference(context, languageCode)
    }

    fun saveLanguagePreference(context: Context, languageCode: String) {
        val pref = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        pref.edit().putString("selected_language", languageCode).apply()
    }

    fun getLanguagePreference(context: Context): String {
        val pref = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
        return pref.getString("selected_language", Locale.getDefault().language) ?: Locale.getDefault().language
    }
}