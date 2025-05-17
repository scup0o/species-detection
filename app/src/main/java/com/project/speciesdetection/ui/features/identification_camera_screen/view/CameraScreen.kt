package com.project.speciesdetection.ui.features.identification_camera_screen.view

import android.Manifest
import android.annotation.SuppressLint
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
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.core.theme.strokes
import com.project.speciesdetection.domain.model.camera.CameraState
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraNavigationEffect
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraViewModel
import kotlinx.coroutines.flow.collectLatest

private const val TAG_SCREEN = "CameraScreen"

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onNavigateToEditScreen: (Uri) -> Unit,
    onNavigateBack: () -> Unit,
) {

    //string
    val camera_permision_denied = stringResource(R.string.camera_permission_denied)

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    // Lấy screen rotation hiện tại
    val currentScreenRotation = remember(configuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
        }
    }
    // TextureView ref để lấy surface
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }


    //ui state management
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Biến state để trigger việc mở camera khi surface và permission sẵn sàng
    var surfaceDetailsForCamera by remember { mutableStateOf<SurfaceDetails?>(null) }
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    // Zoom state
    var currentZoomRatio by remember { mutableStateOf(1.0f) }

    val scaleGestureDetector = remember(context, viewModel) {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                currentZoomRatio *= detector.scaleFactor
                currentZoomRatio = currentZoomRatio.coerceIn(1.0f, 8.0f) // Clamp locally first, Camera2Source will clamp with actual maxZoom
                viewModel.setZoomRatio(currentZoomRatio)
                return true
            }
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
            if (!granted) {
                Toast.makeText(context, camera_permision_denied, Toast.LENGTH_LONG).show()
                //Log.e(TAG_SCREEN, "Camera permission denied by user.")
                // Show rationale or navigate away
            }
        }
    )
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onGalleryImageSelected(it) }
    }

    // Quản lý vòng đời của Composable để yêu cầu quyền và đóng camera
    DisposableEffect(lifecycleOwner, hasCamPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    if (!hasCamPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                /*
                Lifecycle.Event.ON_RESUME -> {
                    // When resuming, if camera was closed (e.g. onStop), and we have surface + permission, reopen
                    if (hasCamPermission && surfaceDetailsForCamera != null &&
                        (uiState.cameraState is CameraState.Closed || uiState.cameraState is CameraState.Idle)) {
                        Log.d(TAG_SCREEN, "ON_RESUME: Reopening camera.")
                        surfaceDetailsForCamera?.let { details ->
                            viewModel.onSurfaceReady(
                                details.surface,
                                currentScreenRotation,
                                details.width,
                                details.height
                            )
                        }
                    }
                }*/
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

    LaunchedEffect(hasCamPermission, surfaceDetailsForCamera) {
        if (hasCamPermission && surfaceDetailsForCamera != null) {
            Log.d(TAG_SCREEN, "Permission granted and surface available. Requesting ViewModel to open camera.")
            currentZoomRatio = 1.0f
            viewModel.onSurfaceReady(
                surfaceDetailsForCamera!!.surface,
                currentScreenRotation,
                surfaceDetailsForCamera!!.width,
                surfaceDetailsForCamera!!.height
            )
        }
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

    LaunchedEffect(currentScreenRotation) {
        viewModel.onScreenRotationChanged(currentScreenRotation)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)) {
        if (hasCamPermission) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        textureViewRef.value = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                st: SurfaceTexture,
                                w: Int,
                                h: Int
                            ) {
                                Log.d(TAG_SCREEN, "SurfaceTexture available: $w x $h")
                                val surface = Surface(st)
                                uiState.optimalPreviewSize?.let { optimalSize ->
                                    st.setDefaultBufferSize(optimalSize.width, optimalSize.height)
                                }
                                surfaceDetailsForCamera = SurfaceDetails(surface, w, h)

                                applyTransform(this@apply, currentScreenRotation)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                st: SurfaceTexture,
                                w: Int,
                                h: Int
                            ) {
                                Log.d(TAG_SCREEN, "SurfaceTexture size changed: $w x $h")
                                val currentDetails = surfaceDetailsForCamera
                                if (currentDetails == null || currentDetails.width != w || currentDetails.height != h) {
                                    val surface = Surface(st)
                                    surfaceDetailsForCamera = SurfaceDetails(surface, w, h)
                                }

                                applyTransform(this@apply, currentScreenRotation)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                surfaceDetailsForCamera = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                        setOnTouchListener { _, event ->
                            if (uiState.isCameraReady) { // Chỉ xử lý khi camera sẵn sàng
                                scaleGestureDetector.onTouchEvent(event)
                            }
                            true // Đã xử lý sự kiện
                        }
                    }
                },
                modifier = with(LocalConfiguration.current) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f) // Portrait aspect ratio
                            .align(Alignment.Center)
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(4f / 3f) // Landscape aspect ratio — hoán đổi lại
                            .align(Alignment.Center)
                    }
                },
                update = {
                    /*view ->
                    uiState.optimalPreviewSize?.let { optimalSize ->
                        if (view.surfaceTexture?.isReleased == false &&
                            view.width > 0 && view.height > 0
                        ) {
                            view.surfaceTexture?.setDefaultBufferSize(optimalSize.width, optimalSize.height)
                        }
                    }*/
                }
            )
        }
        else {
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

        if (!isPortrait) {
            // Top controls: Flash
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::toggleFlash, enabled = uiState.isCameraReady) {
                    Icon(
                        if (uiState.isFlashOn) painterResource(R.drawable.flash_on)
                        else painterResource(R.drawable.flash_off),
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier
                    )
                }
                IconButton(
                    onClick = onNavigateBack,
                    enabled = uiState.isCameraReady
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            //Bottom Control
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(
                        horizontal = MaterialTheme.spacing.xl,
                        vertical = MaterialTheme.spacing.m
                    ),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

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
                                currentZoomRatio = 1.0f
                            } else {
                                Log.w(
                                    TAG_SCREEN,
                                    "Cannot flip camera, surface texture is released."
                                )
                            }
                        } ?: Log.w(
                            TAG_SCREEN,
                            "Cannot flip camera, TextureView or SurfaceTexture is null."
                        )
                    },
                    enabled = uiState.isCameraReady
                ) {
                    Icon(
                        painterResource(R.drawable.camera_flip),
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Button(
                    onClick = viewModel::takePicture,
                    enabled = uiState.isCameraReady && uiState.cameraState is CameraState.Previewing,
                    modifier = Modifier
                        .size(60.dp)
                        .border(MaterialTheme.strokes.xl, Color.White, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f)
                    )
                ) {}

                IconButton(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Icon(
                        painterResource(R.drawable.image),
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier
                    )
                }
            }
        } else {
            // Top controls: Flash
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = uiState.isCameraReady
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = viewModel::toggleFlash, enabled = uiState.isCameraReady) {
                    Icon(
                        if (uiState.isFlashOn) painterResource(R.drawable.flash_on)
                        else painterResource(R.drawable.flash_off),
                        contentDescription = "Toggle Flash",
                        tint = Color.White,
                        modifier = Modifier
                    )
                }
            }

            // Bottom Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = MaterialTheme.spacing.m,
                        vertical = MaterialTheme.spacing.xl
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Icon(
                        painterResource(R.drawable.image),
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier
                    )
                }

                Button(
                    onClick = viewModel::takePicture,
                    enabled = uiState.isCameraReady && uiState.cameraState is CameraState.Previewing,
                    modifier = Modifier
                        .size(60.dp)
                        .border(MaterialTheme.strokes.xl, Color.White, CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f)
                    )
                ) {}

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
                                currentZoomRatio = 1.0f
                            } else {
                                Log.w(
                                    TAG_SCREEN,
                                    "Cannot flip camera, surface texture is released."
                                )
                            }
                        } ?: Log.w(
                            TAG_SCREEN,
                            "Cannot flip camera, TextureView or SurfaceTexture is null."
                        )
                    },
                    enabled = uiState.isCameraReady
                ) {
                    Icon(
                        painterResource(R.drawable.camera_flip),
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Hiển thị lỗi
        uiState.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                /*action = {
                    Button(onClick = { /* viewModel.clearError() */ }) { // Cần thêm hàm clearError
                        Text("Dismiss")
                    }
                }*/
            ) { Text(message) }
        }

        // Loading indicator
        if (uiState.cameraState == CameraState.Opening || uiState.cameraState == CameraState.Capturing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }


}

fun applyTransform(textureView: TextureView, rotation: Int) {
    val matrix = Matrix()


    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    val centerX = viewWidth / 2
    val centerY = viewHeight / 2

    if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
        // Swap width/height for scaling
        val scale = viewHeight / viewWidth
        matrix.setScale(scale, 1 / scale, centerX, centerY)
        val rotationDegrees = if (rotation == Surface.ROTATION_90) -90f else 90f
        matrix.postRotate(rotationDegrees, centerX, centerY)
    } else if (rotation == Surface.ROTATION_180) {
        matrix.postRotate(180f, centerX, centerY)
    } else {
        matrix.reset()
    }

    textureView.setTransform(matrix)
}


data class SurfaceDetails(val surface: Surface, val width: Int, val height: Int)
