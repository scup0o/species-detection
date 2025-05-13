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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    /*sealed interface SpeciesScreenUiState {
        data object Loading : SpeciesScreenUiState
        data class Success(val speciesList: List<DisplayableSpecies>) : SpeciesScreenUiState
        data class Error(val message: String) : SpeciesScreenUiState
        data object Empty : SpeciesScreenUiState
    }

    private val _uiState = MutableStateFlow<SpeciesScreenUiState>(SpeciesScreenUiState.Loading)
    val uiState: StateFlow<SpeciesScreenUiState> = _uiState.asStateFlow()*/

    private val _speciesClassList = MutableStateFlow(emptyList<DisplayableSpeciesClass>())
    val speciesClassList : StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()
    private val speciesClassMapFlow: StateFlow<Map<String, String>> =
        _speciesClassList
            .map { list ->
                list.associateBy({ it.id }, { it.localizedName }) // Giả sử DisplayableSpeciesClass có id và localizedName
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L), // Giữ map tồn tại một chút
                initialValue = emptyMap() // Giá trị ban đầu
            )

    private val _selectedClassId = MutableStateFlow<String?>("0")
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    val speciesPagingDataFlow: Flow<PagingData<DisplayableSpecies>> =
        combine(_selectedClassId, speciesClassMapFlow) { classId, classMap ->
            classId to classMap
        }.flatMapLatest { (classId, classMap) ->
            if (classId == null) {
                Log.d("ViewModel", "No classId selected, returning empty PagingData flow")
                kotlinx.coroutines.flow.flowOf(PagingData.empty()) // Trả về PagingData rỗng nếu không có classId
            }
            else{
                if (classId=="0"){
                    getLocalizedSpeciesUseCase.getAll(searchQuery = "")
                        .map { pagingData ->
                            pagingData.map { species ->
                                val className = classMap[species.localizedClass] ?: ""
                                species.copy(localizedClass = className)
                            }
                        }
                }
                else {
                    Log.d("ViewModel", "Fetching paged species for classId: $classId")
                    getLocalizedSpeciesUseCase.getByClassPaged(classIdValue = classId, searchQuery = "")
                        .map { pagingData ->
                            pagingData.map { species ->
                                val className = classMap[species.localizedClass] ?: ""
                                species.copy(localizedClass = className)
                            }
                        }
                }
            }
        }.cachedIn(viewModelScope)

    init {
        loadInitialSpeciesClasses()
    }

    private fun loadInitialSpeciesClasses() {
        viewModelScope.launch(Dispatchers.IO) { // IO cho network/db
            getLocalizedSpeciesClassUseCase.getAll()
                .catch { e ->
                    Log.e("ViewModel", "Error loading species classes", e)
                    // Xử lý lỗi tải species class (ví dụ: hiển thị Snackbar)
                }
                .collect { result ->
                    when (result) {
                        is DataResult.Success -> {
                            _speciesClassList.value = result.data
                            if (result.data.isNotEmpty() && _selectedClassId.value == null) {
                                // Tự động chọn class đầu tiên nếu chưa có class nào được chọn
                                Log.d("ViewModel", "Species classes loaded, selecting first class: ${result.data.first().id}")
                                selectSpeciesClass(result.data.first().id)
                            } else if (result.data.isEmpty()){
                                Log.d("ViewModel", "No species classes found.")
                                _selectedClassId.value = null // Đảm bảo không có class nào được chọn
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
            // speciesPagingDataFlow sẽ tự động cập nhật nhờ flatMapLatest
        }
    }

    // Phương thức handleListResult không còn cần thiết vì Paging 3 có LoadState riêng.
    /*init{
        viewModelScope.launch(Dispatchers.IO) {
            getLocalizedSpeciesClassUseCase.getAll()
                .catch {
                    e -> _uiState.value = SpeciesScreenUiState.Error(e.localizedMessage ?: "Error")
                }
                .collect{
                    when (it){
                        is DataResult.Success -> {
                            _speciesClassList.value = it.data
                        }
                        is DataResult.Error -> {
                        }
                        is DataResult.Loading -> {
                        }
                    }
                }
            if (_speciesClassList.value.isNotEmpty()){
                getLocalizedSpeciesUseCase.getByClass(
                    value = _speciesClassList.value.first().id,
                    sortByName = true
                )
                    .catch { e -> _uiState.value = SpeciesScreenUiState.Error(e.localizedMessage ?: "Error") }
                    .collect {

                        handleListResult(it) }
            }
        }
    }

    private fun handleListResult(result: DataResult<List<DisplayableSpecies>>) {
        when (result) {
            is DataResult.Success -> {

                _uiState.value = if (result.data.isEmpty()) {
                    SpeciesScreenUiState.Empty
                } else {
                    Log.d("a",result.data[0].toString())
                    SpeciesScreenUiState.Success(result.data)

                }
            }
            is DataResult.Error -> {
                _uiState.value = SpeciesScreenUiState.Error(result.exception.localizedMessage ?: "Failed to load data")
            }
            is DataResult.Loading -> {
                _uiState.value = SpeciesScreenUiState.Loading
            }

        }
    }*/
}