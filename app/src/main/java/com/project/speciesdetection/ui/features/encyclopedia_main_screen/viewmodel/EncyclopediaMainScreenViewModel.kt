package com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.Timestamp
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesClassUseCase
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class EncyclopediaMainScreenViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val getLocalizedSpeciesClassUseCase: GetLocalizedSpeciesClassUseCase,
    private val observationRepository: ObservationRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "EncyclopediaVM"
        private const val SEARCH_DEBOUNCE_MS = 700L
    }

    private val _speciesDateFound =
        MutableStateFlow<Map<String, Timestamp?>>(emptyMap())
    val speciesDateFound: StateFlow<Map<String, Timestamp?>> = _speciesDateFound.asStateFlow()

    val currentLanguageState = MutableStateFlow("none")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _speciesClassList = MutableStateFlow<List<DisplayableSpeciesClass>>(emptyList())
    val speciesClassList: StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()

    private val _selectedClassId = MutableStateFlow<String?>("0")
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    private val _currentUser = MutableStateFlow(userRepository.getCurrentUser())
    val currentUser = _currentUser.asStateFlow()

    private val _sortByDesc = MutableStateFlow(false)
    val sortByDesc = _sortByDesc.asStateFlow()

    fun updateSortDirection() {
        _sortByDesc.value = !_sortByDesc.value
    }


    val _init = MutableStateFlow(true)
    val init = _init.asStateFlow()

    val speciesPagingDataFlow: Flow<PagingData<DisplayableSpecies>> =
        combine(
            _sortByDesc,
            _selectedClassId,
            _searchQuery.debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged(),
        ) { sortBy, classId, query ->
            Triple(sortBy, classId, query)
        }.flatMapLatest { (sortBy, classId, query) -> // flatMapLatest để hủy request cũ khi có giá trị mới
            //Log.d(TAG, "flatMapLatest triggered. ClassId: $classId, Query: '$query'")

            if (classId == null) {
                //Log.w(TAG, "ClassId is null, returning empty PagingData.")
                return@flatMapLatest flowOf(PagingData.empty<DisplayableSpecies>())
            }

            val currentUser = userRepository.getCurrentUser()

            if (classId == "0") {
                //Log.d(TAG, "Fetching ALL paged species with query: '$query'")
                getLocalizedSpeciesUseCase.getAll(
                    searchQuery = query, currentUser?.uid, sortByDesc = _sortByDesc.value
                )
            } else {
                //Log.d(TAG, "Fetching paged species for ClassId: $classId, query: '$query'")
                getLocalizedSpeciesUseCase.getByClassPaged(
                    classIdValue = classId,
                    searchQuery = query,
                    uid = currentUser?.uid, sortByDesc = _sortByDesc.value
                )
            }
        }
            .cachedIn(viewModelScope) // cachedIn rất quan trọng để Paging 3 hoạt động đúng và giữ dữ liệu khi xoay màn hình

    init {
        viewModelScope.launch {

            loadInitialSpeciesClasses()

        }
    }

    fun getSpeciesListState(speciesList: List<DisplayableSpecies>, uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _init.value = true
            clearObservationState()
            observationRepository.getObservationsStateForSpeciesList(speciesList, uid)
                .forEach { pair ->
                    //Log.i("a", pair.toString())
                    _speciesDateFound.value = _speciesDateFound.value.toMutableMap().apply {
                        put(pair.key, pair.value)
                    }
                }

            //Log.i("a", _speciesDateFound.value.toString())
            _init.value = false
            observer(uid)

        }
    }


    fun loadInitialSpeciesClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            //Log.d(TAG, "Loading initial species classes...")
            getLocalizedSpeciesClassUseCase.getAll()
                .catch { e ->
                    //Log.e("ViewModel", "Error loading species classes", e)
                }
                .collect { result ->
                    when (result) {
                        is DataResult.Success -> {
                            _speciesClassList.value = result.data
                            if (result.data.isEmpty() && _selectedClassId.value == "0") {
                                /*Log.d(
                                    "ViewModel",
                                    "No species classes found, but 'All' is selected."
                                )*/
                            }
                        }

                        is DataResult.Error -> {
//                            Log.e(
//                                "ViewModel",
//                                "Error in species class result: ${result.exception.localizedMessage}"
//                            )
                        }

                        is DataResult.Loading -> {
//                            Log.d("ViewModel", "Loading species classes...")
                        }
                    }
                }
        }
    }

    fun selectSpeciesClass(classId: String) {
//        Log.d(TAG, "selectSpeciesClass called with ID: $classId")
        if (_selectedClassId.value != classId) {
            _selectedClassId.value = classId
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setLanguage(language: String) {
        currentLanguageState.value = language
    }

    fun observer(userId: String) {
        viewModelScope.launch {
            observeAllUserObservationsChanges(userId).collect { change ->
                val currentMap = _speciesDateFound.value.toMutableMap()

                when (change) {
                    is ObservationChange.Added,
                    is ObservationChange.Modified -> {
                        val observation = when (change) {
                            is ObservationChange.Added -> change.observation
                            is ObservationChange.Modified -> change.observation
                            else -> null
                        }

                        observation?.let {
                            val id = it.speciesId
                            val timestamp = it.dateFound
                            val currentTimestamp = currentMap[id]

                            if (currentTimestamp != timestamp) {
                                currentMap[id] = timestamp
                                _speciesDateFound.value = currentMap
//                                Log.i("ObserveChange", "Updated: $id -> $timestamp")
                            }
                        }
                    }

                    is ObservationChange.Removed -> {
                        val id = change.observationId
//                        Log.i("checkObserve0,", currentMap.toString())
                        if (currentMap.containsKey(id)) {
                            currentMap.remove(id)
                            _speciesDateFound.value = currentMap
//                            Log.i("ObserveChange", "Removed: $id")
                        }
                    }
                }
//                Log.i("á", "${change}")
            }

        }
    }


    fun observeAllUserObservationsChanges(userId: String): Flow<ObservationChange> {
        return observationRepository.getObservationChangesForUser(userId)
    }

    fun observeDateFoundForUidAndSpecies(uid: String, speciesId: String) {
        if (uid.isNotEmpty())
            viewModelScope.launch {
                observationRepository.checkUserObservationState(uid, speciesId) { dateFound ->
                    if (dateFound != null) {
                        _speciesDateFound.value = _speciesDateFound.value.toMutableMap().apply {
                            put(speciesId, dateFound)
                        }
                    } else {
                    }
                }
            }

    }

    fun clearObservationState() {
        _speciesDateFound.value = emptyMap()
    }

    // Hàm để retry load PagingData (có thể gọi từ UI)
    // Tuy nhiên, Paging 3.x thường dùng lazyPagingItems.retry() hoặc .refresh() từ UI.
    // Nếu bạn muốn một hàm cụ thể trong ViewModel:
    // fun retryLoadSpecies() {
    //     // Để trigger lại flatMapLatest, bạn có thể thay đổi một trong các StateFlow đầu vào của nó một chút
    //     // Ví dụ, nếu bạn muốn refresh dựa trên selectedClassId hiện tại:
    //     val currentClassId = _selectedClassId.value
    //     _selectedClassId.value = null // Tạm thời set null
    //     _selectedClassId.value = currentClassId // Set lại giá trị cũ để trigger
    //     Log.d(TAG, "Retry load species triggered.")
    // }
}