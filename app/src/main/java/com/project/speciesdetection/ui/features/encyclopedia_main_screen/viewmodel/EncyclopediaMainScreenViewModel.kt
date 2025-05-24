package com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel

import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.project.speciesdetection.R
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesClassUseCase
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EncyclopediaMainScreenViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val getLocalizedSpeciesClassUseCase: GetLocalizedSpeciesClassUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _speciesClassList = MutableStateFlow(emptyList<DisplayableSpeciesClass>())
    val speciesClassList: StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()
    private val speciesClassMapFlow: StateFlow<Map<String, String>> =
        _speciesClassList
            .map { list ->
                list.associateBy({ it.id }, { it.localizedName })
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L), // Giữ map tồn tại
                initialValue = emptyMap()
            )

    private val _selectedClassId = MutableStateFlow<String?>("0")
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    @OptIn(FlowPreview::class)
    val speciesPagingDataFlow: Flow<PagingData<DisplayableSpecies>> =
        combine(
            _selectedClassId,
            speciesClassMapFlow,
            _searchQuery // Lắng nghe thay đổi searchQuery
                .debounce(1000L) // Chờ 500ms sau khi người dùng ngừng gõ
                .distinctUntilChanged() // Chỉ phát ra nếu giá trị thực sự thay đổi
        ) { classId, classMap, query ->
            Triple(classId, classMap, query) // Kết hợp thành một Triple
        }.flatMapLatest { (classId, classMap, query) ->
            Log.d("ViewModel", "flatMapLatest triggered. ClassId: $classId, Query: '$query'")
            if (classId == null) {
                Log.d("ViewModel", "No classId selected, returning empty PagingData flow")
                kotlinx.coroutines.flow.flowOf(PagingData.empty())
            } else {
                if (classId == "0") { // "Tất cả"
                    Log.d("ViewModel", "Fetching ALL paged species with query: '$query'")
                    getLocalizedSpeciesUseCase.getAll(searchQuery = query) // Truyền searchQuery
                        .map { pagingData ->
                            pagingData.map { species ->
                                val scientificList =
                                    species.scientific + mapOf(
                                        "class" to species.localizedClass.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase() else it.toString()
                                                })
                                val className = classMap[species.localizedClass] ?: ""
                                species.copy(
                                    localizedClass = className,
                                    scientific = scientificList)
                            }
                        }
                } else {
                    Log.d("ViewModel", "Fetching paged species for classId: $classId, query: '$query'")
                    getLocalizedSpeciesUseCase.getByClassPaged(
                        classIdValue = classId,
                        searchQuery = query // Truyền searchQuery
                    )
                        .map { pagingData ->
                            pagingData.map { species ->
                                val scientificList =
                                    species.scientific + mapOf(
                                        "class" to species.localizedClass.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase() else it.toString()
                                        })
                                val className = classMap[species.localizedClass] ?: ""
                                species.copy(
                                    localizedClass = className,
                                    scientific = scientificList)
                            }
                        }

                }
            }
        }.cachedIn(viewModelScope)

    init {
        loadInitialSpeciesClasses()
    }

    private fun loadInitialSpeciesClasses() {
        viewModelScope.launch(Dispatchers.IO) {
            getLocalizedSpeciesClassUseCase.getAll()
                .catch { e ->
                    Log.e("ViewModel", "Error loading species classes", e)
                }
                .collect { result ->
                    when (result) {
                        is DataResult.Success -> {
                            _speciesClassList.value = result.data
                            if (result.data.isEmpty() && _selectedClassId.value == "0") {
                                Log.d("ViewModel", "No species classes found, but 'All' is selected.")
                            }
                        }
                        is DataResult.Error -> {
                            Log.e("ViewModel", "Error in species class result: ${result.exception.localizedMessage}")
                        }
                        is DataResult.Loading -> {
                            Log.d("ViewModel", "Loading species classes...")
                        }
                    }
                }
        }
    }

    fun selectSpeciesClass(classId: String) {
        Log.d("ViewModel", "selectSpeciesClass called with ID: $classId")
        if (_selectedClassId.value != classId) {
            _selectedClassId.value = classId
            // speciesPagingDataFlow tự động cập nhật nhờ flatMapLatest
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}