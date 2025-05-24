package com.project.speciesdetection.ui.features.identification_analysis.viewmodel

import android.app.Application // Cần Application context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Sử dụng AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.provider.image_classifier.ImageClassifierProvider
import com.project.speciesdetection.domain.provider.image_classifier.Recognition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

sealed class AnalysisUiState {
    object Initial : AnalysisUiState() // Trạng thái ban đầu, chưa có yêu cầu
    object ClassifierInitializing : AnalysisUiState() // Classifier (trong AnalysisVM) đang được khởi tạo
    object ImageProcessing : AnalysisUiState()      // Đang xử lý ảnh (load bitmap, classify)
    data class Success(val recognitions: List<Recognition>) : AnalysisUiState() // Thành công
    data class Error(val message: String) : AnalysisUiState()              // Lỗi
    object NoResults : AnalysisUiState()                                  // Không có kết quả
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    application: Application, // Inject Application
    @Named("enetb0_classifier_provider") private val classifier: ImageClassifierProvider // Inject Classifier (từ AppModule)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var currentAnalysisJob: Job? = null
    private var isClassifierReadyInternal = false

    companion object { private const val TAG = "AnalysisViewModel" }

    fun startImageAnalysis(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.value = AnalysisUiState.Error("No image provided for analysis.")
            return
        }
        currentAnalysisJob?.cancel() // Hủy job cũ nếu có
        currentAnalysisJob = viewModelScope.launch {
            try {
                if (!isClassifierReadyInternal) {
                    _uiState.value = AnalysisUiState.ClassifierInitializing
                    Log.i(TAG, "Initializing classifier for analysis...")
                    val setupSuccess = classifier.setup()
                    if (!setupSuccess) {
                        _uiState.value = AnalysisUiState.Error("Failed to initialize analysis engine.")
                        isClassifierReadyInternal = false
                        return@launch
                    }
                    isClassifierReadyInternal = true
                    Log.i(TAG, "Classifier ready for analysis.")
                }

                _uiState.value = AnalysisUiState.ImageProcessing
                Log.d(TAG, "Loading bitmap from Uri: $imageUri")
                val bitmap = loadBitmapFromUriForClassification(imageUri)

                if (bitmap != null) {
                    Log.d(TAG, "Bitmap loaded. Starting classification...")
                    val results = classifier.classify(bitmap)
                    if (results.isNotEmpty()) {
                        _uiState.value = AnalysisUiState.Success(results)
                        Log.i(TAG, "Analysis successful with ${results.size} results.")
                    } else {
                        _uiState.value = AnalysisUiState.NoResults
                        Log.i(TAG, "Analysis returned no results.")
                    }
                } else {
                    _uiState.value = AnalysisUiState.Error("Could not load image for analysis.")
                    Log.e(TAG, "Bitmap is null after loading from Uri.")
                }
            } catch (e: CancellationException) {
                _uiState.value = AnalysisUiState.Initial // Reset nếu bị hủy
                Log.i(TAG, "Analysis job was cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during analysis process", e)
                _uiState.value = AnalysisUiState.Error("Analysis error: ${e.localizedMessage}")
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
        Log.i(TAG, "Analysis explicitly cancelled by user action.")
    }

    fun resetState() {
        if (isProcessing()){
            Log.w(TAG, "ResetState called while processing, cancelling current analysis.")
            cancelAnalysis()
        }
        _uiState.value = AnalysisUiState.Initial
        Log.i(TAG, "AnalysisViewModel state reset to Initial.")
    }

    private suspend fun loadBitmapFromUriForClassification(imageUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
            val rawBitmap = BitmapFactory.decodeStream(inputStream)?.copy(Bitmap.Config.ARGB_8888, false)

            inputStream?.close()
            rawBitmap?.let {
                Bitmap.createScaledBitmap(it, 224, 224, true)
            }
        } catch (e: Exception) {
            Log.e("BitmapLoader", "Failed to load bitmap", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun onCleared() {
        super.onCleared()
        currentAnalysisJob?.cancel()
        Log.d(TAG, "AnalysisViewModel cleared.")
        // Classifier is likely a Singleton, Hilt manages its lifecycle beyond this ViewModel.
        // If Classifier needs explicit cleanup tied to this VM, call classifier.close() here.
    }
}