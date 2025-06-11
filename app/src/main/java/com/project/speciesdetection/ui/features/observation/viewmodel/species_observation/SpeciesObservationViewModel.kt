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

@HiltViewModel
class SpeciesObservationViewModel @Inject constructor(
    private val mapService : GeocodingService,
    private val repository: ObservationRepository // Giả sử tên là repository
) : ViewModel() {

    enum class ViewMode {
        LIST,
        MAP
    }

    // --- State quản lý các bộ lọc và tab ---

    // State để lưu tab đang được chọn (0: Tất cả, 1: Của tôi)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // <-- THAY ĐỔI: Tên biến rõ ràng hơn
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // State để lưu các tham số lọc (speciesId và userId)
    private val _filterParams = MutableStateFlow<Pair<String, String?>>(Pair("", null))

    // --- State quản lý các thay đổi real-time ---

    // Map để lưu trữ các observation đã được cập nhật, thêm, hoặc xóa.
    // Key là ID, Value là Observation. Nếu value là null, nghĩa là nó đã bị xóa.
    private val _updatedObservations = MutableStateFlow<Map<String, Observation?>>(emptyMap())
    val updatedObservations: StateFlow<Map<String, Observation?>> = _updatedObservations.asStateFlow()


    // --- Flow chính cung cấp dữ liệu Paging cho UI ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val observationPagingData: Flow<PagingData<Observation>> = combine(
        _filterParams,
        _selectedTab
    ) { params, tabIndex ->
        val speciesId = params.first
        val currentUserId = params.second
        val queryUid = if (tabIndex == 1) currentUserId else null
        Pair(speciesId, queryUid)
    }
        .filter { it.first.isNotBlank() } // Chỉ bắt đầu khi có speciesId
        .distinctUntilChanged() // Chỉ trigger khi filter hoặc tab thực sự thay đổi
        .flatMapLatest { (speciesId, uid) ->
            Log.d("ViewModel", "Tab/Filter changed. Clearing updatedObservations.")
            _updatedObservations.value = emptyMap()
            // flatMapLatest sẽ tự động hủy và tạo PagingSource cũ, tạo cái mới
            // khi tab hoặc filter thay đổi.
            repository.getObservationPager(speciesId = speciesId, uid = uid) // Giả sử có một hàm chung này

        }.cachedIn(viewModelScope) // cacheIn là bắt buộc!

    // --- Luồng dữ liệu cho chế độ MAP (Tải tất cả) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val allObservationsForMap: StateFlow<List<Observation>> = combine(
        _filterParams,
        _selectedTab,
        _viewMode
    ) { params, tabIndex, mode ->
        Triple(params, tabIndex, mode)
    }
        .filter { (params, _, mode) ->
            // Chỉ kích hoạt luồng này khi ở chế độ MAP và có speciesId
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
            initialValue = emptyList() // Giá trị ban đầu là danh sách rỗng
        )
    init {
        // <-- THAY ĐỔI: Chuyển toàn bộ logic lắng nghe vào đây
        viewModelScope.launch {
            // Luồng này sẽ quyết định cần lắng nghe những gì
            combine(_filterParams, _selectedTab) { params, tabIndex ->
                Pair(params, tabIndex)
            }
                .distinctUntilChanged()
                .collect { (params, tabIndex) ->
                    val (speciesId, currentUserId) = params

                    // DỌN DẸP TRẠNG THÁI CŨ KHI CHUYỂN TAB
                    _updatedObservations.value = emptyMap()

                    if (speciesId.isNotBlank()) {
                        if (tabIndex == 1 && currentUserId != null) {
                            // --- Chế độ "CỦA TÔI": Lắng nghe tất cả thay đổi ---
                            Log.d("ViewModel", "Listening for MY observations. UID: $currentUserId")
                            startListeningForChanges(speciesId, currentUserId)
                        } else {
                            // --- Chế độ "TẤT CẢ": Chỉ lắng nghe Sửa/Xóa, KHÔNG Thêm ---
                            Log.d("ViewModel", "Listening for ALL observations (Modify/Remove only).")
                            // Chúng ta sẽ có một hàm mới để làm việc này
                            // Hiện tại, chúng ta có thể tạm thời không làm gì để ngăn lỗi
                            // Hoặc triển khai logic chỉ lắng nghe Sửa/Xóa (phức tạp hơn)

                            // GIẢI PHÁP ĐƠN GIẢN NHẤT: Không lắng nghe real-time ở tab "Tất cả"
                            // Nếu muốn có listener, xem phần nâng cao bên dưới.
                        }
                    }
                }
        }
    }

    // Hàm trợ giúp để lắng nghe thay đổi (Thêm/Sửa/Xóa)
    private var currentListenerJob: Job? = null
    private fun startListeningForChanges(speciesId: String, uid: String?) {
        currentListenerJob?.cancel() // Hủy job listener cũ
        currentListenerJob = viewModelScope.launch {
            repository.listenToObservationChanges(speciesId, uid)
                .collect { changes ->
                    // Logic xử lý `_updatedObservations` của bạn giữ nguyên
                    _updatedObservations.update { currentMap ->
                        val newMap = currentMap.toMutableMap()
                        changes.forEach { change ->
                            when (change) {
                                //is ObservationChange.Added -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Modified -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Removed -> newMap[change.observationId] = null // Đánh dấu là đã xóa
                                else -> {}
                            }
                        }
                        newMap
                    }
                }
        }
    }

    /*init {
        // Bắt đầu lắng nghe các thay đổi real-time ngay khi ViewModel được tạo

        //listenForRealtimeUpdates()





    }*/

    // --- Các hàm được gọi từ UI ---

    /**
     * Được gọi từ UI khi màn hình được tạo hoặc khi tham số thay đổi.
     */
    fun setFilters(speciesId: String, currentUserId: String?) {
        _filterParams.value = Pair(speciesId, currentUserId)
    }

    /**
     * Được gọi từ UI khi người dùng chuyển tab.
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    // <-- THAY ĐỔI: Hàm chọn chế độ xem
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
        return formatter.format(date)
    }


    // --- Logic lắng nghe và xử lý thay đổi ---

    /*@OptIn(ExperimentalCoroutinesApi::class)
    fun listenForRealtimeUpdates() {
        viewModelScope.launch {
            // Kết hợp các filter để biết cần lắng nghe câu truy vấn nào
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
                    // Xử lý các thay đổi nhận được từ Flow
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