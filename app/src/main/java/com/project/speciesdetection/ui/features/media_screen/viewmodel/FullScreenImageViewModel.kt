package com.project.speciesdetection.ui.features.media_screen.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.speciesdetection.domain.usecase.common.MediaFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FullScreenImageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaFileUseCase: MediaFileUseCase
) : ViewModel(){

    data class UiState(
        val isDownloading : Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun downloadImage(image : Uri){
        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            try{
                mediaFileUseCase.saveMediaToGallery(image)
                Toast.makeText(context, "Lưu thành công", Toast.LENGTH_SHORT).show()
            }
            catch (e: Exception) {
                Toast.makeText(context, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show()

            }
            _uiState.value = _uiState.value.copy(isDownloading = false)

        }
    }
}