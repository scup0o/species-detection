package com.project.speciesdetection.ui.features.observation.viewmodel.species_picker

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.provider.image_classifier.ImageClassifierProvider
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

// --- THAY ĐỔI 1: Tạo data class mới để chứa kết quả nhận dạng ---
/**
 * Đại diện cho một kết quả nhận dạng, bao gồm thông tin loài và điểm tin cậy.
 * @param species Thông tin chi tiết của loài.
 * @param confidence Điểm tin cậy của kết quả (ví dụ: 0.95 cho 95%).
 */
data class RecognitionResult(
    val species: DisplayableSpecies,
    val confidence: Float
)

sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    object ClassifierInitializing : AnalysisUiState()
    object ImageProcessing : AnalysisUiState()
    // --- THAY ĐỔI 2: Cập nhật trạng thái Success để dùng RecognitionResult ---
    data class Success(val recognitions: List<RecognitionResult>) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
    object NoResults : AnalysisUiState()
}

@HiltViewModel
class SpeciesPickerViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    @Named("enetb0_classifier_provider") private val classifier: ImageClassifierProvider,
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SpeciesPickerViewModel"
        private const val SEARCH_DEBOUNCE_MS = 700L
    }

    // --- State cho chức năng phân tích hình ảnh ---
    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()
    private var analysisJob: Job? = null
    private var isClassifierReady = false

    private val currentUser = userRepository.getCurrentUser()

    // --- State cho chức năng tìm kiếm bằng văn bản ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchPagingData: Flow<PagingData<DisplayableSpecies>> =
        _searchQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(PagingData.empty())
                } else {
                    Log.d(TAG, "Fetching paged species for query: '$query'")
                    getLocalizedSpeciesUseCase.getAll(searchQuery = query, uid = currentUser?.uid)
                }
            }
            .cachedIn(viewModelScope)

    // === LOGIC PHÂN TÍCH HÌNH ẢNH ===

    fun startImageAnalysis(imageUri: Uri?) {
        if (imageUri == null) {
            _analysisState.value = AnalysisUiState.Error("No image provided.")
            return
        }

        analysisJob?.cancel() // Hủy job cũ nếu có
        analysisJob = viewModelScope.launch {
            try {
                // Khởi tạo classifier nếu chưa sẵn sàng
                if (!isClassifierReady) {
                    _analysisState.value = AnalysisUiState.ClassifierInitializing
                    if (!classifier.setup()) {
                        _analysisState.value = AnalysisUiState.Error("Failed to initialize analysis engine.")
                        isClassifierReady = false
                        return@launch
                    }
                    isClassifierReady = true
                }

                _analysisState.value = AnalysisUiState.ImageProcessing
                val bitmap = loadBitmapFromUri(imageUri)

                if (bitmap != null) {
                    val results = classifier.classify(bitmap)
                    if (results.isNotEmpty()) {
                        // Lấy thông tin chi tiết các loài từ kết quả
                        val speciesMap = getLocalizedSpeciesUseCase.getById(
                            results.map { it.id },
                            uid = currentUser?.uid
                        ).associateBy { it.id }

                        // --- THAY ĐỔI 3: Tạo danh sách RecognitionResult có cả loài và độ tin cậy ---
                        val recognitionResults = results.mapNotNull { recognition ->
                            val speciesDetails = speciesMap[recognition.id]
                            speciesDetails?.let {
                                RecognitionResult(
                                    species = it,
                                    confidence = recognition.confidence // Lấy độ tin cậy từ kết quả classify
                                )
                            }
                        }

                        _analysisState.value = AnalysisUiState.Success(recognitionResults)
                    } else {
                        _analysisState.value = AnalysisUiState.NoResults
                    }
                } else {
                    _analysisState.value = AnalysisUiState.Error("Could not load image.")
                }
            } catch (e: CancellationException) {
                _analysisState.value = AnalysisUiState.Initial
                Log.i(TAG, "Analysis job was cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during analysis", e)
                _analysisState.value = AnalysisUiState.Error("Analysis error: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun loadBitmapFromUri(imageUri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap", e)
                null
            }
        }

    // === LOGIC TÌM KIẾM BẰNG VĂN BẢN ===

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // === QUẢN LÝ VÒNG ĐỜI VIEWMODEL ===

    fun resetAndClear() {
        analysisJob?.cancel()
        _analysisState.value = AnalysisUiState.Initial
        _searchQuery.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
        Log.d(TAG, "ViewModel cleared.")
    }
}