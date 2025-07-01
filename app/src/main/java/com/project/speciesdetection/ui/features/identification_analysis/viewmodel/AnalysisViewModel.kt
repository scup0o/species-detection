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

sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    object ClassifierInitializing : AnalysisUiState()
    object ImageProcessing : AnalysisUiState()
    data class Success(val recognitions: List<DisplayableSpecies>) : AnalysisUiState()
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
                    val results = classifier.classify(bitmap)
                    if (results.isNotEmpty()) {
                        val speciesMap = getLocalizedSpeciesUseCase.getById(
                            results.map { result -> result.id }, currentUser

                        ).associateBy { it.id }

                        val orderedSpecies = results.mapNotNull { recognition ->
                            speciesMap[recognition.id]
                        }

                        _uiState.value = AnalysisUiState.Success(orderedSpecies)

                        currentUser?.let { userId ->
                            startListeningForResults(orderedSpecies, userId)
                        }
                    } else {
                        _uiState.value = AnalysisUiState.NoResults
                    }
                } else {
                    _uiState.value = AnalysisUiState.Error("Could not load image for analysis.")
                }
            } catch (e: CancellationException) {
                _uiState.value = AnalysisUiState.Initial
                //Log.i(TAG, "Analysis job was cancelled.")
            } catch (e: Exception) {
                //Log.e(TAG, "Exception during analysis process", e)
                _uiState.value = AnalysisUiState.Error("Analysis error: ${e.localizedMessage}")
            }
        }
    }

    private fun startListeningForResults(speciesList: List<DisplayableSpecies>, userId: String) {
        val newSpeciesIds = speciesList.map { it.id }.toSet()

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
                //Log.e(TAG, "Failed to load bitmap", e)
                null
            }
        }

    override fun onCleared() {
        super.onCleared()
        currentAnalysisJob?.cancel() // Hủy job đang chạy nếu có

        //Log.d(TAG, "AnalysisViewModel cleared. Removing ${activeListeners.size} active listeners.")
        // Lặp qua tất cả listener trong map và gỡ bỏ chúng
        activeListeners.values.forEach { listener ->
            listener.remove()
        }
        activeListeners.clear() // Xóa sạch map
    }
}