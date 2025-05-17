package com.project.speciesdetection.ui.features.identification_edit_image_screen.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel.EditImageForIdentificationViewModel

// ---- TỶ LỆ KHUNG HÌNH CỐ ĐỊNH CHO HIỂN THỊ (PHẢI KHỚP VỚI CAMERA) ----
// Nếu FIXED_CAMERA_ASPECT_RATIO_INT trong CameraScreen là AspectRatio.RATIO_4_3
private const val FIXED_DISPLAY_ASPECT_RATIO_FLOAT: Float = 4f / 3f
// Nếu FIXED_CAMERA_ASPECT_RATIO_INT trong CameraScreen là AspectRatio.RATIO_16_9
// private const val FIXED_DISPLAY_ASPECT_RATIO_FLOAT: Float = 16f / 9f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditImageForIdentificationScreen(
    viewModel: EditImageForIdentificationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Quyền đã được cấp, tiến hành lưu ảnh
                viewModel.saveCurrentImageToGallery(context)
            } else {
                // Quyền bị từ chối
                Toast.makeText(context, "Storage permission denied. Cannot save image.", Toast.LENGTH_LONG).show()
            }
        }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            viewModel.onImageCropped(result.uriContent)
        } else {
            Log.e("EditImageScreen", "Cropping failed or cancelled", result.error)
            viewModel.onImageCropped(null)
            Toast.makeText(context, "Crop failed: ${result.error?.localizedMessage ?: "Cancelled"}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        when (uiState.saveSuccess) {
            true -> {
                Toast.makeText(context, "Image saved successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveStatus()
            }
            false -> {
                Toast.makeText(context, uiState.error ?: "Failed to save image.", Toast.LENGTH_LONG).show()
                viewModel.resetSaveStatus()
            }
            null -> {}
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            if (uiState.saveSuccess == null) { // Chỉ hiện nếu không phải lỗi save
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                // viewModel.resetSaveStatus() // hoặc viewModel.clearError() nếu có
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(
                        onClick = {
                            uiState.currentImageUri?.let { uri ->
                                val cropOptions = CropImageContractOptions(
                                    uri = uri,
                                    cropImageOptions = CropImageOptions(
                                        guidelines = CropImageView.Guidelines.ON_TOUCH,
                                        fixAspectRatio = false, // Cho phép crop tự do
                                        // Nếu muốn crop cũng theo tỷ lệ cố định:
                                        // fixAspectRatio = true,
                                        // aspectRatioX = 4, // Thay đổi nếu tỷ lệ khác
                                        // aspectRatioY = 3, // Thay đổi nếu tỷ lệ khác
                                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                                        outputCompressQuality = 90
                                    )
                                )
                                cropImageLauncher.launch(cropOptions)
                            } ?: Toast.makeText(context, "No image to crop", Toast.LENGTH_SHORT).show()
                        },
                        enabled = uiState.currentImageUri != null && !uiState.isLoading
                    ) { Icon(Icons.Default.Edit, "Crop Image") }

                    IconButton(
                        onClick = {
                            // Kiểm tra quyền trước khi gọi viewModel.saveCurrentImageToGallery
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Chỉ cần kiểm tra cho Android 9 (API 28) trở xuống
                                when (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )) {
                                    PackageManager.PERMISSION_GRANTED -> {
                                        // Quyền đã được cấp
                                        viewModel.saveCurrentImageToGallery(context)
                                    }
                                    else -> {
                                        // Yêu cầu quyền
                                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            } else {
                                // Android 10 (Q) trở lên, MediaStore không cần quyền này để ghi vào thư mục public
                                // (như Pictures/YourAppName mà ViewModel đang dùng)
                                viewModel.saveCurrentImageToGallery(context)
                            }
                        },
                        enabled = uiState.currentImageUri != null && !uiState.isLoading
                    ){ Icon(Icons.Default.Add, "Save Image") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    //.aspectRatio(FIXED_DISPLAY_ASPECT_RATIO_FLOAT)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading && uiState.saveSuccess == null) { CircularProgressIndicator(color = Color.White) }
                else if (uiState.currentImageUri != null) {
                    GlideImage(
                        model = uiState.currentImageUri,
                        contentDescription = "Image to edit",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (uiState.error != null && uiState.currentImageUri == null) {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                } else {
                    Text("Loading image...", modifier = Modifier.padding(16.dp), color = Color.White)
                }
            }


            Button(
                onClick = { viewModel.showImageInPopup() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp).height(50.dp),
                enabled = uiState.currentImageUri != null && !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Analyze", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.showAnalysisPopup && uiState.imageForPopup != null) {
            AnalysisResultPopup(
                imageUri = uiState.imageForPopup!!,
                onDismissRequest = { viewModel.dismissAnalysisPopup() }
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AnalysisResultPopup(imageUri: Uri, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)) {
        Card(modifier = Modifier.fillMaxWidth(0.85f).wrapContentHeight().clip(RoundedCornerShape(16.dp))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Preview Result", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        //.aspectRatio(FIXED_DISPLAY_ASPECT_RATIO_FLOAT) // Ép tỷ lệ cho popup
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp)) // Hồng rất nhạt
                        .border(2.dp, Color(0xFFF06292), RoundedCornerShape(12.dp)) // Hồng nhạt
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(model = imageUri, contentDescription = "Analyzed image preview", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismissRequest, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(8.dp)) { Text("Close") }
            }
        }
    }
}