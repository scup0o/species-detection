package com.project.speciesdetection.ui.features.observation.viewmodel.species_observation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SpeciesObservationViewModel @Inject constructor(
    private val mapService: GeocodingService,
    @Named("language_provider") languageProvider: LanguageProvider,
    private val repository: ObservationRepository
) : ViewModel() {

    enum class ViewMode {
        LIST,
        MAP
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _filterParams = MutableStateFlow<Pair<String, String?>>(Pair("", null))

    private val _updatedObservations = MutableStateFlow<Map<String, Observation?>>(emptyMap())
    val updatedObservations: StateFlow<Map<String, Observation?>> =
        _updatedObservations.asStateFlow()

    private val _sortByDesc = MutableStateFlow(true)
    val sortByDesc = _sortByDesc.asStateFlow()

    fun updateSortDirection() {
        _sortByDesc.value = !_sortByDesc.value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val observationPagingData: Flow<PagingData<Observation>> = combine(
        _filterParams,
        _selectedTab,
        _sortByDesc
    ) { params, tabIndex, sortByDesc ->
        val speciesId = params.first
        val currentUserId = params.second
        val queryUid = if (tabIndex == 1) currentUserId else null
        Triple(speciesId, queryUid, sortByDesc)
    }
        .filter { it.first.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { (speciesId, uid, sortByDesc) ->
            //Log.d("ViewModel", "Tab/Filter changed. Clearing updatedObservations.")
            _updatedObservations.value = emptyMap()
            repository.getObservationPager(
                speciesId = speciesId,
                uid = uid,
                queryByDesc = sortByDesc
            ) // Giả sử có một hàm chung này

        }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allObservationsForMap: StateFlow<List<Observation>> = combine(
        _filterParams,
        _selectedTab,
        _viewMode
    ) { params, tabIndex, mode ->
        Triple(params, tabIndex, mode)
    }
        .filter { (params, _, mode) ->
            params.first.isNotBlank() && mode == ViewMode.MAP
        }
        .distinctUntilChanged()
        .flatMapLatest { (params, tabIndex, _) ->
            val (speciesId, currentUserId) = params
            val queryUid = if (tabIndex == 1) currentUserId else null
            repository.getAllObservationsAsList(speciesId = speciesId, uid = queryUid)
            /*.map {

                observation -> observation.map { obs ->
                var geo = mapService.reverseSearch(obs.location?.latitude?:0.0, obs.location?.longitude?:0.0)

                obs.copy(
                locationName = geo?.name?:"",
                locationDisplayName = geo?.displayName?:""
            ) }
            }*/
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            combine(_filterParams, _selectedTab) { params, tabIndex ->
                Pair(params, tabIndex)
            }
                .distinctUntilChanged()
                .collect { (params, tabIndex) ->
                    val (speciesId, currentUserId) = params

                    _updatedObservations.value = emptyMap()

                    if (speciesId.isNotBlank()) {
                        if (tabIndex == 1 && currentUserId != null) {
                            //Log.d("ViewModel", "Listening for MY observations. UID: $currentUserId")
                            startListeningForChanges(speciesId, currentUserId)
                        } else {
                            //Log.d("ViewModel", "Listening for ALL observations (Modify/Remove only).")

                        }
                    }
                }
        }
    }

    private var currentListenerJob: Job? = null
    private fun startListeningForChanges(speciesId: String, uid: String?) {
        currentListenerJob?.cancel()
        currentListenerJob = viewModelScope.launch {
            repository.listenToObservationChanges(speciesId, uid)
                .collect { changes ->
                    _updatedObservations.update { currentMap ->
                        val newMap = currentMap.toMutableMap()
                        changes.forEach { change ->
                            when (change) {
                                //is ObservationChange.Added -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Modified -> newMap[change.observation.id!!] =
                                    change.observation

                                is ObservationChange.Removed -> newMap[change.observationId] =
                                    null // Đánh dấu là đã xóa
                                else -> {}
                            }
                        }
                        newMap
                    }
                }
        }
    }

    fun setFilters(speciesId: String, currentUserId: String?) {
        _filterParams.value = Pair(speciesId, currentUserId)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }


    /*@OptIn(ExperimentalCoroutinesApi::class)
    fun listenForRealtimeUpdates() {
        viewModelScope.launch {
                       combine(_filterParams, _selectedTab) { params, tabIndex ->
                val speciesId = params.first
                val currentUserId = params.second
                Log.i("che", speciesId)
                val queryUid = if (tabIndex == 1) currentUserId else null
                Pair(speciesId, queryUid)
            }
                .filter {
                    it.first.isNotBlank()
                }
                .distinctUntilChanged()
                .flatMapLatest { (speciesId, uid) ->
                    // Mỗi khi tab hoặc filter thay đổi, hủy listener cũ và tạo listener mới
                    Log.d("ViewModel", "Resetting updates and starting new listener for species: $speciesId, uid: $uid")
                    _updatedObservations.value = emptyMap() // Xóa các update cũ khi đổi tab

                    repository.listenToObservationChanges(speciesId, uid)
                }
                .collect { changes ->
                    Log.d("ViewModel", "Received ${changes.size} changes from Firestore.")
                    _updatedObservations.update { currentMap ->
                        val newMap = currentMap.toMutableMap()
                        changes.forEach { change ->
                            when (change) {
                                is ObservationChange.Added -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Modified -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Removed -> newMap[change.observationId] = null // Đánh dấu là đã xóa
                            }
                        }
                        newMap
                    }

                }
        }
    }*/
}