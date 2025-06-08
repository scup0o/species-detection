package com.project.speciesdetection.ui.features.observation.viewmodel.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.core.services.map.NominatimPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class MapPickerUiState(
    val selectedGeoPoint: GeoPoint? = null,
    val selectedAddress: String = "Đang tìm vị trí của bạn...",
    val cameraPosition: GeoPoint? = null,
    val searchQuery: String = "",
    val searchResults: List<NominatimPlace> = emptyList(),
    val isSearching: Boolean = false,
    val ignoreNextMapMove: Boolean = false
)

@HiltViewModel
class MapPickerViewModel @Inject constructor(
    private val geocodingService: GeocodingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun setInitialLocation(geoPoint: GeoPoint) {
        if (_uiState.value.cameraPosition == null) {
            _uiState.update {
                it.copy(cameraPosition = geoPoint, selectedGeoPoint = geoPoint)
            }
            reverseGeocode(geoPoint)
        }
    }

    fun onMapMoved(geoPoint: GeoPoint) {
        if (_uiState.value.ignoreNextMapMove) {
            _uiState.update { it.copy(ignoreNextMapMove = false) }
            return
        }
        _uiState.update { it.copy(selectedGeoPoint = geoPoint) }
        reverseGeocode(geoPoint)
    }

    private fun reverseGeocode(geoPoint: GeoPoint) {
        viewModelScope.launch {
            val result = geocodingService.reverseSearch(geoPoint.latitude, geoPoint.longitude)
            if (result != null) {
                _uiState.update { it.copy(selectedAddress = result.displayName) }
            } else {
                _uiState.update { it.copy(selectedAddress = "Không tìm thấy địa chỉ") }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length < 3) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(500)
            val results = geocodingService.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun onPlaceSelected(place: NominatimPlace) {
        val newGeoPoint = GeoPoint(place.lat.toDouble(), place.lon.toDouble())
        _uiState.update {
            it.copy(
                cameraPosition = newGeoPoint,
                selectedGeoPoint = newGeoPoint,
                selectedAddress = place.displayName,
                searchQuery = "",
                searchResults = emptyList(),
                ignoreNextMapMove = true
            )
        }
    }
}