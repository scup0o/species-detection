package com.project.speciesdetection.ui.features.observation.viewmodel.detail

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.core.services.backend.message.MessageApiService
import com.project.speciesdetection.core.services.backend.message.NotificationTriggerRequest
import com.project.speciesdetection.core.services.content_moderation.ContentModerationService
import com.project.speciesdetection.core.services.map.GeocodingService
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.observation.repository.ListUpdate
import com.project.speciesdetection.data.model.observation.repository.ObservationRepository
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.data.model.user.repository.UserRepository
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.domain.usecase.species.GetLocalizedSpeciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

sealed class UiState {
    object Init : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object DeleteSuccess : UiEvent()
    object PostCommentSuccess : UiEvent()
}

@HiltViewModel
class ObservationDetailViewModel @Inject constructor(
    private val observationRepository: ObservationRepository,
    private val speciesUseCase : GetLocalizedSpeciesUseCase,
    private val messageApiService: MessageApiService,
    private val geocodingService: GeocodingService,
    private val userRepository: UserRepository,
    private val contentModerationService: ContentModerationService,
    @Named("language_provider") languageProvider: LanguageProvider
) : ViewModel() {

    private val currentUser = userRepository.getCurrentUser()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    private val _uiState = MutableStateFlow<UiState>(UiState.Init)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _observationState = MutableStateFlow<Observation?>(null)
    val observationState: StateFlow<Observation?> = _observationState.asStateFlow()

    private val _commentsState = MutableStateFlow<List<Comment>>(emptyList())
    val commentsState: StateFlow<List<Comment>> = _commentsState.asStateFlow()

    private val _commentsSpeciesState = MutableStateFlow<Map<String, DisplayableSpecies?>>(emptyMap())
    val commentsSpeciesState: StateFlow<Map<String, DisplayableSpecies?>> = _commentsSpeciesState.asStateFlow()

    private var originalComments: List<Comment> = emptyList()
    private val _commentSortState = MutableStateFlow(false) // false = oldest, true = newest
    val commentSortState = _commentSortState.asStateFlow()

    private var observationListenerJob: Job? = null
    private var commentListenerJob: Job? = null

    private val _currentLanguage = MutableStateFlow(languageProvider.getCurrentLanguageCode())
    val currentLanguage = _currentLanguage.asStateFlow()

    val newCommentSpecies = MutableStateFlow(DisplayableSpecies())

    fun updateNewCommentSpecies(species: DisplayableSpecies){
        newCommentSpecies.value = species
    }

    /*fun startListening(observationId: String, sortDescending: Boolean = false) {
        _uiState.value = UiState.Loading
        if (observationId.isEmpty()) {
            _uiState.value = UiState.Error("empty_id")
        } else {
            // Nếu đã có job lắng nghe, cancel job cũ
            commentListenerJob?.cancel()

            // Bắt đầu một job mới
            commentListenerJob = viewModelScope.launch {
                observationRepository.observeObservationWithComments(observationId, sortDescending).collect { (observation, comments) ->
                    if (observation == null) {
                        _uiState.value = UiState.Error("observation_not_found")
                        _observationState.value = null
                        _commentsState.value = emptyList()
                    } else {
                        if (observation.state == "normal") {
                            val locationName = getLocationName(
                                observation.location?.latitude ?: 0.0,
                                observation.location?.longitude ?: 0.0
                            )


                            _observationState.value = observation.copy(
                                locationName = locationName,
                            )


                            viewModelScope.launch(Dispatchers.Default) {
                                _commentsState.value = comments.sortedBy {
                                    it.timestamp
                                }.let {
                                    if (_commentSortState.value) it.reversed() else it
                                }}


                            _uiState.value = UiState.Success

                            if (!_commentsState.value.isNullOrEmpty()) {
                                getSpeciesList()
                            }
                        }
                        if (observation.state == "violated") {
                            _uiState.value = UiState.Error("violated")
                        }

                    }
                }
            }
        }
    }


    private fun getSpeciesList() {
        viewModelScope.launch {
            val uid = _observationState.value?.uid ?: return@launch

            // B1: Lấy tất cả commentId và speciesId (lọc null)
            val commentIdToSpeciesId = _commentsState.value.mapNotNull { comment ->
                val speciesId = comment.speciesId
                if (speciesId.isNotEmpty()) comment.id!! to speciesId else null
            }.toMap()

            // B2: Gọi 1 lần để lấy tất cả species
            val speciesIdList = commentIdToSpeciesId.values.distinct()
            val speciesList = speciesUseCase.getById(speciesIdList, uid)

            // B3: Map speciesId -> DisplayableSpecies
            val speciesMap = speciesList.associateBy { it.id }

            // B4: Tạo map commentId -> species?
            val commentSpeciesMap = commentIdToSpeciesId.mapValues { (_, speciesId) ->
                speciesMap[speciesId]
            }

            _commentsSpeciesState.value = commentSpeciesMap
        }
    }

    fun sortComment(){
        _commentSortState.value = !_commentSortState.value
        sortCommentsByTimestamp()
    }*/

    fun startListeningToObservation(observationId: String) {
        if (_uiState.value !is UiState.Init) return
        if (observationId.isEmpty()) {
            _uiState.value = UiState.Error("empty_id")
            return
        }
        _uiState.value = UiState.Loading
        observationListenerJob?.cancel()
        observationListenerJob = viewModelScope.launch {
            observationRepository.observeObservation(observationId).collect { observation ->
                if (observation == null) {
                    _uiState.value = UiState.Error("not_found")
                    _observationState.value = null
                    return@collect
                }
                // Lấy tên vị trí nếu cần (có thể cache để tránh gọi lại)
                var locationName = _observationState.value?.locationName ?: ""
                if (locationName.isEmpty() && observation.location != null) {
                    locationName = geocodingService.reverseSearch(
                        observation.location.latitude,
                        observation.location.longitude
                    )?.displayName ?: ""
                }
                _observationState.value = observation.copy(locationName = locationName)
                _uiState.value = UiState.Success
            }
        }
    }

    /**
     * Hàm này được gọi bởi ObservationCommentsSheet để lắng nghe comment.
     */
    fun startListeningToComments(observationId: String) {
        commentListenerJob?.cancel()
        commentListenerJob = viewModelScope.launch {
            observationRepository.observeCommentUpdates(observationId).collect { update ->
                processCommentUpdate(update)
                getSpeciesForNewComment(update)
            }
        }
    }



    //old

    fun startListening(observationId: String) {
        if (_uiState.value !is UiState.Init) return // Chỉ chạy nếu đang ở trạng thái ban đầu

        _uiState.value = UiState.Loading
        if (observationId.isEmpty()) {
            _uiState.value = UiState.Error("empty_id")
            return
        }

        // Hủy các job cũ để tránh leak
        observationListenerJob?.cancel()
        commentListenerJob?.cancel()

        // Bắt đầu lắng nghe Observation
        observationListenerJob = viewModelScope.launch {
            observationRepository.observeObservation(observationId).collect { observation ->
                if (observation == null) {
                    _uiState.value = UiState.Error("observation_not_found")
                    _observationState.value = null
                    return@collect
                }

                if (observation.state == "violated") {
                    _observationState.value = observation
                    _uiState.value = UiState.Error("violated")

                    return@collect
                }

                if (observation.privacy=="Private" && currentUser?.uid!=observation.uid){
                    _uiState.value = UiState.Error("private")
                    return@collect
                }
                var locationName = ""
                // Lấy tên vị trí và cập nhật state
                if (observation.location!=null){
                     locationName= getLocationName(
                        observation.location?.latitude ?: 0.0,
                        observation.location?.longitude ?: 0.0
                    )
                }
                _observationState.value = observation.copy(locationName = locationName)
                _uiState.value = UiState.Success // Đánh dấu UI đã sẵn sàng
            }
        }

        // Bắt đầu lắng nghe Comment Updates
        commentListenerJob = viewModelScope.launch {
            observationRepository.observeCommentUpdates(observationId).collect { update ->
                // Xử lý từng loại update một cách hiệu quả
                processCommentUpdate(update)
                // Lấy thông tin species cho các comment mới nếu cần
                getSpeciesForNewComment(update)
            }
        }
    }

    // === HÀM MỚI ĐỂ XỬ LÝ COMMENT UPDATE ===
    // Trong file ObservationDetailViewModel.kt

    private fun processCommentUpdate(update: ListUpdate<Comment>) {
        val currentList = originalComments.toMutableList()

        // Lấy id của item được cập nhật từ sealed class
        val updatedItemId = when(update) {
            is ListUpdate.Added -> update.item.id
            is ListUpdate.Modified -> update.item.id
            is ListUpdate.Removed -> update.item.id
        }

        // Tìm index của item trong danh sách hiện tại bằng id đó
        val existingIndex = currentList.indexOfFirst { it.id == updatedItemId }

        when (update) {
            is ListUpdate.Added -> {
                // Chỉ thêm nếu item chưa tồn tại trong danh sách
                if (existingIndex == -1) {
                    currentList.add(update.item)
                } else {
                    // Nếu đã tồn tại, có thể là một sự kiện cập nhật bị gửi dưới dạng ADDED
                    // hoặc listener kết nối lại. Ta cập nhật nó.
                    currentList[existingIndex] = update.item
                }
            }
            is ListUpdate.Modified -> {
                // Nếu tìm thấy, thay thế item cũ bằng item mới
                if (existingIndex != -1) {
                    currentList[existingIndex] = update.item
                }
            }
            is ListUpdate.Removed -> {
                // Nếu tìm thấy, xóa item khỏi danh sách
                if (existingIndex != -1) {
                    currentList.removeAt(existingIndex)
                }
            }
        }

        // Cập nhật lại danh sách gốc và áp dụng lại bộ lọc
        originalComments = currentList
        applySortToComments()
    }

    // === HÀM MỚI ĐỂ SORT HIỆU QUẢ ===
    private fun applySortToComments() {
        viewModelScope.launch(Dispatchers.Default) {
            val sortedList = if (_commentSortState.value) {
                originalComments.sortedByDescending { it.timestamp }
            } else {
                originalComments.sortedBy { it.timestamp }
            }
            _commentsState.value = sortedList
        }
    }

    // Hàm được gọi từ UI
    fun sortComment() {
        _commentSortState.value = !_commentSortState.value
        applySortToComments() // Chỉ cần sắp xếp lại danh sách gốc
    }

    // === CÁC HÀM CŨ ĐƯỢC GIỮ LẠI VÀ CHỈNH SỬA NHẸ ===

    private fun getSpeciesForNewComment(update: ListUpdate<Comment>) {
        // Chỉ xử lý cho comment mới được thêm và có speciesId
        if (update is ListUpdate.Added && update.item.speciesId.isNotEmpty()) {
            viewModelScope.launch {
                val species = speciesUseCase.getById(
                    listOf(update.item.speciesId,),
                    uid = currentUser?.uid
                ).firstOrNull()
                if (species != null) {
                    _commentsSpeciesState.value += (update.item.id!! to species)
                }
            }
        }
    }

    private fun sendActionNotification(
        actorUserId: String,
        actorUsername: String,
        targetUserId: String,
        postId: String,
        actionType: String,
        commentId: String? = null,
        content: String? = null
    ) {
        // Chỉ gửi thông báo nếu người hành động không phải là chủ sở hữu nội dung
        if (actorUserId == targetUserId) return

        viewModelScope.launch {
            try {
                val request = NotificationTriggerRequest(
                    actorUserId = actorUserId,
                    targetUserId = targetUserId,
                    postId = postId,
                    actionType = actionType,
                    commentId = commentId ?: "",
                    content = content ?: "",
                    actorUsername = "actorUsername"
                )
                messageApiService.sendNotificationTrigger(request)
            } catch (e: Exception) {
                // Log lỗi hoặc hiển thị một toast không quá quan trọng
                Log.e("NotificationError", "Failed to send notification: ${e.message}")
            }
        }
    }
    fun postCommentForList(observationId: String, authorId: String, comment: Comment) {
        viewModelScope.launch {
            // Gọi thẳng đến repository với các tham số đã được cung cấp
            val result = observationRepository.postComment(observationId, comment)
            result.onSuccess {
                //_toastMessage.emit("Đăng bình luận thành công")
                _eventFlow.emit(UiEvent.PostCommentSuccess)
                // Nếu người bình luận khác người đăng bài => gửi thông báo
                if (comment.userId != authorId) {
                    val request = NotificationTriggerRequest(
                        actorUserId = comment.userId,
                        targetUserId = authorId,
                        postId = observationId,
                        actionType = "comment",
                        commentId = it.id ?: "", // `it` là comment được trả về từ repository
                        content = comment.content,
                        actorUsername = "actorUsername" // Nên lấy từ authState
                    )
                    try {
                        messageApiService.sendNotificationTrigger(request)
                    } catch (e: Exception) {
                        _toastMessage.emit("Lỗi khi gửi thông báo: ${e.message}")
                    }
                }
            }.onFailure {
                _toastMessage.emit("Lỗi: ${it.message}")
            }
        }
    }
    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()
    fun postComment(comment: Comment) {
        if (_isPostingComment.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try{
                _isPostingComment.value=true
                val observation = _observationState.value
                val postId = observation?.id ?: return@launch
                val postAuthorId = observation.uid

                // --- 1. KIỂM DUYỆT VĂN BẢN ---
                if (comment.content.isNotBlank()) {
                    val textResult = contentModerationService.isTextAppropriate(comment.content, _currentLanguage.value)
                    if (textResult.isFailure || !textResult.getOrDefault(false)) {
                        val errorMessage = if(textResult.isFailure) "moderation_error" else "comment_inappropriate"
                        withContext(Dispatchers.Main) { _eventFlow.emit(UiEvent.ShowSnackbar(errorMessage)) }
                        return@launch
                    }
                }

                // --- 2. KIỂM DUYỆT HÌNH ẢNH (NẾU CÓ URI) ---
                val imageUri = comment.imageUrl
                if (imageUri.isNotEmpty()) {
                    val imageResult = contentModerationService.isImageAppropriate(Uri.decode(imageUri).toUri())
                    if (imageResult.isFailure || !imageResult.getOrDefault(false)) {
                        val errorMessage = if(imageResult.isFailure) "moderation_error" else "image_inappropriate"
                        withContext(Dispatchers.Main) { _eventFlow.emit(UiEvent.ShowSnackbar(errorMessage)) }
                        return@launch
                    }
                }

                // --- 3. NẾU MỌI THỨ HỢP LỆ, GỌI REPOSITORY ---
                // Repository sẽ tự xử lý việc tải ảnh lên từ comment.imageUri
                val result = observationRepository.postComment(postId, comment)

                result.onSuccess { newComment ->
                    withContext(Dispatchers.Main) {
                        //_toastMessage.emit("Đăng bình luận thành công")
                        _eventFlow.emit(UiEvent.PostCommentSuccess)
                    }

                    // Gửi thông báo
                    if (comment.userId != postAuthorId) {
                        val request = NotificationTriggerRequest(
                            actorUserId = comment.userId,
                            targetUserId = postAuthorId,
                            postId = postId,
                            actionType = "comment",
                            commentId = newComment.id ?: "",
                            content = comment.content,
                            actorUsername = "actorUsername"
                        )
                        try {
                            messageApiService.sendNotificationTrigger(request)
                        } catch (e: Exception) {
                            Log.e("NotificationError", "Lỗi khi gửi thông báo: ${e.message}")
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        _toastMessage.emit("Lỗi: ${it.message}")
                    }
                }
            }
            finally {
                _isPostingComment.value = false
            }
        }
    }

    fun deleteComment(observationId: String, commentId: String) {
        viewModelScope.launch {
            val result = observationRepository.deleteComment(observationId, commentId)
            result.onSuccess {
                //_toastMessage.emit("Đã xoá bình luận")
                messageApiService.updateNotificationState(commentId = commentId, state = "hide")
            }.onFailure {
                _toastMessage.emit("Lỗi khi xoá: ${it.message}")
            }
        }
    }

    fun likeComment(commentId: String, userId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val observationId = _observationState.value?.id ?: return@launch

            // 1. Lưu lại trạng thái gốc để hoàn tác nếu lỗi
            val originalComments = _commentsState.value
            val commentIndex = originalComments.indexOfFirst { it.id == commentId }
            if (commentIndex == -1) return@launch

            val commentToUpdate = originalComments[commentIndex]

            // 2. Tính toán trạng thái mới và cập nhật UI ngay lập tức
            val hasLiked = userId in commentToUpdate.likeUserIds
            val hasDisliked = userId in commentToUpdate.dislikeUserIds

            val newLikes = if (hasLiked) commentToUpdate.likeUserIds - userId else commentToUpdate.likeUserIds + userId
            val newDislikes = commentToUpdate.dislikeUserIds - userId
            val pointChange = when {
                hasLiked -> -1
                hasDisliked -> 2 // Bỏ dislike (+1) + thêm like (+1)
                else -> 1
            }

            val updatedComment = commentToUpdate.copy(
                likeUserIds = newLikes,
                dislikeUserIds = newDislikes,
                likeCount = commentToUpdate.likeCount + pointChange
            )

            val newCommentList = originalComments.toMutableList().also { it[commentIndex] = updatedComment }
            _commentsState.value = newCommentList

            try {
                observationRepository.toggleLikeComment(observationId, commentId, userId)
                if (!hasLiked) {
                    sendActionNotification(
                        actorUserId = userId,
                        actorUsername = "",
                        targetUserId = commentToUpdate.userId, // Người nhận là chủ comment
                        postId = observationId,
                        actionType = "like_comment",
                        commentId = commentId,
                        content = commentToUpdate.content
                    )
                }

            } catch (e: Exception) {
                // 4. Nếu lỗi, hoàn tác lại UI về trạng thái gốc
                _commentsState.value = originalComments
                _toastMessage.emit("Thao tác thích bình luận thất bại: ${e.message}")
            }
        }
    }

    fun dislikeComment(commentId: String, userId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val observationId = _observationState.value?.id ?: return@launch

            val originalComments = _commentsState.value
            val commentIndex = originalComments.indexOfFirst { it.id == commentId }
            if (commentIndex == -1) return@launch

            val commentToUpdate = originalComments[commentIndex]

            val hasLiked = userId in commentToUpdate.likeUserIds
            val hasDisliked = userId in commentToUpdate.dislikeUserIds

            val newDislikes = if (hasDisliked) commentToUpdate.dislikeUserIds - userId else commentToUpdate.dislikeUserIds + userId
            val newLikes = commentToUpdate.likeUserIds - userId
            val pointChange = when {
                hasDisliked -> 1
                hasLiked -> -2 // Bỏ like (-1) + thêm dislike (-1)
                else -> -1
            }

            val updatedComment = commentToUpdate.copy(
                likeUserIds = newLikes,
                dislikeUserIds = newDislikes,
                likeCount = commentToUpdate.likeCount + pointChange
            )

            val newCommentList = originalComments.toMutableList().also { it[commentIndex] = updatedComment }
            _commentsState.value = newCommentList

            try {
                observationRepository.toggleDislikeComment(observationId, commentId, userId)
                if (!hasDisliked) {
                    sendActionNotification(
                        actorUserId = userId,
                        actorUsername = "",
                        targetUserId = commentToUpdate.userId,
                        postId = observationId,
                        actionType = "dislike_comment", // Loại hành động
                        commentId = commentId,
                        content = commentToUpdate.content
                    )
                }

            } catch (e: Exception) {
                _commentsState.value = originalComments
                _toastMessage.emit("Thao tác không thích bình luận thất bại: ${e.message}")
            }
        }
    }

    fun likeObservation(observationId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _observationState.value ?: return@launch

            val hasLiked = userId in current.likeUserIds
            val hasDisliked = userId in current.dislikeUserIds

            val newLikes = if (hasLiked) current.likeUserIds - userId else current.likeUserIds + userId
            val newDislikes = current.dislikeUserIds - userId

            // Tính điểm mới
            val pointChange = when {
                hasLiked -> -1
                hasDisliked -> 2 // Bỏ dislike + thêm like
                else -> 1
            }

            _observationState.value = current.copy(
                likeUserIds = newLikes,
                dislikeUserIds = newDislikes,
                point = current.point + pointChange
            )

            try {
                observationRepository.toggleLikeObservation(observationId, userId)
                if (!hasLiked) {
                    sendActionNotification(
                        actorUserId = userId,
                        actorUsername = "",
                        targetUserId = current.uid, // Người nhận là chủ observation
                        postId = observationId,
                        actionType = "like_observation",
                        content = current.content
                    )
                }

            } catch (e: Exception) {
                _observationState.value = current // revert
                _toastMessage.emit("Thao tác thích thất bại: ${e.message}")
            }
        }
    }

    fun dislikeObservation(observationId: String, userId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val current = _observationState.value ?: return@launch

            val hasDisliked = userId in current.dislikeUserIds
            val hasLiked = userId in current.likeUserIds

            val newDislikes = if (hasDisliked) current.dislikeUserIds - userId else current.dislikeUserIds + userId
            val newLikes = current.likeUserIds - userId

            // Tính điểm mới
            val pointChange = when {
                hasDisliked -> +1
                hasLiked -> -2 // Bỏ like + thêm dislike
                else -> -1
            }

            _observationState.value = current.copy(
                dislikeUserIds = newDislikes,
                likeUserIds = newLikes,
                point = current.point + pointChange
            )

            try {
                observationRepository.toggleDislikeObservation(observationId, userId)
                if (!hasDisliked) {
                    sendActionNotification(
                        actorUserId = userId,
                        actorUsername = "",
                        targetUserId = current.uid,
                        postId = observationId,
                        actionType = "dislike_observation",
                        content = current.content
                    )
                }

            } catch (e: Exception) {
                _observationState.value = current // revert
                _toastMessage.emit("Thao tác không thích thất bại: ${e.message}")
            }
        }
    }
    fun saveObservation(observationId: String, userId: String) {
        viewModelScope.launch {
            val current = _observationState.value ?: return@launch

            // Giả lập thay đổi trên UI ngay lập tức
            val isSaved = current.saveUserIds.containsKey(userId)
            val updatedMap = if (isSaved) {
                current.saveUserIds - userId
            } else {
                current.saveUserIds + (userId to System.currentTimeMillis().toString()) // hoặc định dạng ISO nếu bạn muốn
            }

            _observationState.value = current.copy(saveUserIds = updatedMap)

            try {
                observationRepository.toggleSaveObservation(observationId, userId)
            } catch (e: Exception) {
                _observationState.value = current // revert nếu lỗi
                _toastMessage.emit("Lưu thất bại: ${e.message}")
            }
        }
    }

    fun deleteObservation(observationId: String) {
        viewModelScope.launch {
            try {
                observationRepository.deleteObservation(observationId)
                _eventFlow.emit(UiEvent.DeleteSuccess)
                // Optional: Reset UI state nếu cần
                _observationState.value = null
                _commentsState.value = emptyList()
                _uiState.value = UiState.Success
                messageApiService.updateNotificationState(postId = observationId, state = "hide")
            } catch (e: Exception) {
                _toastMessage.emit("Lỗi khi xoá: ${e.message}")
            }
        }
    }

    /*fun sortCommentsByTimestamp() {
        viewModelScope.launch(Dispatchers.Default) {
        _commentsState.value = _commentsState.value.sortedBy {
            it.timestamp
        }.let {
            if (_commentSortState.value) it.reversed() else it
        }}
    }*/


    suspend fun getLocationName(lat: Double, lon: Double): String {
        // Bọc toàn bộ hàm trong withContext
        return withContext(Dispatchers.IO) {
            // Có thể thêm try-catch ở đây để xử lý lỗi mạng một cách mượt mà
            try {
                geocodingService.reverseSearch(lat, lon)?.displayName ?: ""
            } catch (e: Exception) {
                Log.e("GeocodingError", "Failed to get location name", e)
                "" // Trả về chuỗi rỗng nếu có lỗi
            }
        }
    }

    override fun onCleared() {
        Log.i("a", "im being cleared")
        observationListenerJob?.cancel()
        commentListenerJob?.cancel()
        super.onCleared()
    }

}