package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
// import androidx.lifecycle.viewmodel.compose.viewModel // Không cần import này trong ViewModel
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.repository.UserRepository
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
    private val observationRepository : ObservationRepository,
    private val savedStateHandle: SavedStateHandle,
    private val getLocalizedSpeciesUseCase: GetLocalizedSpeciesUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val species: DisplayableSpecies) : UiState()
        data class Error(val message: String) : UiState()
    }

    // Biến Json nên là private hoặc internal nếu không cần truy cập từ bên ngoài
    private val json = Json { ignoreUnknownKeys = true }

    private val _speciesDateFound = MutableStateFlow<Timestamp?>(null) // Map để lưu trữ dateFound theo speciesId
    val speciesDateFound: StateFlow<Timestamp?> = _speciesDateFound.asStateFlow()


    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Thay đổi kiểu của _sourceList thành List<SourceInfoItem>
    private val _sourceList = MutableStateFlow<List<SourceInfoItem>>(emptyList())
    val sourceList: StateFlow<List<SourceInfoItem>> = _sourceList.asStateFlow()

    // Biến đếm để theo dõi thứ tự thêm vào
    private var sourceOrderCounter = 0 // Bắt đầu từ 0 hoặc 1 tùy bạn muốn
    private var observationListener: ListenerRegistration? = null

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
                            localizedClass = baseSpecies.localizedClass,
                            //haveObservation = baseSpecies.haveObservation,
                            //firstFound = baseSpecies.firstFound
                            // Bạn có thể muốn copy thêm các trường khác từ baseSpecies nếu cần
                        )
                    )

                    val currentUser = userRepository.getCurrentUser()
                    if (currentUser!=null){
                        observeDateFoundForUidAndSpecies(detailedSpecies.id, currentUser.uid)
                    }


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

    fun observeDateFoundForUidAndSpecies(speciesId: String, uid : String) {
        if (uid.isNotEmpty())
            viewModelScope.launch {
            }
            viewModelScope.launch {
                observationListener?.remove()

                // Bắt đầu lắng nghe và lưu lại listener registration
                observationListener =
                observationRepository.checkUserObservationState(uid,speciesId) { dateFound ->

                        Log.i("obs", dateFound.toString())
                        _speciesDateFound.value = dateFound

                }
            }

    }

    fun addInfoPairsToSourceList() {
        val currentState = _uiState.value

        if (currentState is UiState.Success) {
            val species = currentState.species
            val newSourceItems = mutableListOf<SourceInfoItem>()

            fun processList(list: List<String>, listNameForLog: Int) {
                if (list.size >= 2) {
                    // Duyệt ngược từ phần tử gần cuối (list.size - 2) đến index = 1
                    for (i in list.size - 2 downTo 1 step 2) {
                        val first = list.getOrNull(i)
                        val second = list.getOrNull(i + 1)

                        if (first != null && second != null) {
                            sourceOrderCounter++
                            val newItem = SourceInfoItem(
                                firstValue = first,
                                secondValue = second,
                                listIndex = listNameForLog,
                                orderAdded = sourceOrderCounter
                            )
                            newSourceItems.add(newItem)
                            Log.d("AddInfoPairs", "Prepared item for $listNameForLog: $newItem")
                        }
                    }
                } else {
                    Log.w("AddInfoPairs", "$listNameForLog does not have enough elements. Size: ${list.size}")
                }
            }

            processList(species.localizedSummary, 0)
            processList(species.localizedPhysical, 2)
            processList(species.localizedDistribution, 3)
            processList(species.localizedHabitat, 4)
            processList(species.localizedBehavior, 5)

            if (newSourceItems.isNotEmpty()) {
                _sourceList.update { currentList ->
                    currentList + newSourceItems
                }
                Log.i("AddInfoPairs", "Updated _sourceList with ${newSourceItems.size} new items. Total size: ${_sourceList.value.size}")
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

    fun clearObservationState(){
        _speciesDateFound.value = null
    }

    override fun onCleared() {
        super.onCleared() // Luôn gọi super.onCleared()

        // 3. Gỡ bỏ listener để tránh memory leak.
        // Kiểm tra null để đảm bảo an toàn.
        observationListener?.remove()

        // Log để xác nhận việc dọn dẹp đã xảy ra
        Log.d("ViewModelLifecycle", "SpeciesDetailViewModel cleared and listener removed.")
    }


}