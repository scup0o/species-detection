package com.project.speciesdetection.ui.features.community_main_screen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ObservationListViewModel @Inject constructor(
    private val repository: ObservationRepository
) : ViewModel() {

    val hotObservations: Flow<PagingData<Observation>> =
        repository.getHotObservationsPager().flow.cachedIn(viewModelScope)
}