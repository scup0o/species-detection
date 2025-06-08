package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import androidx.lifecycle.viewmodel.compose.viewModel // Không cần import này trong ViewModel
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

// Data class đã định nghĩa ở Bước 1
data class SourceInfoItem(
    val firstValue: String,
    val secondValue: String,
    val listIndex: Int,
    val orderAdded: Int
)

@HiltViewModel
class EncyclopediaDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val species: DisplayableSpecies) : UiState()
        data class Error(val message: String) : UiState()
    }

    // Biến Json nên là private hoặc internal nếu không cần truy cập từ bên ngoài
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Thay đổi kiểu của _sourceList thành List<SourceInfoItem>
    private val _sourceList = MutableStateFlow<List<SourceInfoItem>>(emptyList())
    val sourceList: StateFlow<List<SourceInfoItem>> = _sourceList.asStateFlow()

    // Biến đếm để theo dõi thứ tự thêm vào
    private var sourceOrderCounter = 0 // Bắt đầu từ 0 hoặc 1 tùy bạn muốn

    init {
        getSpeciesDetailed()
    }

    fun getSpeciesDetailed() {
        _uiState.value = UiState.Loading
        val baseSpeciesJson = savedStateHandle.get<String>("baseSpeciesJson")
        val speciesJson = baseSpeciesJson?.let { Uri.decode(it) }
        val baseSpecies = speciesJson?.let {
            try {
                json.decodeFromString<DisplayableSpecies>(it)
            } catch (e: Exception) {
                Log.e("EncyclopediaDetailVM", "Error decoding baseSpeciesJson", e) // Log lỗi rõ ràng hơn
                null
            }
        }

        if (baseSpecies == null) {
            _uiState.value = UiState.Error("Could not load species data.")
            return
        }

        viewModelScope.launch {
            try {
                val detailedSpecies = getLocalizedSpeciesUseCase.getDetailsByDocId(baseSpecies.id)
                if (detailedSpecies != null) {
                    _uiState.value = UiState.Success(
                        detailedSpecies.copy(
                            localizedClass = baseSpecies.localizedClass
                            // Bạn có thể muốn copy thêm các trường khác từ baseSpecies nếu cần
                        )
                    )
                    // Reset bộ đếm và sourceList trước khi thêm mới (nếu hàm này có thể được gọi lại)
                    sourceOrderCounter = 0
                    _sourceList.value = emptyList() // Xóa danh sách cũ
                    addInfoPairsToSourceList() // Gọi sau khi UiState.Success được thiết lập
                } else {
                    _uiState.value = UiState.Error("Could not fetch species details for ID: ${baseSpecies.id}")
                }
            } catch (e: Exception) {
                Log.e("EncyclopediaDetailVM", "Error fetching species details", e)
                _uiState.value = UiState.Error("An error occurred while fetching species details.")
            }
        }
    }

    fun addInfoPairsToSourceList() {
        val currentState = _uiState.value

        if (currentState is UiState.Success) {
            val species = currentState.species
            // Không cần newPairsToAdd tạm thời nữa, chúng ta sẽ cập nhật _sourceList trực tiếp
            // hoặc tạo danh sách mới mỗi lần processList để giữ thứ tự.
            // Cách an toàn hơn là tạo một list tạm thời rồi update _sourceList một lần.
            val newSourceItems = mutableListOf<SourceInfoItem>()

            // Hàm processList giờ sẽ tạo SourceInfoItem
            fun processList(list: List<String>, listNameForLog: Int) {
                if (list.size >= 2) {
                    val secondLastItem = list[list.size - 2]
                    val lastItem = list[list.size - 1]
                    sourceOrderCounter++ // Tăng bộ đếm thứ tự
                    val newItem = SourceInfoItem(
                        firstValue = secondLastItem,
                        secondValue = lastItem,
                        listIndex = listNameForLog,
                        orderAdded = sourceOrderCounter // Gán số thứ tự hiện tại
                    )
                    newSourceItems.add(newItem)
                    Log.d("AddInfoPairs", "Prepared item for $listNameForLog: $newItem")
                } else {
                    Log.w("AddInfoPairs", "$listNameForLog does not have at least 2 elements. Size: ${list.size}")
                }
            }

            // Thứ tự gọi processList sẽ quyết định `orderAdded`
            processList(species.localizedSummary, 0)
            processList(species.localizedPhysical, 2)
            processList(species.localizedDistribution, 3)
            processList(species.localizedHabitat, 4)
            processList(species.localizedBehavior, 5)
            // Thêm các danh sách khác nếu cần

            if (newSourceItems.isNotEmpty()) {
                // Cập nhật _sourceList với tất cả các item mới đã thu thập
                _sourceList.update { currentList ->
                    currentList + newSourceItems // Nối danh sách item mới vào danh sách hiện tại
                }
                Log.i("AddInfoPairs", "Updated _sourceList with ${newSourceItems.size} new items. Total size: ${(_sourceList.value).size}")
            } else {
                Log.i("AddInfoPairs", "No new items to add to _sourceList.")
            }
        }
    }

    fun updateSaveStated(imageUri : Uri?){
        val currentState = _uiState.value
        if (currentState is UiState.Success)
        {
            savedStateHandle["speciesId"] = currentState.species.id
            savedStateHandle["speciesName"] = currentState.species.localizedName
            savedStateHandle["speciesSN"] = currentState.species.getScientificName()
            savedStateHandle["imageUri"] = imageUri
        }

    }


}