package com.project.speciesdetection.ui.features.identification_image_source.view // Hoặc package bạn muốn đặt

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.core.theme.strokes
import com.project.speciesdetection.ui.features.identification_image_source.viewmodel.ImageSourceSelectionViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MediaActionItem(
    val id: String,
    val label: Int,
    val icon: Int,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceSelectionBottomSheet(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    viewModel: ImageSourceSelectionViewModel = hiltViewModel(),
    chooseVideo: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tempImageFileForCleanup by remember { mutableStateOf<File?>(null) }
    var uriToLaunchCameraWith by remember { mutableStateOf<Uri?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImageSelected(it)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                uriToLaunchCameraWith?.let { capturedUri ->
                    onImageSelected(capturedUri)
                } ?: run {
                    Toast.makeText(
                        context,
                        context.getString(R.string.image_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                    tempImageFileForCleanup?.delete()
                }
            } else {
                //Toast.makeText(context, "Chụp ảnh bị hủy.", Toast.LENGTH_SHORT).show()
                tempImageFileForCleanup?.delete()
            }
            onDismissRequest()
            uriToLaunchCameraWith = null
            tempImageFileForCleanup = null
        }
    )


    val selectImageFromGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    )
    { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        } else {
            //Toast.makeText(context, "Chọn ảnh từ thư viện bị hủy.", Toast.LENGTH_SHORT).show()
        }
        onDismissRequest()
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scope.launch {
                val imageFileAndUriPair =
                    viewModel.createTemporaryImageFileUseCase.createImageFileAndUriInCache()
                if (imageFileAndUriPair != null) {
                    val (uriForCamera, fileForCamera) = imageFileAndUriPair
                    tempImageFileForCleanup = fileForCamera
                    uriToLaunchCameraWith = uriForCamera
                    takePictureLauncher.launch(uriForCamera)
                } else {
                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG)
                        .show()
                    onDismissRequest()
                }
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            onDismissRequest()
        }
    }

    // Danh sách các hành động media
    val mediaActions = listOf(
        MediaActionItem(
            id = "gallery",
            label = R.string.gallery_source_for_identification,
            icon = R.drawable.image,
            action = {
                if (chooseVideo) {
                    selectImageFromGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                } else {
                    selectImageFromGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }

        ),
        MediaActionItem(
            id = "camera",
            label = R.string.camera_source_for_identification,
            icon = R.drawable.camera,
            action = {
                when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        scope.launch {
                            val imageFileAndUriPair = viewModel.createTemporaryImageFile()
                            if (imageFileAndUriPair != null) {
                                val (uriForCamera, fileForCamera) = imageFileAndUriPair
                                tempImageFileForCleanup = fileForCamera
                                uriToLaunchCameraWith = uriForCamera
                                takePictureLauncher.launch(uriForCamera)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error),
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismissRequest()
                            }
                        }
                    }

                    else -> {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        )
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                onDismissRequest()
                if (uriToLaunchCameraWith != null || tempImageFileForCleanup != null) {
                    tempImageFileForCleanup?.delete()
                    uriToLaunchCameraWith = null
                    tempImageFileForCleanup = null
                    //Log.d("ImageSourceSelection", "Dismissed, cleaned up pending camera resources.")
                }
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /*Text(
                    "Chọn nguồn ảnh",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )*/
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    mediaActions.forEach { actionItem ->
                        OptionDisplayItem(
                            icon = painterResource(actionItem.icon),
                            text = actionItem.label,
                            onClick = actionItem.action
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }


}

@Composable
private fun OptionDisplayItem(
    icon: Painter,
    text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            border = BorderStroke(
                MaterialTheme.strokes.xs,
                MaterialTheme.colorScheme.outlineVariant
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(20.dp),
            colors = ButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.tertiary,
                disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor,
            )
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(stringResource(text), textAlign = TextAlign.Center)
    }
}

