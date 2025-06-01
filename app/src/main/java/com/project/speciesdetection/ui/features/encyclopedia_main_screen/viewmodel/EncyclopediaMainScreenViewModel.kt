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
) : ViewModel() {

    companion object {
        private const val TAG = "EncyclopediaVM" // Tag cho Log
        private const val SEARCH_DEBOUNCE_MS = 700L // Thời gian chờ trước khi thực hiện search
    }

    // StateFlow cho query tìm kiếm từ người dùng
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // StateFlow cho danh sách các species class (dùng cho filter chips)
    private val _speciesClassList = MutableStateFlow<List<DisplayableSpeciesClass>>(emptyList())
    val speciesClassList: StateFlow<List<DisplayableSpeciesClass>> = _speciesClassList.asStateFlow()

    // StateFlow cho classId đang được chọn (mặc định là "0" - Tất cả)
    private val _selectedClassId = MutableStateFlow<String?>("0")
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

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

            // Dựa vào classId để gọi UseCase tương ứng
            if (classId == "0") { // "0" đại diện cho "Tất cả các class"
                Log.d(TAG, "Fetching ALL paged species with query: '$query'")
                getLocalizedSpeciesUseCase.getAll(searchQuery = query)
            } else { // Lọc theo một classId cụ thể
                Log.d(TAG, "Fetching paged species for ClassId: $classId, query: '$query'")
                getLocalizedSpeciesUseCase.getByClassPaged(
                    classIdValue = classId,
                    searchQuery = query
                )
            }
        }.cachedIn(viewModelScope) // cachedIn rất quan trọng để Paging 3 hoạt động đúng và giữ dữ liệu khi xoay màn hình

    init {
        viewModelScope.launch {
            loadInitialSpeciesClasses()
        }
    }



    // Load danh sách các species class (ví dụ cho filter chips)
    private fun loadInitialSpeciesClasses() {
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