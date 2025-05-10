package com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species.repository.SpeciesRepository
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.species_class.SpeciesClass
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesClassUseCase
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EncyclopediaMainScreenViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val getLocalizedSpeciesClassUseCase: GetLocalizedSpeciesClassUseCase
) : ViewModel() {

    sealed interface SpeciesScreenUiState {
        data object Loading : SpeciesScreenUiState
        data class Success(val speciesList: List<DisplayableSpecies>) : SpeciesScreenUiState
        data class DetailSuccess(val species: DisplayableSpecies) : SpeciesScreenUiState
        data class Error(val message: String) : SpeciesScreenUiState
        data object Empty : SpeciesScreenUiState
    }

    private val _uiState = MutableStateFlow<SpeciesScreenUiState>(SpeciesScreenUiState.Loading)
    val uiState: StateFlow<SpeciesScreenUiState> = _uiState.asStateFlow()

    private val _speciesClassList = MutableStateFlow(emptyList<DisplayableSpeciesClass>())
    val speciesClassList : StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()

    init{
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
                    targetClassName = _speciesClassList.value.first().localizedName,
                    sortByName = true
                )
                    .catch { e -> _uiState.value = SpeciesScreenUiState.Error(e.localizedMessage ?: "Error") }
                    .collect {
                        //Log.d("a",it.toString())
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
    }
}