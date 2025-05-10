package com.project.speciesdetection.domain.provider.language

interface LanguageProvider {
    fun getCurrentLanguageCode(): String
}