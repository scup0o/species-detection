package com.project.speciesdetection.ui.features.identification_edit_image_screen.view

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel.EditImageForIdentificationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditImageForIdentificationScreen(
    viewModel: EditImageForIdentificationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            viewModel.onImageCropped(result.uriContent)
        } else {
            val exception = result.error
            Log.e("EditImageScreen", "Cropping failed or cancelled", exception)
            viewModel.onImageCropped(null) // Báo cho ViewModel crop không thành công
            if (exception != null) {
                Toast.makeText(context, "Crop failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Crop cancelled.", Toast.LENGTH_SHORT).show()
            }
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
            null -> { /* Do nothing */ }
        }
    }

    // Hiển thị Toast cho các lỗi chung không liên quan đến save
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            // Chỉ hiển thị nếu không phải là lỗi liên quan đến save (đã được xử lý ở trên)
            if (uiState.saveSuccess == null) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                // viewModel.clearError() // Cần thêm hàm clearError trong ViewModel nếu muốn error chỉ hiện 1 lần
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            uiState.currentImageUri?.let { uriToCrop ->
                                val cropOptions = CropImageContractOptions(
                                    uri = uriToCrop,
                                    cropImageOptions = CropImageOptions(
                                        guidelines = CropImageView.Guidelines.ON_TOUCH,
                                        fixAspectRatio = false,
                                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                                        outputCompressQuality = 90,
                                        imageSourceIncludeCamera = false, // Không hiện option camera trong cropper
                                        imageSourceIncludeGallery = false, // Không hiện option gallery trong cropper

                                        activityBackgroundColor = Color.Black.toArgb(),
                                        toolbarColor = Color.Black.toArgb(),
                                        toolbarTitleColor = null,
                                        toolbarBackButtonColor = Color.White.toArgb(),
                                        toolbarTintColor= null,
                                        activityMenuTextColor = Color.White.toArgb(),
                                    )
                                )
                                cropImageLauncher.launch(cropOptions)
                            } ?: Toast.makeText(context, "No image to crop", Toast.LENGTH_SHORT).show()
                        },
                        enabled = uiState.currentImageUri != null && !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Crop Image")
                    }
                    IconButton(
                        onClick = { viewModel.saveCurrentImageToGallery(context) },
                        enabled = uiState.currentImageUri != null && !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Save Image")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading && uiState.saveSuccess == null) { // Chỉ hiện loading khi đang lưu
                    CircularProgressIndicator(color = Color.White)
                } else if (uiState.currentImageUri != null) {
                    GlideImage(
                        model = uiState.currentImageUri,
                        contentDescription = "Image to edit",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (uiState.error != null && uiState.currentImageUri == null) {
                    // Hiển thị lỗi nếu không có ảnh và có lỗi (ví dụ: lỗi URI ban đầu)
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else {
                    Text(
                        "Loading image or no image selected...",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = { viewModel.showImageInPopup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.currentImageUri != null && !uiState.isLoading
            ) {
                Text(
                    "Analyze",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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
fun AnalysisResultPopup(
    imageUri: Uri,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f) // Tăng nhẹ độ rộng popup
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Màu nền Card
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Preview Result",
                    style = MaterialTheme.typography.headlineSmall, // Sử dụng style từ theme
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Giữ tỷ lệ 1:1, hoặc bạn có thể điều chỉnh
                        .background(
                            Color(0xFFFFEBEE), // Màu hồng rất nhạt (Material Pink 50)
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            2.dp,
                            Color(0xFFF06292), // Màu hồng nhạt hơn (Material Pink 300)
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        model = imageUri,
                        contentDescription = "Analyzed image preview",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), // Clip ảnh bên trong cho mượt
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}