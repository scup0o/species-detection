package com.project.speciesdetection.ui.features.identification_camera_screen.view // Hoặc package của bạn

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.view.PreviewView // Thêm import này
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraViewModel
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.FlashModeState
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    navigateToEditScreen: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
    val galleryImageUri by viewModel.galleryImageUri.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Log.e("CameraScreen", "Camera permission denied")
            }
        }
    )

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.onGalleryImageSelected(it)
            }
        }
    )

    // Khởi tạo PreviewView ở đây
    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FIT_CENTER // QUAN TRỌNG
        }
    }

    // ImageCapture được tạo lại khi flashMode thay đổi
    val imageCapture = remember(uiState.flashMode) {
        ImageCapture.Builder()
            .setFlashMode(
                when (uiState.flashMode) {
                    FlashModeState.ON -> ImageCapture.FLASH_MODE_ON
                    FlashModeState.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashModeState.AUTO -> ImageCapture.FLASH_MODE_AUTO // Nếu bạn hỗ trợ
                }
            )
            // KHÔNG setTargetAspectRatio hoặc setTargetResolution ở đây
            // để ViewPort và UseCaseGroup xử lý
            .build()
    }
    var camera: Camera? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(capturedImageUri) {
        capturedImageUri?.let { uri ->
            Log.d("CameraScreen", "Photo captured: $uri")
            navigateToEditScreen(uri)
            viewModel.clearProcessedImageUris()
        }
    }

    LaunchedEffect(galleryImageUri) {
        galleryImageUri?.let { uri ->
            Log.d("CameraScreen", "Gallery image selected: $uri")
            navigateToEditScreen(uri)
            viewModel.clearProcessedImageUris()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Rebind camera khi lensFacing, cameraProvider, quyền, flashMode, hoặc display của previewView thay đổi
    LaunchedEffect(
        uiState.lensFacing,
        uiState.cameraProvider,
        hasCameraPermission,
        uiState.flashMode,
        previewView.display // Key quan trọng để re-bind khi viewport có thể đã sẵn sàng
    ) {
        val cameraProvider = uiState.cameraProvider ?: return@LaunchedEffect
        if (!hasCameraPermission || !uiState.isCameraReady) return@LaunchedEffect

        try {
            cameraProvider.unbindAll() // Unbind tất cả use cases trước khi bind lại

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(uiState.lensFacing)
                .build()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Tạo UseCaseGroup
            val useCaseGroupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)

            // Lấy ViewPort từ PreviewView và áp dụng cho UseCaseGroup
            // previewView.viewPort chỉ non-null sau khi PreviewView được layout
            previewView.viewPort?.let { viewPort ->
                Log.d("CameraScreen", "Applying ViewPort: AspectRatio=${viewPort.aspectRatio}, Rotation=${viewPort.rotation}")
                useCaseGroupBuilder.setViewPort(viewPort)
            } ?: run {
                Log.w("CameraScreen", "ViewPort is null. CameraX will attempt to match aspect ratios.")
                // Nếu ViewPort null, CameraX sẽ cố gắng khớp, nhưng có thể không hoàn hảo.
                // Bạn có thể thử đặt targetAspectRatio cho Preview và ImageCapture theo cách thủ công
                // dựa trên kích thước của previewView, nhưng ViewPort là cách tốt hơn.
            }

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                useCaseGroupBuilder.build() // Bind UseCaseGroup
            )

            // Thiết lập torch (flash cho preview) sau khi bind
            if (uiState.lensFacing == CameraSelector.LENS_FACING_BACK && camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(uiState.flashMode == FlashModeState.ON)
            }

        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
            // Cân nhắc hiển thị thông báo lỗi cho người dùng
        }
    }


    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to use this feature.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    } else if (!uiState.isCameraReady && hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Initializing camera...", modifier = Modifier.padding(top = 60.dp))
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Sử dụng PreviewView đã khởi tạo
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            // Top controls (Flash)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (camera?.cameraInfo?.hasFlashUnit() == true && uiState.lensFacing == CameraSelector.LENS_FACING_BACK) {
                    IconButton(onClick = { viewModel.toggleFlash(camera) }) {
                        Icon(
                            imageVector = if (uiState.flashMode == FlashModeState.ON) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Flash",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f)) // Tăng alpha để rõ hơn
                    .padding(vertical = 24.dp, horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.takePhoto(
                                imageCapture = imageCapture,
                                onImageCaptured = { uri -> viewModel.onPhotoCaptured(uri) },
                                onError = { exception ->
                                    Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                                    // Hiển thị Toast hoặc thông báo lỗi cho người dùng
                                    Toast.makeText(context, "Failed to capture photo: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .size(72.dp) // Kích thước nút chụp
                        .background(Color.White, CircleShape)
                        .border(2.dp, Color.LightGray, CircleShape) // Viền cho nút chụp
                ) {
                    // Có thể thêm Icon chụp ảnh ở đây nếu muốn, nhưng thường để trống
                }

                IconButton(onClick = { viewModel.flipCamera() }) {
                    Icon(
                        imageVector = Icons.Default.Call, // Hoặc Icons.Filled.Cameraswitch
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}