package com.project.speciesdetection.ui.features.profile_main_screen.view

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.project.speciesdetection.R
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.profile_main_screen.viewmodel.ProfileViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EditProfileDialog(
    user: User,
    isUpdating: Boolean,
    onDismissRequest: () -> Unit,
    onUpdateProfile: (newName: String, newPhotoUri: Uri?) -> Unit,
    viewModel : ProfileViewModel
) {
   /* var newName by remember { mutableStateOf(user.name ?: "") }

    val newPhotoUri by viewModel.imageUri.collectAsStateWithLifecycle()

    var showImageSourceSelector by remember { mutableStateOf(false) }

    val hasChanges = remember(newName, newPhotoUri) {
        (newName.isNotBlank() && newName != user.name) || newPhotoUri != null
    }

    val context = LocalContext.current

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            viewModel.updateImageUri(result.uriContent)
        }
        showImageSourceSelector = false
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (hasChanges) {
                        onUpdateProfile(newName, newPhotoUri)
                    }
                },
                enabled = !isUpdating && hasChanges
            ) {
                Box(
                    modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.update))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isUpdating
            ) {
                Text(stringResource(R.string.dismiss))
            }
        },
        title = { },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(enabled = !isUpdating) { showImageSourceSelector = true }
                ) {
                    if (user.photoUrl?.isNotEmpty() == true) {
                        GlideImage(
                            model = newPhotoUri ?: user.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )

                    } else {
                        Icon(
                            Icons.Default.AccountCircle, null,
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    Icon(
                        painterResource(R.drawable.camera),
                        contentDescription = "Change Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )
            }
        }
    )

    if (showImageSourceSelector) {
        ImageSourceSelectionBottomSheet(
            showBottomSheet = true,
            onDismissRequest = { showImageSourceSelector = false },
            onImageSelected = { selectedUri ->
                showImageSourceSelector = false

                val cropOptions = CropImageContractOptions(
                    uri = selectedUri,
                    cropImageOptions = CropImageOptions(
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        guidelines = CropImageView.Guidelines.ON,
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 90,
                        toolbarColor = Color.Black.toArgb(),
                        toolbarBackButtonColor = Color.White.toArgb(),
                        toolbarTintColor = Color.White.toArgb(),
                        activityBackgroundColor = Color.Black.toArgb(),
                        toolbarTitleColor = Color.Black.toArgb(),
                        activityMenuTextColor = Color.Black.toArgb(),
                        guidelinesColor = Color.White.toArgb(),
                        cropMenuCropButtonIcon = R.drawable.checkmark
                    )
                )
                cropImageLauncher.launch(cropOptions)
            },
            chooseVideo = false
        )
    }*/
}