package com.project.speciesdetection.ui.features.setting_main_screen.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.OfflineDataRepository
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanguageState(
    val name: String,
    val code: String,
    val isDownloaded: Boolean = false,
    val isProcessing: Boolean = false
)

data class DataManagementUiState(
    val languageStates: List<LanguageState> = emptyList(),
    val isNetworkAvailable: Boolean = false
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val offlineDataRepository: OfflineDataRepository,
    speciesClassDao: SpeciesClassDao,
    speciesDao : SpeciesDao,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    companion object {
        private const val TAG = "DataManagementVM"
    }

    private val _processingLanguageCode = MutableStateFlow<String?>(null)

    private val supportedLanguages = mapOf(
        "en" to "English",
        "vi" to "Tiếng Việt",
    )

    val uiState: StateFlow<DataManagementUiState> = combine(
        speciesDao.getDownloadedLanguageCodes(),
        _processingLanguageCode,
        connectivityObserver.observe()
    ) { downloadedCodes, processingCode, networkStatus ->

        // Tạo danh sách trạng thái cho từng ngôn ngữ
        val languages = supportedLanguages.map { (code, name) ->
            LanguageState(
                name = name,
                code = code,
                isDownloaded = downloadedCodes.contains(code),
                isProcessing = (code == processingCode)
            )
        }

        DataManagementUiState(
            languageStates = languages,
            isNetworkAvailable = (networkStatus == ConnectivityObserver.Status.Available)
        )
    }.stateIn(
        scope = viewModelScope,
        // Bắt đầu flow khi có subscriber và giữ nó active trong 5 giây sau khi subscriber cuối cùng rời đi.
        started = SharingStarted.WhileSubscribed(5000),
        // Giá trị ban đầu, sẽ được hiển thị ngay lập tức trước khi flow đầu tiên phát ra.
        initialValue = DataManagementUiState()
    )

    fun onDownloadClicked(languageCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Không cho phép chạy nhiều tác vụ cùng lúc để tránh xung đột
            if (_processingLanguageCode.value != null) return@launch

            Log.d(TAG, "Download requested for language: $languageCode")
            // Cập nhật state để hiển thị loading cho đúng ngôn ngữ
            _processingLanguageCode.value = languageCode

            when (val result = offlineDataRepository.downloadAllDataForLanguage(languageCode)) {
                is DataResult.Success -> {
                    Log.i(TAG, "Download successful for: $languageCode")
                }
                is DataResult.Error -> {
                    Log.e(TAG, "Download failed for: $languageCode", result.exception)
                }

                DataResult.Loading -> {}
            }

            // Hoàn thành xử lý, tắt loading
            _processingLanguageCode.value = null
        }
    }

    fun onRemoveClicked(languageCode: String) {
        viewModelScope.launch {
            if (_processingLanguageCode.value != null) return@launch

            Log.d(TAG, "Remove requested for language: $languageCode")
            _processingLanguageCode.value = languageCode

            offlineDataRepository.removeAllDataForLanguage(languageCode)
            Log.i(TAG, "Removal complete for: $languageCode")

            // Hoàn thành xử lý, tắt loading
            _processingLanguageCode.value = null
        }
    }
}