package com.project.speciesdetection.ui.features.observation.viewmodel.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.core.services.map.NominatimPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class NewMapPickerViewModel @Inject constructor(
    private val geocodingService: GeocodingService
) : ViewModel() {

    data class MapPickerUiState(
        val selectedGeoPoint: GeoPoint? = null,
        val selectedAddress: String = "Đang tìm vị trí của bạn...",
        val selectedDisplayName: String = "",
        val cameraPosition: GeoPoint? = null,
        val searchQuery: String = "",
        val searchResults: List<NominatimPlace> = emptyList(),
        val isSearching: Boolean = false,
        val ignoreNextMapMove: Boolean = false,
        val isLoading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(emptyList<NominatimPlace>())
    val searchResults: StateFlow<List<NominatimPlace>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    fun reverseGeocode(geoPoint: GeoPoint){
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            Log.i("a", geoPoint.toString())
            val selectedAddress = geocodingService.reverseSearch(geoPoint.latitude, geoPoint.longitude)
            val name = if (selectedAddress != null) {if (selectedAddress.name!="") selectedAddress.name else selectedAddress.displayName} else "Khong"
            val displayName = if (selectedAddress != null) {if (selectedAddress.name!="") selectedAddress.displayName else ""}else ""
            val geoPoint : GeoPoint = GeoPoint(
                selectedAddress?.lat?.toDouble() ?: 0.0,
                selectedAddress?.lon?.toDouble() ?: 0.0
            )
                _uiState.update { it.copy(isLoading = false, selectedAddress = name, selectedDisplayName = displayName, selectedGeoPoint = geoPoint) }

        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(500)
            val results = geocodingService.search(query)
            _searchResults.value = results
            _uiState.update { it.copy(isSearching = false) }
            Log.i("search", _searchResults.value.toString())
        }
    }
}