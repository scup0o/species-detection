package com.project.speciesdetection.ui.features.media_screen.view

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.helpers.MediaHelper
import com.project.speciesdetection.ui.features.media_screen.viewmodel.FullScreenImageViewModel
// --- 1. IMPORT CÁC THÀNH PHẦN TỪ THƯ VIỆN MỚI "TELEPHOTO" ---
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.zoomable

@OptIn(ExperimentalGlideComposeApi::class) // Vẫn cần thiết vì ta dùng Glide
@Composable
fun FullScreenImageViewer(
    image: Uri,
    onNavigateBack: () -> Unit,
    viewModel: FullScreenImageViewModel = hiltViewModel(),
    transform: Boolean = true,
) {
    val imageModel =
        if (transform) Uri.decode(CloudinaryImageURLHelper.restoreCloudinaryOriginalUrl(image.toString())).toUri()
    else image
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
                // --- 2. XỬ LÝ TRƯỜNG HỢP LÀ ẢNH VỚI TELEPHOTO ---
                mimeType.startsWith("image/") || MediaHelper.isImageFile(imageModel.toString()) -> {
                    // Telephoto yêu cầu một đối tượng state để quản lý việc zoom.
                    // Điều này giúp tách biệt state và UI, một thực hành tốt.
                    val zoomableState = rememberZoomableImageState()

                    // Sử dụng Composable chuyên dụng cho Glide từ thư viện Telephoto
                    ZoomableGlideImage(
                        model = if (transform) imageModel else imageModel.toString(),
                        contentDescription = "Full screen zoomable image",
                        modifier = Modifier.fillMaxSize(),
                        state = zoomableState // Truyền state vào
                    )
                }

                // --- 3. XỬ LÝ TRƯỜNG HỢP LÀ VIDEO (KHÔNG THAY ĐỔI) ---
                mimeType.startsWith("video/") || MediaHelper.isVideoFile(imageModel.toString()) -> {
                    VideoPlayer(uri = imageModel)
                }
            }

            // --- 4. CÁC NÚT ĐIỀU KHIỂN (KHÔNG THAY ĐỔI) ---
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




            if (transform){
                IconButton(
                    onClick = {
                        viewModel.downloadImage(imageModel)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    if (uiState.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .width(1.dp)
                        )
                    } else {
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
}

@Composable
fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            VideoView(context).apply {
                setVideoURI(uri)
                setMediaController(MediaController(context).apply {
                    setAnchorView(this@apply)
                })
                setOnPreparedListener {
                    start()
                }
            }
        }
    )
}