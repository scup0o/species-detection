package com.project.speciesdetection.ui.features.identification_camera_screen.view // Hoặc package của bạn

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraViewModel
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.FlashModeState
import kotlinx.coroutines.launch

private const val PREFERRED_SENSOR_ASPECT_RATIO_INT = AspectRatio.RATIO_16_9

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "RestrictedApi") // RestrictedApi cho ViewPort.Builder
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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) { Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show() }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onGalleryImageSelected(it) }
    }

    var previewViewAspectRatio by remember { mutableStateOf<Rational?>(null) }
    var previewViewRotation by remember { mutableStateOf(Surface.ROTATION_0) }

    // State để giữ ImageCapture instance cho nút chụp
    var imageCaptureForButton by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FILL_CENTER
            this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var camera: Camera? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    // LaunchedEffects cho error, Uris, permission
    LaunchedEffect(uiState.error) { uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearError() } }
    LaunchedEffect(capturedImageUri) { capturedImageUri?.let { navigateToEditScreen(it); viewModel.clearProcessedImageUris() } }
    LaunchedEffect(galleryImageUri) { galleryImageUri?.let { navigateToEditScreen(it); viewModel.clearProcessedImageUris() } }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    // LaunchedEffect để bind camera, sẽ tạo ImageCapture bên trong
    LaunchedEffect(
        uiState.lensFacing,
        uiState.cameraProvider,
        hasCameraPermission,
        uiState.flashMode, // Flash mode thay đổi cần tạo lại ImageCapture
        previewViewAspectRatio, // Tỷ lệ ViewPort thay đổi cần rebind
        previewViewRotation   // Hướng màn hình thay đổi cần rebind
    ) {
        val camProvider = uiState.cameraProvider ?: return@LaunchedEffect

        if (!hasCameraPermission || !uiState.isCameraReady ) {
            Log.d("CameraScreen", "Binding prerequisites (1) not met: Perm=$hasCameraPermission, Ready=${uiState.isCameraReady}")
            return@LaunchedEffect
        }

        if (previewViewAspectRatio == null || previewViewAspectRatio!!.numerator == 0 || previewViewAspectRatio!!.denominator == 0) {
            Log.d("CameraScreen", "Waiting for valid previewViewAspectRatio before binding. Current: $previewViewAspectRatio")
            return@LaunchedEffect
        }

        Log.i("CameraScreen", "Attempting to bind camera. Target ViewPort AR: $previewViewAspectRatio, Rotation: $previewViewRotation, Flash: ${uiState.flashMode}")

        try {
            camProvider.unbindAll() // Unbind trước khi bind lại
            val cameraSelector = CameraSelector.Builder().requireLensFacing(uiState.lensFacing).build()

            val preview = Preview.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy(PREFERRED_SENSOR_ASPECT_RATIO_INT, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()
                )
                .setTargetRotation(previewViewRotation)
                .build().also { it.surfaceProvider = previewView.surfaceProvider }

            val newImageCapture = ImageCapture.Builder()
                .setFlashMode(
                    when (uiState.flashMode) {
                        FlashModeState.ON -> ImageCapture.FLASH_MODE_ON
                        FlashModeState.OFF -> ImageCapture.FLASH_MODE_OFF
                        FlashModeState.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    }
                )
                .setResolutionSelector( // Selector để lấy nguồn ảnh tốt
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy(PREFERRED_SENSOR_ASPECT_RATIO_INT, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()
                )
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(previewViewRotation)
                // KHÔNG dùng .setCropAspectRatio() nữa
                .build()
            imageCaptureForButton = newImageCapture // Cập nhật cho nút bấm

            val useCaseGroupBuilder = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(newImageCapture)

            // Tạo và áp dụng ViewPort cho UseCaseGroup
            // Rational(width, height)
            val targetViewPort = ViewPort.Builder(previewViewAspectRatio!!, previewViewRotation).build()
            Log.d("CameraScreen", "Setting ViewPort for UseCaseGroup: AR=${targetViewPort.aspectRatio.toFloat()}, Rotation=${targetViewPort.rotation}")
            useCaseGroupBuilder.setViewPort(targetViewPort)

            camera = camProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroupBuilder.build())
            Log.i("CameraScreen", "Camera bound successfully with ViewPort.")

            if (uiState.lensFacing == CameraSelector.LENS_FACING_BACK && camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(uiState.flashMode == FlashModeState.ON)
            }
        } catch (e: Exception) {
            Log.e("CameraScreen", "Binding failed: ${e.localizedMessage}", e)
            viewModel.setCameraError("Binding failed: ${e.javaClass.simpleName}")
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Camera permission required."); Button(onClick = {permissionLauncher.launch(Manifest.permission.CAMERA)}){ Text("Grant")}}}
    } else if (!uiState.isCameraReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(); Text("Initializing Camera...", Modifier.padding(top = 70.dp))}
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // Bạn có thể để trống nếu không muốn tiêu đề: Text("")
                        // Hoặc thêm tiêu đề nếu muốn:
                        // Text("Camera", color = Color.White)
                    },
                    // navigationIcon = { /* Nếu bạn có icon điều hướng bên trái */ },
                    actions = {
                        // Đặt IconButton của bạn vào đây
                        if (camera?.cameraInfo?.hasFlashUnit() == true && uiState.lensFacing == CameraSelector.LENS_FACING_BACK) {
                            IconButton(onClick = { viewModel.toggleFlash(camera) }) {
                                Icon(
                                    imageVector = if (uiState.flashMode == FlashModeState.ON) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Toggle Flash",
                                    tint = Color.White, // Icon màu trắng
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        // Thêm các IconButton khác cho actions nếu có
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black, // MÀU NỀN ĐEN CHO TOPAPPBAR
                        titleContentColor = Color.White, // Màu chữ tiêu đề (nếu có)
                        navigationIconContentColor = Color.White, // Màu icon điều hướng (nếu có)
                        actionIconContentColor = Color.White // Màu mặc định cho các action icons
                    )
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black).padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Filled.AccountBox, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = {
                            imageCaptureForButton?.let { ic ->
                                Log.d("CameraScreen", "Capture button pressed. ImageCapture should be cropped by UseCaseGroup's ViewPort.")
                                coroutineScope.launch {
                                    viewModel.takePhoto(ic, viewModel::onPhotoCaptured) { ex ->
                                        Log.e("CameraScreen", "Photo capture failed: ${ex.message}", ex)
                                        Toast.makeText(context, "Capture failed: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } ?: Toast.makeText(context, "Camera (ImageCapture) not ready.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(68.dp).background(Color.White, CircleShape).border(3.dp, Color.DarkGray, CircleShape)
                    ) {}
                    IconButton(onClick = { viewModel.flipCamera() }) {
                        Icon(Icons.Filled.Build, "Flip Camera", tint = Color.White, modifier = Modifier.size(32.dp)) // Ví dụ icon
                    }
                }
            }
        ) { paddingValues ->
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .onGloballyPositioned { coordinates ->
                        val widthPx = coordinates.size.width
                        val heightPx = coordinates.size.height

                        if (widthPx > 0 && heightPx > 0) {
                            val newRational = Rational(widthPx, heightPx)
                            val newRotation = previewView.display?.rotation ?: Surface.ROTATION_0

                            var changed = false
                            if (previewViewAspectRatio?.toFloat() != newRational.toFloat()) {
                                Log.i("CameraScreen", "PreviewView Rational AR Updated: ${newRational.numerator}:${newRational.denominator} (was $previewViewAspectRatio)")
                                previewViewAspectRatio = newRational
                                changed = true
                            }
                            if (previewViewRotation != newRotation) {
                                Log.i("CameraScreen", "PreviewView Rotation Updated: $newRotation (was $previewViewRotation)")
                                previewViewRotation = newRotation
                                changed = true
                            }
                            if(changed) {
                                Log.d("CameraScreen", "PreviewView layout changed, will trigger rebind if necessary.")
                            }
                        }
                    }
            )
        }
    }
}