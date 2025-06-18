package com.project.speciesdetection.ui.features.media_screen.view

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.helpers.MediaHelper
import com.project.speciesdetection.domain.usecase.common.MediaFileUseCase
import com.project.speciesdetection.ui.features.media_screen.viewmodel.FullScreenImageViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FullScreenImageViewer(
    image: Uri,
    onNavigateBack: () -> Unit,
    viewModel : FullScreenImageViewModel = hiltViewModel()
) {
    val imageModel = Uri.decode(CloudinaryImageURLHelper.restoreCloudinaryOriginalUrl(image.toString())).toUri()
    val context = LocalContext.current
    val mimeType = context.contentResolver.getType(imageModel) ?: ""
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                mimeType.startsWith("image/") || MediaHelper.isImageFile(imageModel.toString()) -> {
                    GlideImage(
                        model = imageModel,
                        contentDescription = "Full screen image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                mimeType.startsWith("video/") || MediaHelper.isVideoFile(imageModel.toString()) -> {
                    VideoPlayer(uri = imageModel)
                }
            }

            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            if (uiState.isDownloading){
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
            else{
                IconButton(
                    onClick = {
                        viewModel.downloadImage(imageModel)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }
            }

        }
    }
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(), // Cho video full screen như ảnh
        factory = {
            VideoView(context).apply {
                setVideoURI(uri)
                setMediaController(MediaController(context).apply {
                    setAnchorView(this@apply)
                })
                setOnPreparedListener {
                    //it.isLooping = true
                    start()
                }
            }
        }
    )
}