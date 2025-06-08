package com.project.speciesdetection.ui.features.observation.viewmodel

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ObservationUiState(
    val isEditing: Boolean = false,
    val observationId: String? = null,
    val description: String = "",
    val speciesName: String = "",
    val speciesId: String = "",
    val speciesScientificName: String = "",

    val location: GeoPoint? = null,
    val locationName: String = "Chọn vị trí",
    val locationDisplayName : String = "",
    val isFetchingInitialLocation: Boolean = true, // Cờ để biết đang lấy vị trí ban đầu

    val dateFound: Timestamp? = null,
    val dateFoundText: String = "Chọn ngày giờ",
    val images: List<Any> = emptyList(),
    val privacy: String = "Public",
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false
)

sealed interface ObservationEvent {
    data class OnDescriptionChange(val text: String) : ObservationEvent
    data class OnLocationSelected(val lan: Double, val lon: Double, val name: String, val displayName: String) : ObservationEvent
    object OnLocationClear : ObservationEvent
    data class OnDateSelected(val date: Date) : ObservationEvent
    object OnDateClear : ObservationEvent
    data class OnAddImage(val uri: Uri) : ObservationEvent
    data class OnRemoveImage(val image: Any) : ObservationEvent
    data class OnPrivacyChange(val newPrivacy: String) : ObservationEvent
    data class OnImageClick(val image: Any) : ObservationEvent
    data class SaveObservation(val user: User) : ObservationEvent
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
    private val geocodingService: GeocodingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObservationUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ObservationEffect>()
    val effect = _effect.asSharedFlow()

    init {
        initializeState(savedStateHandle)
    }

    private fun initializeState(savedStateHandle: SavedStateHandle) {
        // Luôn reset cờ loading vị trí khi ViewModel được tạo
        _uiState.update { it.copy(isFetchingInitialLocation = true) }

        val observationJson: String? = savedStateHandle["observationJson"]
        val observationFromNav = observationJson?.let {
            try { Json.decodeFromString<Observation>(Uri.decode(it)) } catch (e: Exception) { null }
        }

        if (observationFromNav != null) {
            // Chế độ chỉnh sửa
            _uiState.update {
                it.copy(
                    isEditing = true,
                    observationId = observationFromNav.id,
                    description = observationFromNav.content,
                    speciesName = observationFromNav.speciesName,
                    speciesId = observationFromNav.speciesId,
                    speciesScientificName = observationFromNav.speciesScientificName,
                    location = observationFromNav.location,
                    locationName = observationFromNav.locationName,
                    locationDisplayName = observationFromNav.locationDisplayName,
                    dateFound = observationFromNav.dateFound,
                    dateFoundText = observationFromNav.dateFound?.toDate()?.let { d -> formatDate(d) } ?: "Chọn ngày giờ",
                    images = observationFromNav.imageURL,
                    privacy = observationFromNav.privacy,
                    isFetchingInitialLocation = false // Đã có vị trí, không cần fetch
                )
            }
            //observationFromNav.location?.let { reverseGeocode(it) }
        } else {
            // Chế độ tạo mới
            val imageUri: Uri? = savedStateHandle.get<String>("imageUri")?.toUri()
            val speciesId: String? = savedStateHandle["speciesId"]
            val speciesName: String? = savedStateHandle.get<String>("speciesName")?.let { Uri.decode(it) }
            val speciesScientificName: String? = savedStateHandle.get<String>("speciesSN")?.let { Uri.decode(it) }

            val initialImages = imageUri?.let { listOf(it) } ?: emptyList()
            _uiState.update {
                it.copy(
                    isEditing = false,
                    speciesId = speciesId ?: "",
                    speciesName = speciesName ?: "",
                    speciesScientificName = speciesScientificName ?: "",
                    images = initialImages
                )
            }
        }
    }

    fun reverseGeocode(geoPoint: org.osmdroid.util.GeoPoint) {
        viewModelScope.launch {
            val selectedAddress = geocodingService.reverseSearch(geoPoint.latitude, geoPoint.longitude)?.displayName
                ?: "Khong"
            if (selectedAddress.isNotEmpty()) {
                val firebaseGeoPoint = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                _uiState.update {
                    it.copy(
                        location = firebaseGeoPoint,
                        locationName = selectedAddress
                    )
                }
            }
        }
        /*if (!Geocoder.isPresent()) {
            _uiState.update { it.copy(locationName = "Không thể tìm địa chỉ") }
            return
        }

        viewModelScope.launch {
            val address = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var foundAddress: String? = null
                        geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) { addresses ->
                            foundAddress = addresses.firstOrNull()?.getAddressLine(0)
                        }
                        kotlinx.coroutines.delay(500)
                        foundAddress
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                            ?.firstOrNull()?.getAddressLine(0)
                    }
                } catch (e: Exception) { "Vị trí không xác định" }
            }
            _uiState.update { it.copy(locationName = address ?: "Vị trí không xác định") }
        }*/
    }

    fun onEvent(event: ObservationEvent) {
        when (event) {
            is ObservationEvent.OnDescriptionChange -> _uiState.update { it.copy(description = event.text) }
            is ObservationEvent.OnLocationSelected -> {

                _uiState.update {
                    it.copy(
                        location = GeoPoint(event.lan,event.lon),
                        locationName = event.name,
                        locationDisplayName = event.displayName,
                        isFetchingInitialLocation = false
                    )
                }

                /*
                // Luôn tắt cờ loading khi nhận được một vị trí mới
                _uiState.update {
                    it.copy(
                        location = event.geoPoint,
                        isFetchingInitialLocation = false
                    )
                }
                // Nếu có tên địa chỉ (từ search) thì dùng luôn, nếu không thì gọi API
                if (event.name.isNotBlank()) {
                    _uiState.update { it.copy(locationName = event.name) }
                } else {
                    //reverseGeocode(event.geoPoint)
                }*/
            }
            is ObservationEvent.OnLocationClear -> _uiState.update { it.copy(location = null, locationName = "Chọn vị trí") }
            is ObservationEvent.OnDateSelected -> {
                val timestamp = Timestamp(event.date)
                _uiState.update { it.copy(dateFound = timestamp, dateFoundText = formatDate(event.date)) }
            }
            is ObservationEvent.OnDateClear -> _uiState.update { it.copy(dateFound = null, dateFoundText = "Chọn ngày giờ") }
            is ObservationEvent.OnAddImage -> _uiState.update { it.copy(images = it.images + event.uri) }
            is ObservationEvent.OnRemoveImage -> _uiState.update { it.copy(images = it.images - event.image) }
            is ObservationEvent.OnPrivacyChange -> _uiState.update { it.copy(privacy = event.newPrivacy) }
            is ObservationEvent.OnImageClick -> viewModelScope.launch { _effect.emit(ObservationEffect.NavigateToFullScreenImage(event.image)) }
            is ObservationEvent.SaveObservation -> save(event.user)
        }
    }

    private fun save(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value
            val result = if (currentState.isEditing) {
                repository.updateObservation(
                    observationId = currentState.observationId!!,
                    content = currentState.description,
                    images = currentState.images,
                    privacy = currentState.privacy,
                    location = currentState.location,
                    dateFound = currentState.dateFound,
                    speciesId = currentState.speciesId
                )
            } else {
                if (currentState.speciesId.isEmpty() || user.uid.isEmpty()) {
                    _effect.emit(ObservationEffect.ShowError("Thiếu thông tin loài hoặc người dùng."))
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                repository.createObservation(
                    user = user,
                    speciesId = currentState.speciesId,
                    content = currentState.description,
                    imageUris = currentState.images.filterIsInstance<Uri>(),
                    privacy = currentState.privacy,
                    location = currentState.location,
                    dateFound = currentState.dateFound
                )
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(ObservationEffect.ShowError(error.message ?: "Đã xảy ra lỗi không xác định"))
            }
        }
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}