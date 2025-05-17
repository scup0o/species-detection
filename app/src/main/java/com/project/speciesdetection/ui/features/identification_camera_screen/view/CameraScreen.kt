package com.project.speciesdetection.ui.features.identification_camera_screen.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraNavigationEffect
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraState
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraViewModel
import kotlinx.coroutines.flow.collectLatest

private const val TAG_SCREEN = "CameraScreen"

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToEditScreen: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val aspectRatio = if (isPortrait) 3f / 4f else 4f / 3f

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
            if (!granted) {
                Log.e(TAG_SCREEN, "Camera permission denied by user.")
                // Show rationale or navigate away
            }
            // Nếu quyền được cấp, TextureView listener sẽ tự động gọi onSurfaceTextureAvailable
            // và kích hoạt camera nếu view đã sẵn sàng.
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onGalleryImageSelected(it) }
    }

    // Lắng nghe navigation effects từ ViewModel
    LaunchedEffect(viewModel.navigationEffect) {
        viewModel.navigationEffect.collectLatest { effect ->
            when (effect) {
                is CameraNavigationEffect.NavigateToEditScreen -> {
                    onNavigateToEditScreen(effect.imageUri)
                }
            }
        }
    }

    // Lấy screen rotation hiện tại
    val currentScreenRotation = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
        }
    }

    val activity = LocalActivity.current

    DisposableEffect(Unit) {
        if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }


    // TextureView ref để lấy surface
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }
    // Biến state để trigger việc mở camera khi surface và permission sẵn sàng
    var surfaceDetailsForCamera by remember { mutableStateOf<SurfaceDetails?>(null) }


    // Quản lý vòng đời của Composable để yêu cầu quyền và đóng camera
    DisposableEffect(lifecycleOwner, hasCamPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> { // Hoặc ON_START tùy logic bạn muốn
                    if (!hasCamPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    // Không nên đóng camera ở đây vì TextureView có thể bị destroy và recreate
                    // khi app chỉ vào background rồi quay lại (mà không phải ON_DESTROY)
                    // ViewModel.onCleared() sẽ handle việc đóng camera khi ViewModel bị hủy.
                    // Hoặc nếu bạn muốn đóng camera khi màn hình không còn visible:
                    // if (viewModel.uiState.value.cameraState !is CameraState.Idle && viewModel.uiState.value.cameraState !is CameraState.Closed) {
                    //     Log.d(TAG_SCREEN, "ON_STOP: Releasing camera from Composable")
                    //     viewModel.releaseCameraOnStop() // Cần thêm hàm này vào ViewModel để gọi helper.releaseCamera()
                    // }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            Log.d(TAG_SCREEN, "CameraScreen Composable disposed.")
            // ViewModel.onCleared sẽ giải phóng camera
        }
    }

    // Effect để mở camera khi có đủ điều kiện (permission và surface)
    LaunchedEffect(hasCamPermission, surfaceDetailsForCamera) {
        if (hasCamPermission && surfaceDetailsForCamera != null) {
            Log.d(TAG_SCREEN, "Permission granted and surface available. Requesting ViewModel to open camera.")
            viewModel.onSurfaceReady(
                surfaceDetailsForCamera!!.surface,
                currentScreenRotation,
                surfaceDetailsForCamera!!.width,
                surfaceDetailsForCamera!!.height
            )
        }
    }


    LaunchedEffect(currentScreenRotation) {
        viewModel.onScreenRotationChanged(currentScreenRotation)

        // Reset lại camera preview khi orientation đổi
        textureViewRef.value?.surfaceTexture?.let { st ->
            val surface = Surface(st)
            val width = textureViewRef.value?.width ?: 0
            val height = textureViewRef.value?.height ?: 0
            if (width > 0 && height > 0) {
                viewModel.onSurfaceReady(surface, currentScreenRotation, width, height)
            }
        }
    }


    if (hasCamPermission) {
        Box(modifier = Modifier.fillMaxSize()) {


            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        textureViewRef.value = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                Log.d(TAG_SCREEN, "SurfaceTexture available: $w x $h")
                                val surface = Surface(st)
                                uiState.optimalPreviewSize?.let { optimalSize ->
                                    st.setDefaultBufferSize(optimalSize.width, optimalSize.height)
                                }
                                surfaceDetailsForCamera = SurfaceDetails(surface, w, h)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                Log.d(TAG_SCREEN, "SurfaceTexture size changed: $w x $h")
                                val currentDetails = surfaceDetailsForCamera
                                if (currentDetails == null || currentDetails.width != w || currentDetails.height != h) {
                                    val surface = Surface(st)
                                    surfaceDetailsForCamera = SurfaceDetails(surface, w, h)
                                }
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                surfaceDetailsForCamera = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(aspectRatio)
                    .align(Alignment.Center),
                update = { view ->
                    uiState.optimalPreviewSize?.let { optimalSize ->
                        if (view.surfaceTexture?.isReleased == false &&
                            view.width > 0 && view.height > 0
                        ) {
                            view.surfaceTexture?.setDefaultBufferSize(optimalSize.width, optimalSize.height)
                        }
                    }
                }
            )

            // Top controls: Flash
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::toggleFlash, enabled = uiState.isCameraReady) {
                    Icon(
                        imageVector = if (uiState.isFlashOn) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom controls: Gallery, Capture, Flip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Filled.AccountBox,
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Button(
                    onClick = viewModel::takePicture,
                    enabled = uiState.isCameraReady && uiState.cameraState is CameraState.Previewing,
                    modifier = Modifier
                        .size(70.dp)
                        .border(2.dp, Color.White, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) { /* Icon chụp ảnh (nếu có) */ }

                IconButton(
                    onClick = {
                        textureViewRef.value?.surfaceTexture?.let { st ->
                            if (!st.isReleased) {
                                val surface = Surface(st)
                                viewModel.flipCamera(
                                    surface = surface,
                                    screenRotation = currentScreenRotation,
                                    viewWidth = textureViewRef.value?.width ?: 0,
                                    viewHeight = textureViewRef.value?.height ?: 0
                                )
                            } else {
                                Log.w(TAG_SCREEN, "Cannot flip camera, surface texture is released.")
                            }
                        } ?: Log.w(TAG_SCREEN, "Cannot flip camera, TextureView or SurfaceTexture is null.")
                    },
                    enabled = uiState.isCameraReady // Hoặc kiểm tra cụ thể hơn nếu cần
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Hiển thị lỗi nếu có
            uiState.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                    action = {
                        Button(onClick = { /* viewModel.clearError() */ }) { // Cần thêm hàm clearError
                            Text("Dismiss")
                        }
                    }
                ) { Text(message) }
            }

            // Loading indicator (ví dụ)
            if (uiState.cameraState == CameraState.Opening || uiState.cameraState == CameraState.Capturing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

        }
    } else {
        // UI yêu cầu quyền
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

data class SurfaceDetails(val surface: Surface, val width: Int, val height: Int)