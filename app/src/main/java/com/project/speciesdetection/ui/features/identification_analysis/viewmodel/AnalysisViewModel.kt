package com.project.speciesdetection.ui.features.identification_analysis.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.provider.image_classifier.ImageClassifierProvider
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

// --- THAY ĐỔI 1: Tạo một data class mới để chứa cả loài và điểm tin cậy ---
/**
 * Đại diện cho một kết quả nhận dạng duy nhất, bao gồm thông tin loài và điểm tin cậy.
 * @param species Thông tin chi tiết của loài được hiển thị.
 * @param confidence Điểm tin cậy của kết quả nhận dạng (ví dụ: 0.95 cho 95%).
 */
data class RecognitionResult(
    val species: DisplayableSpecies,
    val confidence: Float
)

sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    object ClassifierInitializing : AnalysisUiState()
    object ImageProcessing : AnalysisUiState()
    // --- THAY ĐỔI 2: Cập nhật trạng thái Success để sử dụng RecognitionResult ---
    data class Success(val recognitions: List<RecognitionResult>) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
    object NoResults : AnalysisUiState()
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val observationRepository: ObservationRepository,
    private val userRepository: UserRepository,
    application: Application,
    @Named("enetb0_classifier_provider") private val classifier: ImageClassifierProvider
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var currentAnalysisJob: Job? = null
    private var isClassifierReadyInternal = false

    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    private val _speciesDateFound = MutableStateFlow<Map<String, Timestamp?>>(emptyMap())
    val speciesDateFound: StateFlow<Map<String, Timestamp?>> = _speciesDateFound.asStateFlow()

    val currentUser = userRepository.getCurrentUser()?.uid

    companion object {
        private const val TAG = "AnalysisViewModel"
    }

    fun startImageAnalysis(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.value = AnalysisUiState.Error("No image provided for analysis.")
            return
        }

        currentAnalysisJob?.cancel() // Hủy job phân tích cũ
        currentAnalysisJob = viewModelScope.launch {
            try {
                if (!isClassifierReadyInternal) {
                    _uiState.value = AnalysisUiState.ClassifierInitializing
                    if (!classifier.setup()) {
                        _uiState.value =
                            AnalysisUiState.Error("Failed to initialize analysis engine.")
                        isClassifierReadyInternal = false
                        return@launch
                    }
                    isClassifierReadyInternal = true
                }

                _uiState.value = AnalysisUiState.ImageProcessing
                val bitmap = loadBitmapFromUriForClassification(imageUri)

                if (bitmap != null) {
                    // `results` bây giờ sẽ là một List<Recognition> với `id` và `confidence`
                    val results = classifier.classify(bitmap)
                    if (results.isNotEmpty()) {
                        // Lấy thông tin chi tiết của các loài dựa trên ID
                        val speciesMap = getLocalizedSpeciesUseCase.getById(
                            results.map { result -> result.id }, currentUser
                        ).associateBy { it.id }

                        // --- THAY ĐỔI 3: Kết hợp kết quả nhận dạng với thông tin loài ---
                        val recognitionResults = results.mapNotNull { recognition ->
                            // Tìm thông tin chi tiết của loài tương ứng với ID
                            val speciesDetails = speciesMap[recognition.id]
                            // Nếu tìm thấy, tạo đối tượng RecognitionResult mới
                            speciesDetails?.let {
                                RecognitionResult(
                                    species = it,
                                    confidence = recognition.confidence // Giả sử thuộc tính này tên là 'confidence'
                                )
                            }
                        }

                        _uiState.value = AnalysisUiState.Success(recognitionResults)

                        currentUser?.let { userId ->
                            startListeningForResults(recognitionResults, userId)
                        }
                    } else {
                        _uiState.value = AnalysisUiState.NoResults
                    }
                } else {
                    _uiState.value = AnalysisUiState.Error("Could not load image for analysis.")
                }
            } catch (e: CancellationException) {
                _uiState.value = AnalysisUiState.Initial
                Log.i(TAG, "Analysis job was cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during analysis process", e)
                _uiState.value = AnalysisUiState.Error("Analysis error: ${e.localizedMessage}")
            }
        }
    }

    // --- THAY ĐỔI 4: Cập nhật hàm để nhận List<RecognitionResult> ---
    private fun startListeningForResults(results: List<RecognitionResult>, userId: String) {
        // Trích xuất danh sách ID từ List<RecognitionResult>
        val newSpeciesIds = results.map { it.species.id }.toSet()

        // Gỡ bỏ các listener của các loài không còn trong danh sách kết quả mới
        val listenersToRemove = activeListeners.keys - newSpeciesIds
        listenersToRemove.forEach { speciesId ->
            activeListeners.remove(speciesId)?.remove()
        }

        // Cập nhật state, xóa các item không còn được theo dõi
        _speciesDateFound.update { currentMap ->
            currentMap - listenersToRemove
        }

        viewModelScope.launch {
            // Thêm listener cho các loài mới (chưa được lắng nghe)
            newSpeciesIds.forEach { speciesId ->
                if (!activeListeners.containsKey(speciesId)) {
                    val listener = observationRepository.checkUserObservationState(
                        uid = userId,
                        speciesId = speciesId,
                        onDataChanged = { timestamp ->
                            // Cập nhật trạng thái cho item cụ thể này trong map
                            _speciesDateFound.update { currentMap ->
                                currentMap + (speciesId to timestamp)
                            }
                        }
                    )
                    // Thêm listener mới vào map để quản lý
                    activeListeners[speciesId] = listener
                }
            }
        }
    }

    fun isProcessing(): Boolean {
        return uiState.value is AnalysisUiState.ClassifierInitializing ||
                uiState.value is AnalysisUiState.ImageProcessing
    }

    fun cancelAnalysis() {
        currentAnalysisJob?.cancel()
        _uiState.value = AnalysisUiState.Initial
    }

    fun resetState() {
        if (isProcessing()) {
            cancelAnalysis()
        } else {
            _uiState.value = AnalysisUiState.Initial
        }
    }

    private suspend fun loadBitmapFromUriForClassification(imageUri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream =
                    getApplication<Application>().contentResolver.openInputStream(imageUri)
                BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap", e)
                null
            }
        }

    override fun onCleared() {
        super.onCleared()
        currentAnalysisJob?.cancel() // Hủy job đang chạy nếu có

        Log.d(TAG, "AnalysisViewModel cleared. Removing ${activeListeners.size} active listeners.")
        // Lặp qua tất cả listener trong map và gỡ bỏ chúng
        activeListeners.values.forEach { listener ->
            listener.remove()
        }
        activeListeners.clear() // Xóa sạch map
    }
}