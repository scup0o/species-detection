package com.project.speciesdetection.domain.provider.language

import com.project.speciesdetection.domain.model.LanguageItem

interface LanguageProvider {
    fun getCurrentLanguageCode(): String
    fun getUserSelectedLanguage(): String?
    //fun setUserSelectedLanguage(languageCode: String)
    fun getSupportedLanguages(): List<LanguageItem>
}