package com.project.speciesdetection.ui.features.observation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.project.speciesdetection.core.services.content_moderation.ContentModerationService // Đảm bảo import đúng service
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

data class ObservationUiState(
    val isEditing: Boolean = false,
    val observationId: String? = null,
    val description: String = "",
    val speciesName: String = "",
    val speciesId: String = "",
    val speciesScientificName: String = "",

    val location: GeoPoint? = null,
    val locationName: String = "Chọn vị trí",
    val locationDisplayName: String = "",
    val isFetchingInitialLocation: Boolean = true,

    val dateFound: Timestamp? = null,
    val dateFoundText: String = "Chọn ngày giờ",
    val images: List<Any> = emptyList(),
    val privacy: String = "Public",
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,

    val dateCreated: Timestamp? = null,
    val commentCount: Int = 0,
    val likeUserIds: List<String> = emptyList(),
    val dislikeUserIds: List<String> = emptyList(),
    val point: Int = 0,
    val locationTempName: String = "",

    val isSpeciesLocked: Boolean = false,
)

sealed interface ObservationEvent {
    data class OnDescriptionChange(val text: String) : ObservationEvent
    data class OnLocationSelected(
        val lan: Double,
        val lon: Double,
        val name: String,
        val displayName: String,
        val address: String
    ) : ObservationEvent

    object OnLocationClear : ObservationEvent
    data class OnDateSelected(val date: Date) : ObservationEvent
    object OnDateClear : ObservationEvent
    data class OnAddImage(val uri: Uri) : ObservationEvent
    data class OnRemoveImage(val image: Any) : ObservationEvent
    data class OnPrivacyChange(val newPrivacy: String) : ObservationEvent
    data class OnImageClick(val image: Any) : ObservationEvent
    data class SaveObservation(val user: User) : ObservationEvent
    data class OnSpeciesSelected(val species: DisplayableSpecies) : ObservationEvent
    object OnSpeciesClear : ObservationEvent
    data class OnSpeciesNameChange(val name: String) : ObservationEvent
}

sealed interface ObservationEffect {
    data class NavigateToFullScreenImage(val image: Any) : ObservationEffect
    data class ShowError(val message: String) : ObservationEffect
}

@HiltViewModel
class ObservationViewModel @Inject constructor(
    private val repository: ObservationRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val geocodingService: GeocodingService,
    // Đổi tên biến inject để rõ ràng hơn
    private val contentModerationService: ContentModerationService,
    @Named("language_provider") languageProvider: LanguageProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObservationUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ObservationEffect>()
    val effect = _effect.asSharedFlow()

    private val currentLanguage = languageProvider.getCurrentLanguageCode()

    private val observationFromNav: Observation?

    init {
        observationFromNav = savedStateHandle.get<String>("observationJson")?.let {
            try {
                Json.decodeFromString<Observation>(Uri.decode(it))
            } catch (e: Exception) {
                null
            }
        }
        initializeState(savedStateHandle)
    }

    private fun initializeState(savedStateHandle: SavedStateHandle) {
        if (observationFromNav != null) {
            _uiState.update {
                it.copy(
                    isSpeciesLocked = false,
                    isEditing = true,
                    observationId = observationFromNav.id,
                    description = observationFromNav.content,
                    speciesName = observationFromNav.speciesName[currentLanguage] ?: "",
                    speciesId = observationFromNav.speciesId,
                    speciesScientificName = observationFromNav.speciesScientificName,
                    location = observationFromNav.location,
                    locationName = observationFromNav.locationName,
                    locationDisplayName = observationFromNav.locationDisplayName,
                    dateFound = observationFromNav.dateFound,
                    dateFoundText = observationFromNav.dateFound?.toDate()
                        ?.let { d -> formatDate(d) } ?: "Chọn ngày giờ",
                    images = observationFromNav.imageURL,
                    privacy = observationFromNav.privacy,
                    isFetchingInitialLocation = false,
                    dateCreated = observationFromNav.dateCreated,
                    commentCount = observationFromNav.commentCount,
                    point = observationFromNav.point,
                    likeUserIds = observationFromNav.likeUserIds,
                    dislikeUserIds = observationFromNav.dislikeUserIds,
                    locationTempName = observationFromNav.locationTempName
                )
            }
        } else {
            val imageUri: Uri? = savedStateHandle.get<String>("imageUri")?.toUri()
            val speciesId: String? = savedStateHandle["speciesId"]
            val speciesName: String =
                savedStateHandle.get<String>("speciesName")?.let { Uri.decode(it) } ?: ""
            val speciesScientificName: String? =
                savedStateHandle.get<String>("speciesSN")?.let { Uri.decode(it) }
            val isLocked = !speciesId.isNullOrBlank()
            val initialImages = imageUri?.let { listOf(it) } ?: emptyList()
            _uiState.update {
                it.copy(
                    isSpeciesLocked = isLocked,
                    isEditing = false,
                    speciesId = speciesId ?: "",
                    speciesName = speciesName,
                    speciesScientificName = speciesScientificName ?: "",
                    images = initialImages,
                    dateFound = Timestamp.now(),
                    dateFoundText = formatDate(Date())
                )
            }
        }
    }

    fun onEvent(event: ObservationEvent) {
        when (event) {
            is ObservationEvent.OnDescriptionChange -> _uiState.update { it.copy(description = event.text) }
            is ObservationEvent.OnLocationSelected -> {
                _uiState.update {
                    it.copy(
                        location = GeoPoint(event.lan, event.lon),
                        locationName = event.name,
                        locationDisplayName = event.displayName,
                        isFetchingInitialLocation = false,
                        locationTempName = event.address
                    )
                }
            }

            is ObservationEvent.OnLocationClear -> _uiState.update {
                it.copy(
                    location = null,
                    locationName = "Chọn vị trí",
                    locationDisplayName = ""
                )
            }

            is ObservationEvent.OnDateSelected -> {
                val timestamp = Timestamp(event.date)
                _uiState.update {
                    it.copy(
                        dateFound = timestamp,
                        dateFoundText = formatDate(event.date)
                    )
                }
            }

            is ObservationEvent.OnDateClear -> _uiState.update {
                it.copy(
                    dateFound = null,
                    dateFoundText = "Chọn ngày giờ"
                )
            }

            is ObservationEvent.OnAddImage -> _uiState.update { it.copy(images = it.images + event.uri) }
            is ObservationEvent.OnRemoveImage -> _uiState.update { it.copy(images = it.images - event.image) }
            is ObservationEvent.OnPrivacyChange -> _uiState.update { it.copy(privacy = event.newPrivacy) }
            is ObservationEvent.OnImageClick -> viewModelScope.launch {
                _effect.emit(
                    ObservationEffect.NavigateToFullScreenImage(event.image)
                )
            }

            is ObservationEvent.SaveObservation -> save(event.user)
            is ObservationEvent.OnSpeciesSelected -> {
                _uiState.update {
                    it.copy(
                        speciesId = event.species.id,
                        speciesName = event.species.localizedName.ifEmpty { "" },
                        speciesScientificName = event.species.getScientificName() ?: ""
                    )
                }
            }

            is ObservationEvent.OnSpeciesClear -> {
                _uiState.update {
                    it.copy(
                        speciesId = "",
                        speciesName = "",
                        speciesScientificName = ""
                    )
                }
            }

            is ObservationEvent.OnSpeciesNameChange -> {
                _uiState.update {
                    it.copy(
                        speciesName = event.name,
                        speciesId = if (it.speciesName != event.name) "" else it.speciesId,
                        speciesScientificName = if (it.speciesName != event.name) "" else it.speciesScientificName
                    )
                }
            }
        }
    }

    private fun save(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value

            if (currentState.speciesId.isEmpty() && currentState.speciesName.isEmpty()) {
                _effect.emit(ObservationEffect.ShowError("empty_species"))
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            if (currentState.description.isNotBlank()) {
                val textModerationResult = contentModerationService.isTextAppropriate(
                    text = currentState.description,
                    lang = currentLanguage
                )

                if (textModerationResult.isFailure) {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(ObservationEffect.ShowError("moderation_error"))
                    return@launch
                }

                if (!textModerationResult.getOrDefault(false)) {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(ObservationEffect.ShowError("text_unappropriated"))
                    return@launch
                }
            }
            val newImageUris = currentState.images.filterIsInstance<Uri>()

            if (newImageUris.isNotEmpty()) {
                for (uri in newImageUris) {
                    val imageModerationResult = contentModerationService.isImageAppropriate(uri)

                    if (imageModerationResult.isFailure) {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(ObservationEffect.ShowError("moderation_error"))
                        return@launch
                    }

                    if (!imageModerationResult.getOrDefault(false)) {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(ObservationEffect.ShowError("images_unappropriated"))
                        return@launch
                    }
                }
            }
            val result = if (currentState.isEditing) {
                repository.updateObservation(
                    user = user,
                    observationId = currentState.observationId!!,
                    content = currentState.description,
                    images = currentState.images,
                    privacy = currentState.privacy,
                    location = currentState.location,
                    dateFound = currentState.dateFound,
                    speciesId = currentState.speciesId,
                    dateCreated = currentState.dateCreated,
                    point = currentState.point,
                    likeUserIds = currentState.likeUserIds,
                    dislikeUserIds = currentState.dislikeUserIds,
                    commentCount = currentState.commentCount,
                    locationTempName = currentState.locationTempName,
                    speciesName = mapOf(currentLanguage to currentState.speciesName),
                    baseObservation = observationFromNav ?: Observation()
                )
            } else {
                repository.createObservation(
                    user = user,
                    speciesId = currentState.speciesId,
                    speciesName = mapOf(currentLanguage to currentState.speciesName),
                    content = currentState.description,
                    imageUris = newImageUris,
                    privacy = currentState.privacy,
                    location = currentState.location,
                    dateFound = currentState.dateFound,
                    locationTempName = currentState.locationTempName
                )
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(
                    ObservationEffect.ShowError(
                        error.message ?: "Đã xảy ra lỗi không xác định"
                    )
                )
            }
        }
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}