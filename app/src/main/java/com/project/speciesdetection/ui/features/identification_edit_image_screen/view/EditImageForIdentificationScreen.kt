package com.project.speciesdetection.ui.features.identification_edit_image_screen.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap // Vẫn cần cho CropImageOptions
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.features.identification_analysis.view.AnalysisButton
import com.project.speciesdetection.ui.features.identification_analysis.view.AnalysisResultPBS
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisViewModel
import com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel.EditImageForIdentificationViewModel
import com.project.speciesdetection.ui.features.identification_edit_image_screen.viewmodel.EditImageUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditImageForIdentificationScreen(
    viewModel: EditImageForIdentificationViewModel = hiltViewModel(),

    navController: NavHostController
) {
    val editImageUiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val requestStoragePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.saveCurrentImageToGallery()
        else Toast.makeText(context, context.getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show()
    }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                viewModel.onImageCropped(result.uriContent)
            }
    }

    LaunchedEffect(editImageUiState.saveSuccess) {
        if (editImageUiState.saveSuccess == true) {
            Toast.makeText(context, context.getString(R.string.save_image_successfuly), Toast.LENGTH_SHORT).show()
            viewModel.resetSaveStatus()
        } else if (editImageUiState.saveSuccess == false && editImageUiState.error != null) {
            // viewModel.resetSaveStatus()
        }
    }
    LaunchedEffect(editImageUiState.error) {
        editImageUiState.error?.let {
            if (editImageUiState.saveSuccess != false) {
                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            }
            // viewModel.clearGeneralError() // Có thể gọi ở đây hoặc để người dùng tự xóa
        }
    }

    if (editImageUiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
            Text("Loading image...", color = Color.White, modifier = Modifier.padding(top = 70.dp))
        }
    } else if (editImageUiState.currentImageUri != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    },
                    actions = {
                        IconButton(onClick = { // Crop Button
                            val uriToCrop = editImageUiState.currentImageUri
                                uriToCrop?.let {
                                    val cropOptions = CropImageContractOptions(
                                        it,
                                        CropImageOptions(
                                            guidelines = CropImageView.Guidelines.ON_TOUCH,
                                            fixAspectRatio = false,
                                            outputCompressFormat = Bitmap.CompressFormat.JPEG,
                                            outputCompressQuality = 90,
                                            backgroundColor = Color.Black.toArgb(),
                                            toolbarColor = Color.Black.toArgb(),
                                            toolbarBackButtonColor = Color.White.toArgb(),
                                            toolbarTintColor = Color.White.toArgb(),
                                            //cropperLabelTextColor = Color.White.toArgb(),
                                            activityBackgroundColor = Color.Black.toArgb(),
                                            //toolbarTitleColor = Color.White.toArgb(),
                                            activityMenuTextColor = Color.White.toArgb()
                                        )
                                    )
                                    cropImageLauncher.launch(cropOptions)

                            }
                        }) { Icon(Icons.Default.Edit, "Crop") }
                        IconButton(onClick = { // Save Button
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    PackageManager.PERMISSION_GRANTED -> viewModel.saveCurrentImageToGallery()
                                    else -> requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            } else { viewModel.saveCurrentImageToGallery() }
                        }) { Icon(painterResource(R.drawable.download), "Save") }
                    }
                )
            },
            floatingActionButton = {
                AnalysisButton(
                    currentImageUriToAnalyze = editImageUiState.currentImageUri,
                    onAnalysisActionTriggered = {
                        // Khi nút được nhấn, EditImageViewModel sẽ hiển thị popup
                        viewModel.showAnalysisPopup()
                    },
                )
            },
            floatingActionButtonPosition = FabPosition.EndOverlay
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(paddingValues), contentAlignment = Alignment.Center) {
                GlideImage(model = editImageUiState.currentImageUri, contentDescription = "Image to edit", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }

            if (editImageUiState.showAnalysisPopup) {
                AnalysisResultPBS(
                    analysisImage = editImageUiState.currentImageUri!!,
                    onDismiss = {
                        viewModel.dismissAnalysisPopup()
                    },
                    navController = navController
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = editImageUiState.error ?: "Cannot load image. Please try again.", color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {navController.popBackStack()}) { Text("Go Back") }
            }
        }
    }
}