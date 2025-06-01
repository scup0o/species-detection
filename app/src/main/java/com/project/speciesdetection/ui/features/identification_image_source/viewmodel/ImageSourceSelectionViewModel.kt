package com.project.speciesdetection.ui.features.identification_image_source.viewmodel

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.project.speciesdetection.domain.usecase.common.MediaFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ImageSourceSelectionViewModel @Inject constructor(
    val createTemporaryImageFileUseCase: MediaFileUseCase
) : ViewModel(){
    suspend fun createTemporaryImageFile() : Pair<Uri, File>?{
        return createTemporaryImageFileUseCase.createImageFileAndUriInCache()
    }
}

