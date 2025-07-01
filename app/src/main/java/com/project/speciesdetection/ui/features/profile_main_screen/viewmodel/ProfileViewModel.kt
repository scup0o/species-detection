package com.project.speciesdetection.ui.features.profile_main_screen.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.project.speciesdetection.core.services.backend.message.MessageApiService
import com.project.speciesdetection.core.services.backend.user.UserApiService
import com.project.speciesdetection.core.services.content_moderation.ContentModerationService
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ObservationChange
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.data.model.user.repository.RemoteUserRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named

sealed class ProfileUiState {
    object Init : ProfileUiState()
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
sealed class UiEvent {
    object  UpdateProfileSuccess : UiEvent()
    data class ShowError(val message: String): UiEvent()
}


@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: RemoteUserRepository,
    private val mapService : GeocodingService,
    @Named("language_provider") languageProvider: LanguageProvider,
    private val repository: ObservationRepository,
    private val messageApiService: MessageApiService,
    private val userApiService: UserApiService,
    private val contentModerationService: ContentModerationService,
    @ApplicationContext private val context: Context,
): ViewModel(){

    private val _newCroppedPhotoUri = MutableStateFlow<Uri?>(null)
    val newCroppedPhotoUri: StateFlow<Uri?> = _newCroppedPhotoUri.asStateFlow()
    private val _newNameInDialog = MutableStateFlow("")
    val newNameInDialog: StateFlow<String> = _newNameInDialog.asStateFlow()

    // Hàm này được gọi khi dialog được mở
    fun onEditProfileDialogOpened() {
        _newNameInDialog.value = user.value.name ?: ""
        _newCroppedPhotoUri.value = null // Reset ảnh mỗi khi mở dialog
    }
    fun onNameInDialogChanged(name: String) {
        _newNameInDialog.value = name
    }
    fun onPhotoCropped(temporaryUri: Uri?) {
        if (temporaryUri == null) {
            _newCroppedPhotoUri.value = null
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val stableUri = createStableCopy(temporaryUri)
            _newCroppedPhotoUri.value = stableUri
        }
    }

    private fun createStableCopy(sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            // Tạo file trong thư mục cache
            val destinationFile = File.createTempFile("profile_crop_", ".jpg", context.cacheDir)

            inputStream?.use {  input: InputStream ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Tạo Uri ổn định bằng FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Phải khớp với authorities trong Manifest
                destinationFile
            )
        } catch (e: Exception) {
            Log.e("STABLE_COPY_ERROR", "Failed to create stable copy", e)
            null
        }
    }

    private fun clearCroppedPhotoUri() {
        _newCroppedPhotoUri.value = null
        _newNameInDialog.value = ""
    }

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Init)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow(User())
    val user = _user.asStateFlow()

    private val _selectedTab = MutableStateFlow(1)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow(languageProvider.getCurrentLanguageCode())
    val currentLanguage = _currentLanguage.asStateFlow()

    private val _sortByDesc = MutableStateFlow(true)
    val sortByDesc = _sortByDesc.asStateFlow()

    private val _selfCheck = MutableStateFlow(true)



    fun updateSortDirection(){
        _sortByDesc.value = !_sortByDesc.value
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    val observationCount: StateFlow<Int> = user
        .filter { it.uid.isNotBlank() }
        .flatMapLatest { currentUser ->
            repository.listenToUserObservationCount(currentUser.uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    enum class SortOrder {
        BY_NAME_ASC, // Sắp xếp theo tên tăng dần (A-Z)
        BY_NAME_DESC // Sắp xếp theo tên giảm dần (Z-A)
        // Bạn có thể bỏ NONE nếu không cần trạng thái "không sắp xếp"
    }

    // 1. Đổi tên biến và đặt giá trị mặc định là sắp xếp theo tên A-Z
    private val _speciesSortState = MutableStateFlow(SortOrder.BY_NAME_ASC) // <-- THAY ĐỔI Ở ĐÂY
    val speciesSortState: StateFlow<SortOrder> = _speciesSortState.asStateFlow() // <-- VÀ Ở ĐÂY

    /**
     * Hàm này được gọi từ UI để thay đổi cách sắp xếp.
     */
    fun setSortOrder(newOrder: SortOrder) {
        _speciesSortState.value = newOrder // <-- CẬP NHẬT Ở ĐÂY
    }

    val observedSpecies: StateFlow<List<DisplayableSpecies>> = user
        .filter { it.uid.isNotBlank() }
        .flatMapLatest { currentUser ->
            val speciesFlow = repository.listenToUserObservedSpecies(currentUser.uid)

            // 2. Kết hợp với `_speciesSortState` đã được đổi tên
            combine(speciesFlow, _speciesSortState) { speciesList, sortOrder -> // <-- CẬP NHẬT Ở ĐÂY
                when (sortOrder) {
                    SortOrder.BY_NAME_ASC -> speciesList.sortedBy { it.localizedName }
                    SortOrder.BY_NAME_DESC -> speciesList.sortedByDescending { it.localizedName }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _updatedObservations = MutableStateFlow<Map<String, Observation?>>(emptyMap())
    val updatedObservations: StateFlow<Map<String, Observation?>> = _updatedObservations.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val observationPagingData: Flow<PagingData<Observation>> = combine(
        user,
        selectedTab,
        sortByDesc
    ) { user, tabIndex, sortByDesc ->
        // Chỉ lấy PagingData khi không ở tab Species
        if (tabIndex == 2) {
            return@combine null
        }
        val currentUserId = user.uid
        val query = when (tabIndex) {
            1 -> "normal"
            3 -> "save"
            4 -> "deleted"
            else -> ""
        }
        Triple(currentUserId, query, sortByDesc)
    }
        .filter { it != null && it.first.isNotBlank() && it.second != "" }
        .distinctUntilChanged()
        .flatMapLatest { triple ->
            val (uid, query, sortByDesc) = triple!!
            _updatedObservations.value = emptyMap()
            repository.getObservationPager(
                speciesId = "",
                uid = uid,
                userRequested = if (_selfCheck.value) uid else "none",
                queryByDesc = sortByDesc,
                state = query
            )
        }.cachedIn(viewModelScope)


    init {
        // Logic lắng nghe cũng cần được cập nhật
        viewModelScope.launch {
            combine(user, selectedTab) { user, tabIndex ->
                Pair(user, tabIndex)
            }
                .filter { it.first.uid.isNotBlank() }
                .distinctUntilChanged()
                .collect { (currentUser, tabIndex) ->
                    currentListenerJob?.cancel() // Hủy listener cũ mỗi khi user hoặc tab thay đổi
                    if (tabIndex != 2) { // Chỉ bắt đầu listener nếu không ở tab Species
                        val query = when (tabIndex) {
                            1 -> "normal"
                            3 -> "save"
                            4 -> "deleted"
                            else -> ""
                        }
                        if (query.isNotEmpty()) {
                            startListeningForChanges(currentUser.uid, query)
                        }
                    }
                }
        }
    }

    private var currentListenerJob: Job? = null
    private fun startListeningForChanges(uid: String?, query: String,) {
        currentListenerJob?.cancel()
        currentListenerJob =
            viewModelScope.launch(Dispatchers.IO) {
            repository.listenToObservationChanges(speciesId = "", uid = uid, userRequested = if (_selfCheck.value) uid?:"" else "none", queryByDesc = _sortByDesc.value, state = query)
                .collect { changes ->
                    _updatedObservations.update { currentMap ->
                        val newMap = currentMap.toMutableMap()
                        changes.forEach { change ->
                            when (change) {
                                //is ObservationChange.Added -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Modified -> newMap[change.observation.id!!] = change.observation
                                is ObservationChange.Removed -> newMap[change.observationId] = null
                                else -> {}
                            }
                        }
                        newMap
                    }
                }
        }
    }


    fun updateUser(user: User){
        _user.value = user
        Log.i("user",user.toString())
        _uiState.value=UiState.Success
    }

    fun retrieveUserInformation(uid: String){
        viewModelScope.launch(Dispatchers.IO) {
            _selfCheck.value=false
            val user = userRepository.getUserInformation(uid)
            if (user!=null){
                if (user.status=="disabled"){
                    _uiState.value=UiState.Error("disabled")
                    return@launch
                }
                _user.value=user
                _uiState.value=UiState.Success
            }
            else{
                Log.i("user",user.toString())
                _uiState.value=UiState.Error("not_found")
            }
        }
    }

    fun restoreArchivedObservation(observationId: String) {
        viewModelScope.launch {
            val result = repository.restoreObservation(observationId)
            if (result.isSuccess) {
                messageApiService.updateNotificationState(postId = observationId, state = "show")
                //Log.d("ProfileViewModel", "Observation $observationId restored successfully.")
            } else {
                Log.e("ProfileViewModel", "Failed to restore observation $observationId.")
            }
        }
    }

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()


    fun updateUserProfile(newName: String, newPhotoUri: Uri?) {
        if (_isUpdatingProfile.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isUpdatingProfile.value = true

            try {
                // --- 1. KIỂM TRA TÊN RỖNG ---
                if (newName.isBlank()) {
                    _eventFlow.emit(UiEvent.ShowError("name_empty"))
                    return@launch // Dừng hàm ngay lập tức
                }

                // --- 2. KIỂM DUYỆT NỘI DUNG TÊN ---
                // Giả sử ngôn ngữ mặc định là 'en' hoặc ngôn ngữ của người dùng
                val textModerationResult = contentModerationService.isTextAppropriate(newName, currentLanguage.value)
                if (textModerationResult.isFailure || !textModerationResult.getOrDefault(false)) {
                    val errorMessage = if (textModerationResult.isFailure) {
                        "moderation_error"
                    } else {
                        "name_unappropriated"
                    }
                    _eventFlow.emit(UiEvent.ShowError(errorMessage))
                    return@launch
                }

                Log.i("check", newPhotoUri.toString())

                if (newPhotoUri != null) {
                    val imageModerationResult = contentModerationService.isImageAppropriate(newPhotoUri)

                    if (imageModerationResult.isFailure || !imageModerationResult.getOrDefault(false)) {
                        val errorMessage = if (imageModerationResult.isFailure) {
                            "moderation_error"
                        } else {
                            "images_unappropriated"
                        }
                        _eventFlow.emit(UiEvent.ShowError(errorMessage))
                        return@launch
                    }
                }

                val result = userRepository.updateProfileInfo(
                    userId = _user.value.uid,
                    newName = newName,
                    newPhoto = newPhotoUri
                )

                if (result.isSuccess) {
                    val newUrl = result.getOrNull()
                    //Log.d("ProfileViewModel", "Profile updated successfully.")

                    val updateRequest = UserApiService.UpdateRequest(
                        userId = _user.value.uid,
                        updates = UserApiService.UserUpdates(name = newName, photoUrl = newUrl)
                    )
                    userApiService.updateUserDenormalize(updateRequest)

                    _eventFlow.emit(UiEvent.UpdateProfileSuccess)

                } else {
                    //Log.e("ProfileViewModel", "Failed to update profile.", result.exceptionOrNull())
                    _eventFlow.emit(UiEvent.ShowError("Cập nhật thông tin thất bại."))
                }

            } catch (e: Exception) {
                //Log.e("ProfileViewModel", "An unexpected error occurred: ${e.message}", e)
                _eventFlow.emit(UiEvent.ShowError("Đã xảy ra lỗi không mong muốn."))
            } finally {
                _isUpdatingProfile.value = false
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        currentListenerJob?.cancel()
    }
}