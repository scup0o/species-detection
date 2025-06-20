package com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel

import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.google.firebase.Timestamp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
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
import kotlinx.coroutines.flow.collect
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class) // Cho flatMapLatest và debounce
@HiltViewModel // Đánh dấu để Hilt có thể inject ViewModel này
class EncyclopediaMainScreenViewModel @Inject constructor(
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val getLocalizedSpeciesClassUseCase: GetLocalizedSpeciesClassUseCase,
    private val observationRepository : ObservationRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "EncyclopediaVM" // Tag cho Log
        private const val SEARCH_DEBOUNCE_MS = 700L // Thời gian chờ trước khi thực hiện search
    }

    // State lưu trữ giá trị dateFound cho species
    private val _speciesDateFound = MutableStateFlow<Map<String, Timestamp?>>(emptyMap()) // Map để lưu trữ dateFound theo speciesId
    val speciesDateFound: StateFlow<Map<String, Timestamp?>> = _speciesDateFound.asStateFlow()

    val currentLanguageState = MutableStateFlow("none")

    // StateFlow cho query tìm kiếm từ người dùng
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // StateFlow cho danh sách các species class (dùng cho filter chips)
    private val _speciesClassList = MutableStateFlow<List<DisplayableSpeciesClass>>(emptyList())
    val speciesClassList: StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()

    // StateFlow cho classId đang được chọn (mặc định là "0" - Tất cả)
    private val _selectedClassId = MutableStateFlow<String?>("0")
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    private val _currentUser = MutableStateFlow(userRepository.getCurrentUser())
    val currentUser =_currentUser.asStateFlow()

    val _init = MutableStateFlow(true)
    val init = _init.asStateFlow()

    // Flow chính cung cấp PagingData cho UI
    val speciesPagingDataFlow: Flow<PagingData<DisplayableSpecies>> =
        combine( // Kết hợp các Flow đầu vào
            _selectedClassId,
            _searchQuery.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(), // Debounce và chỉ emit khi query thay đổi
        ) { classId, query ->
            // Tạo một Triple hoặc Pair để chứa các giá trị đã kết hợp
            Pair(classId, query)
        }.flatMapLatest { (classId, query) -> // flatMapLatest để hủy request cũ khi có giá trị mới
            Log.d(TAG, "flatMapLatest triggered. ClassId: $classId, Query: '$query'")

            // Nếu không có mạng và chúng ta đang cố gắng load dữ liệu (classId không null),
            // trả về PagingData rỗng để UI có thể xử lý (hiển thị thông báo, không loading vô hạn).
            /*if (netStatus != ConnectivityObserver.Status.Available && classId != null) {
                Log.w(TAG, "Network unavailable, returning empty PagingData for species list.")
                return@flatMapLatest flowOf(PagingData.empty<DisplayableSpecies>())
            }*/

            if (classId == null) { // Trường hợp classId là null (không nên xảy ra nếu có giá trị mặc định "0")
                Log.w(TAG, "ClassId is null, returning empty PagingData.")
                return@flatMapLatest flowOf(PagingData.empty<DisplayableSpecies>())
            }

            val currentUser = userRepository.getCurrentUser()

            // Dựa vào classId để gọi UseCase tương ứng
            if (classId == "0") { // "0" đại diện cho "Tất cả các class"
                Log.d(TAG, "Fetching ALL paged species with query: '$query'")
                getLocalizedSpeciesUseCase.getAll(searchQuery = query, currentUser?.uid
                    )
            } else { // Lọc theo một classId cụ thể
                Log.d(TAG, "Fetching paged species for ClassId: $classId, query: '$query'")
                getLocalizedSpeciesUseCase.getByClassPaged(
                    classIdValue = classId,
                    searchQuery = query,
                    uid = currentUser?.uid
                )
            }
        }
            .cachedIn(viewModelScope) // cachedIn rất quan trọng để Paging 3 hoạt động đúng và giữ dữ liệu khi xoay màn hình

    init {
        viewModelScope.launch {

            loadInitialSpeciesClasses()

        }
    }

    fun getSpeciesListState(speciesList: List<DisplayableSpecies>, uid : String){
        viewModelScope.launch {
            _init.value = true
            clearObservationState()
            observationRepository.getObservationsStateForSpeciesList(speciesList, uid)
                .forEach{ pair ->
                    Log.i("a",pair.toString())
                    _speciesDateFound.value = _speciesDateFound.value.toMutableMap().apply {
                        put(pair.key, pair.value)
                    }
                }

            Log.i("a", _speciesDateFound.value.toString())
            _init.value = false
            observer(uid)

        }
    }


    // Load danh sách các species class (ví dụ cho filter chips)
    fun loadInitialSpeciesClasses() {
        // Kiểm tra lại mạng trước khi gọi API
        viewModelScope.launch(Dispatchers.IO) { // Sử dụng Dispatchers.IO cho các tác vụ mạng/DB
            Log.d(TAG, "Loading initial species classes...")
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

    // Xử lý sự kiện khi người dùng chọn một class khác
    fun selectSpeciesClass(classId: String) {
        Log.d(TAG, "selectSpeciesClass called with ID: $classId")
        if (_selectedClassId.value != classId) {
            _selectedClassId.value = classId
            // PagingDataFlow sẽ tự động cập nhật nhờ `combine` và `flatMapLatest`
        }
    }

    // Xử lý sự kiện khi query tìm kiếm thay đổi
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        // PagingDataFlow sẽ tự động cập nhật nhờ `combine`, `debounce` và `flatMapLatest`
    }

    fun setLanguage(language : String){
        currentLanguageState.value = language
    }

    fun observer(userId: String){
        viewModelScope.launch {
            observeAllUserObservationsChanges(userId).collect{ change->
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

                            // Chỉ cập nhật nếu chưa có hoặc timestamp khác
                            if (currentTimestamp != timestamp) {
                                currentMap[id] = timestamp
                                _speciesDateFound.value = currentMap
                                Log.i("ObserveChange", "Updated: $id -> $timestamp")
                            }
                        }
                    }

                    is ObservationChange.Removed -> {
                        val id = change.observationId
                        Log.i("checkObserve0,", currentMap.toString())
                        if (currentMap.containsKey(id)) {
                            currentMap.remove(id)
                            _speciesDateFound.value = currentMap
                            Log.i("ObserveChange", "Removed: $id")
                        }
                    }
                }
                Log.i("á", "${change}")
            }

        }
    }
    

    fun observeAllUserObservationsChanges(userId: String): Flow<ObservationChange> {
        return observationRepository.getObservationChangesForUser(userId)
    }

    fun observeDateFoundForUidAndSpecies(uid: String, speciesId: String,) {
        if (uid.isNotEmpty())
            viewModelScope.launch {
                observationRepository.checkUserObservationState(uid,speciesId) { dateFound ->
                    if (dateFound != null) {
                        _speciesDateFound.value = _speciesDateFound.value.toMutableMap().apply {
                            put(speciesId, dateFound)
                        }
                    } else {
                    }
                }
            }

    }

    fun clearObservationState(){
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