package com.project.speciesdetection.ui.features.identification_analysis.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.project.speciesdetection.BuildConfig
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.provider.image_classifier.ImageClassifierProvider
import com.project.speciesdetection.domain.provider.language.LanguageProvider
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Đại diện cho một kết quả nhận dạng duy nhất từ ML model, bao gồm thông tin loài và điểm tin cậy.
 */
data class RecognitionResult(
    val species: DisplayableSpecies,
    val confidence: Float
)

/**
 * Data class để chứa kết quả chi tiết từ Google AI.
 */
@Serializable
data class AiSpeciesInfo(
    // Thông tin cơ bản
    val scientificName: String,
    val commonName: String,

    // --- SỬA ĐỔI: Thêm đầy đủ các bậc phân loại ---
    val domain: String?,
    val kingdom: String?,
    val phylum: String?,
    val aClass: String?, // 'aClass' vì 'class' là keyword
    val order: String?,
    val family: String?,
    val genus: String?,
    // scientificName đã có ở trên, đóng vai trò là loài

    val conservationStatus: String,
    val summary: String,

    // Thông tin chi tiết
    val physicalDescription: String?,
    val distribution: String?,
    val habitat: String?,
    val behavior: String?,

    // Links
    val links: Map<String, String?>
)

/**
 * Data class để chứa kết quả cuối cùng của luồng AI.
 */
data class AiIdentificationResult(
    val aiInfo: AiSpeciesInfo,
    val speciesFromServer: DisplayableSpecies?
)


/**
 * Các trạng thái UI cho màn hình phân tích.
 */
sealed class AnalysisUiState {
    object Initial : AnalysisUiState()
    object ClassifierInitializing : AnalysisUiState()
    object ImageProcessing : AnalysisUiState()
    data class Success(val recognitions: List<RecognitionResult>) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
    object NoResults : AnalysisUiState()

    object AiIdentifying : AnalysisUiState()
    data class AiSuccess(val result: AiIdentificationResult) : AnalysisUiState()
    data class AiError(val message: String) : AnalysisUiState()
}


@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val observationRepository: ObservationRepository,
    private val userRepository: UserRepository,
    application: Application,
    @Named("enetb0_classifier_provider") private val classifier: ImageClassifierProvider,
    @Named("language_provider") private val languageProvider: LanguageProvider
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Initial)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var currentAnalysisJob: Job? = null
    private var isClassifierReadyInternal = false

    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    private val _speciesDateFound = MutableStateFlow<Map<String, Timestamp?>>(emptyMap())
    val speciesDateFound: StateFlow<Map<String, Timestamp?>> = _speciesDateFound.asStateFlow()

    val currentUser = userRepository.getCurrentUser()?.uid

    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.4f
            topK = 32
            topP = 1.0f
            maxOutputTokens = 4096
        }
    )

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "AnalysisViewModel"
        private const val BITMAP_COMPRESSION_QUALITY = 75
        private const val MAX_IMAGE_DIMENSION = 768
    }

    fun startImageAnalysis(imageUri: Uri?) {
        if (imageUri == null) {
            _uiState.value = AnalysisUiState.Error("No image provided for analysis.")
            return
        }
        if (_uiState.value is AnalysisUiState.AiIdentifying || _uiState.value is AnalysisUiState.AiSuccess) {
            _uiState.value = AnalysisUiState.Initial
        }

        currentAnalysisJob?.cancel()
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
                val bitmap = loadBitmapFromUri(imageUri)

                if (bitmap != null) {
                    val results = classifier.classify(bitmap)
                    if (results.isNotEmpty()) {
                        val speciesMap = getLocalizedSpeciesUseCase.getById(
                            results.map { result -> result.id }, currentUser
                        ).associateBy { it.id }

                        val recognitionResults = results.mapNotNull { recognition ->
                            speciesMap[recognition.id]?.let {
                                RecognitionResult(
                                    species = it,
                                    confidence = recognition.confidence
                                )
                            }
                        }
                        _uiState.value = AnalysisUiState.Success(recognitionResults)
                        currentUser?.let { userId ->
                            startListeningForResults(recognitionResults, userId)
                        }

                        Log.i("result", recognitionResults.toString())

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

    @OptIn(ExperimentalSerializationApi::class)
    fun identifyWithGoogleAI(
        imageUri: Uri,
        description: String,
        languageCode: String = languageProvider.getCurrentLanguageCode()
    ) {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AnalysisUiState.AiIdentifying
            try {
                val bitmap = loadBitmapFromUri(imageUri)
                if (bitmap == null) {
                    _uiState.value = AnalysisUiState.AiError("Could not load image for analysis.")
                    return@launch
                }

                val prompt = createDetailedPromptForAI(description, languageCode)

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text
                Log.d(TAG, "AI Detailed Response: $responseText")

                if (responseText != null) {
                    val aiInfo = json.decodeFromString<AiSpeciesInfo>(responseText)
                    val scientificNameId = aiInfo.scientificName.replace(" ", "").lowercase()
                    val speciesListFromServer =
                        getLocalizedSpeciesUseCase.getById(listOf(scientificNameId), currentUser)

                    val finalResult = AiIdentificationResult(
                        aiInfo = aiInfo,
                        speciesFromServer = speciesListFromServer.firstOrNull()
                    )
                    _uiState.value = AnalysisUiState.AiSuccess(finalResult)
                } else {
                    _uiState.value = AnalysisUiState.AiError("AI did not return a valid response.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during AI identification", e)
                val errorMessage = when {
                    e.message?.contains("overloaded", ignoreCase = true) == true ||
                            e.message?.contains("UNAVAILABLE", ignoreCase = true) == true -> {
                        "Máy chủ AI hiện đang quá tải. Vui lòng thử lại sau vài phút."
                    }
                    e.message?.contains("400", ignoreCase = true) == true &&
                            e.message?.contains("API_KEY", ignoreCase = true) == true -> {
                        "API Key không hợp lệ. Vui lòng kiểm tra lại cấu hình."
                    }
                    e is java.net.UnknownHostException -> {
                        "Không có kết nối mạng. Vui lòng kiểm tra và thử lại."
                    }
                    else -> {
                        Log.e(TAG, "Unhandled AI Exception", e)
                        "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại."
                    }
                }
                _uiState.value = AnalysisUiState.AiError(errorMessage)
            }
        }
    }

    private fun createDetailedPromptForAI(description: String, languageCode: String): String {
        // --- SỬA ĐỔI: Cập nhật prompt để yêu cầu đầy đủ các bậc phân loại ---
        return """
        Identify the creature in the image based on the following information.
        User description: "$description"
        Desired language for common name and all descriptive fields: "$languageCode"

        Provide a detailed response in a valid JSON format, without markdown. The JSON object must have the following structure. For optional fields, if information is not available, the value must be null.

        {
          "scientificName": "string (Required)",
          "commonName": "string (Required)",
          "domain": "string (Optional, can be null)",
          "kingdom": "string (Optional, can be null)",
          "phylum": "string (Optional, can be null)",
          "aClass": "string (Optional, can be null)",
          "order": "string (Optional, can be null)",
          "family": "string (Optional, can be null)",
          "genus": "string (Optional, can be null)",
          "conservationStatus": "string (IUCN code: LC, NT, VU, EN, CR, EW, EX, DD, or NE. Required)",
          "summary": "string (A brief one-paragraph summary. Required)",
          "physicalDescription": "string (Detailed physical description. Optional, can be null)",
          "distribution": "string (Geographic distribution. Optional, can be null)",
          "habitat": "string (Natural habitat. Optional, can be null)",
          "behavior": "string (Behavior and diet. Optional, can be null)",
          "links": {
            "iNaturalist": "full_url_or_null",
            "wikipedia": "full_url_or_null",
            "adw": "full_url_or_null",
            "worldlandtrust": "full_url_or_null",
            "vietnamredlist": "full_url_or_null"
          }
        }
        """.trimIndent()
    }

    private fun startListeningForResults(results: List<RecognitionResult>, userId: String) {
        val newSpeciesIds = results.map { it.species.id }.toSet()
        val listenersToRemove = activeListeners.keys - newSpeciesIds
        listenersToRemove.forEach { speciesId ->
            activeListeners.remove(speciesId)?.remove()
        }
        _speciesDateFound.update { currentMap ->
            currentMap - listenersToRemove
        }

        viewModelScope.launch {
            newSpeciesIds.forEach { speciesId ->
                if (!activeListeners.containsKey(speciesId)) {
                    val listener = observationRepository.checkUserObservationState(
                        uid = userId,
                        speciesId = speciesId,
                        onDataChanged = { timestamp ->
                            _speciesDateFound.update { currentMap ->
                                currentMap + (speciesId to timestamp)
                            }
                        }
                    )
                    activeListeners[speciesId] = listener
                }
            }
        }
    }

    fun isProcessing(): Boolean {
        return uiState.value is AnalysisUiState.ClassifierInitializing ||
                uiState.value is AnalysisUiState.ImageProcessing ||
                uiState.value is AnalysisUiState.AiIdentifying
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

    private suspend fun loadBitmapFromUri(imageUri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val width = originalBitmap.width
                val height = originalBitmap.height
                val scaleFactor = if (width > height && width > MAX_IMAGE_DIMENSION) {
                    MAX_IMAGE_DIMENSION.toFloat() / width
                } else if (height > width && height > MAX_IMAGE_DIMENSION) {
                    MAX_IMAGE_DIMENSION.toFloat() / height
                } else {
                    1.0f
                }

                val resizedBitmap = if (scaleFactor < 1.0f) {
                    originalBitmap.scale(
                        (width * scaleFactor).toInt(),
                        (height * scaleFactor).toInt()
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_COMPRESSION_QUALITY, outputStream)
                val byteArray = outputStream.toByteArray()
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load and compress bitmap", e)
                null
            }
        }

    override fun onCleared() {
        super.onCleared()
        currentAnalysisJob?.cancel()
        Log.d(TAG, "AnalysisViewModel cleared. Removing ${activeListeners.size} active listeners.")
        activeListeners.values.forEach { listener ->
            listener.remove()
        }
        activeListeners.clear()
    }
}