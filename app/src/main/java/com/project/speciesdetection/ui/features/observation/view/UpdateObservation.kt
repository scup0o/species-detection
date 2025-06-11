package com.project.speciesdetection.ui.features.observation.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.android.gms.location.LocationServices
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.MediaHelper
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.DateTimePicker
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEvent
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationViewModel
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateObservation(
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: ObservationViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showImagePicker by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /*granted ->
        if (granted) {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentPoint = GeoPoint(it.latitude, it.longitude)
                        viewModel.reverseGeocode(currentPoint)
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Không thể truy cập vị trí: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bạn cần cấp quyền vị trí để dùng bản đồ", Toast.LENGTH_LONG).show()
        }*/
    }

    LaunchedEffect(Unit) {
        //Log.i("get location", "pr")
        if (!hasLocationPermission.value) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(key1 = true) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEffect.NavigateToFullScreenImage -> {
                    navController.navigate(AppScreen.FullScreenImageViewer.createRoute(effect.image))
                }
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(
                context,
                if (uiState.isEditing) "Cập nhật thành công!" else "Tạo thành công!",
                Toast.LENGTH_SHORT
            ).show()
            onSaveSuccess()
        }
    }

    if (showDateTimePicker) {
        DateTimePicker(
            onDateTimeSelected = { date ->
                viewModel.onEvent(ObservationEvent.OnDateSelected(date))
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Đóng") }
                Text(
                    text = if (uiState.isEditing) "Chỉnh sửa quan sát" else "Tạo quan sát mới",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        authState.currentUserInformation?.let { user ->
                            viewModel.onEvent(ObservationEvent.SaveObservation(user))
                        }
                    },
                    enabled = !uiState.isLoading && (uiState.description.isNotEmpty() || uiState.images.isNotEmpty()),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(Icons.Default.Check, "Đóng")
                    }
                }

            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                CustomTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onEvent(ObservationEvent.OnDescriptionChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập mô tả", fontStyle = FontStyle.Italic) },
                    minLines = 1,
                    unfocusedBorderColor =
                        if (uiState.description.isNotEmpty()) MaterialTheme.colorScheme.outline.copy(
                            0.2f
                        )
                        else Color.Transparent,
                    focusedBorderColor = if (uiState.description.isNotEmpty()) MaterialTheme.colorScheme.outline.copy(
                        0.2f
                    )
                    else Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedPlaceholderColor = Color.Transparent,
                    paddingValues = if (uiState.description.isNotEmpty()) 15.dp else 0.dp,
                    shape = RoundedCornerShape(10)
                )
                Spacer(Modifier.height(16.dp))



                InfoRow(
                    label = "add an observation for",
                    value = uiState.speciesName.ifEmpty { "Chọn loài từ danh sách hoặc tự nhập" },
                    hasValue = uiState.speciesName.isNotEmpty(),
                    showClearIcon = uiState.speciesId.isEmpty(),
                    onClearClick = {}
                )
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "tại",
                    value = uiState.locationName,
                    hasValue = uiState.location != null,
                    onValueClick = {

                        navController.navigate(AppScreen.MapPickerScreen.route)
                    },
                    onClearClick = {
                        viewModel.onEvent(ObservationEvent.OnLocationClear)
                    }
                )
                InfoRow(
                    icon = Icons.Default.DateRange,
                    label = "vào lúc",
                    value = uiState.dateFoundText,
                    hasValue = uiState.dateFound != null,
                    onValueClick = { showDateTimePicker = true },
                    showClearIcon = false,
                    onClearClick = {
                        //viewModel.onEvent(ObservationEvent.OnDateClear)
                    }
                )
                Spacer(Modifier.height(16.dp))

                ImageSelector(
                    isEditing = uiState.isEditing,
                    context = context,
                    images = uiState.images,
                    onAddClick = { showImagePicker = true },
                    onRemoveClick = { viewModel.onEvent(ObservationEvent.OnRemoveImage(it)) },
                    onImageClick = { viewModel.onEvent(ObservationEvent.OnImageClick(it)) }
                )
                Spacer(Modifier.height(16.dp))

                PrivacySelector(
                    currentPrivacy = uiState.privacy,
                    onPrivacySelected = { viewModel.onEvent(ObservationEvent.OnPrivacyChange(it)) }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showImagePicker) {
        ImageSourceSelectionBottomSheet(
            showBottomSheet = true,
            onDismissRequest = { showImagePicker = false },
            onImageSelected = { uri ->
                viewModel.onEvent(ObservationEvent.OnAddImage(uri))
                showImagePicker = false
            },
            chooseVideo = true
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector? = null,
    label: String,
    value: String,
    hasValue: Boolean,
    onValueClick: () -> Unit = {},
    showClearIcon: Boolean = true,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            //Spacer(Modifier.width(16.dp))
        }

        Text(
            text = label,
            modifier = Modifier,
            style = MaterialTheme.typography.bodyLarge
        )

        if (hasValue) {
            InputChip(
                selected = true,
                onClick = onValueClick,
                label = {
                    LazyRow {
                        item {
                            Text(
                                value,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                            )
                        }
                    }

                },

                trailingIcon = {
                    if (showClearIcon) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa",
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onClearClick() }

                        )
                    }
                },
                modifier = Modifier
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onValueClick)
                    .padding(start = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ImageSelector(
    images: List<Any>,
    onAddClick: () -> Unit,
    onRemoveClick: (Any) -> Unit,
    onImageClick: (Any) -> Unit,
    context : Context,
    isEditing : Boolean = false,
) {
    Log.i("image", images.toString())


    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Thêm ảnh",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(images.size, key = { index -> "${images[index].hashCode()}_$index" }) { index ->
            var image = images[index]
            val mimeType =
                if (!isEditing) context.contentResolver.getType(Uri.decode(image.toString()).toUri()) ?: ""
                else ""
            Box(
                modifier = Modifier.size(100.dp)
            ) {
                if (mimeType.startsWith("image/") || MediaHelper.isImageFile(image.toString())){
                    GlideImage(
                        model = image,
                        contentDescription = "Ảnh quan sát",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(image) },
                        contentScale = ContentScale.Crop
                    )
                }
                if (mimeType.startsWith("video/") || MediaHelper.isVideoFile(image.toString())){
                    Box{
                        GlideImage(
                            model = image,
                            contentDescription = "Ảnh quan sát",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(image) },
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }

                }


                IconButton(
                    onClick = { onRemoveClick(image) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Xóa ảnh",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySelector(
    currentPrivacy: String,
    onPrivacySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("Public", "Private")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text("Set privacy:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .menuAnchor()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPrivacy == "Public")
                    Icon(
                        painterResource(R.drawable.globe_solid),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                else
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                Spacer(Modifier.width(4.dp))
                Text(currentPrivacy, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onPrivacySelected(selectionOption)
                            expanded = false
                        },
                        leadingIcon = {
                            if (selectionOption == "Public")
                                Icon(
                                    painterResource(R.drawable.globe_solid),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            else
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                        }
                    )
                }
            }
        }
    }
}