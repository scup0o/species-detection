package com.project.speciesdetection.ui.features.setting_main_screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.model.LanguageItem
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SettingViewModel @Inject constructor(
    @Named("language_provider") private val languageProvider: LanguageProvider
) :ViewModel(){

    private val _currentLanguageCode = MutableStateFlow(languageProvider.getCurrentLanguageCode())
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()

    val supportedLanguages: List<LanguageItem> = languageProvider.getSupportedLanguages()

    init {
        viewModelScope.launch {
            _currentLanguageCode.value = languageProvider.getCurrentLanguageCode()
        }
    }

    fun onLanguageSelected(languageCode: String) {
        //languageProvider.setUserSelectedLanguage(languageCode)
        _currentLanguageCode.value = languageCode
    }
}