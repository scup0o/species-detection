// file: ui/features/observation/view/UpdateObservation.kt

package com.project.speciesdetection.ui.features.observation.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.DateTimePicker
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.observation.view.species_picker.SpeciesPickerView
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEvent
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationViewModel

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
    var showSpeciesPicker by remember { mutableStateOf(false) }

    val firstImageUriForPicker = remember(uiState.images, uiState.isEditing) {
        if (!uiState.isEditing) {
            uiState.images.firstOrNull { image ->
                try {
                    val uri = image as Uri
                    context.contentResolver.getType(uri)?.startsWith("image/") == true
                } catch (e: Exception) {
                    false
                }
            } as? Uri
        } else {
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val moderation_error = stringResource(R.string.moderation_error)
    val empty_species = stringResource(R.string.empty_species)
    val text_unappropriated = stringResource(R.string.text_unappropriated)
    val images_unappropriated = stringResource(R.string.images_unappropriated)

    LaunchedEffect(key1 = true) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEffect.ShowError -> {
                    val messageText = when (effect.message) {
                        "moderation_error" -> moderation_error
                        "empty_species" -> empty_species
                        "text_unappropriated" -> text_unappropriated
                        "images_unappropriated" -> images_unappropriated
                        else -> effect.message
                    }
                    Toast.makeText(context,messageText,Toast.LENGTH_SHORT).show()
                }

                is com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEffect.NavigateToFullScreenImage -> {
                    navController.navigate(AppScreen.FullScreenImageViewer.createRoute(effect.image))
                }
            }
        }
    }

    val sucessMessage = stringResource(R.string.update_success)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(
                context,
                sucessMessage,
                Toast.LENGTH_SHORT
            ).show()
            onSaveSuccess()
        }
    }

    if (showSpeciesPicker) {
        SpeciesPickerView(
            imageUri = firstImageUriForPicker,
            onDismissRequest = { showSpeciesPicker = false },
            onSpeciesSelected = { selectedSpecies ->
                viewModel.onEvent(ObservationEvent.OnSpeciesSelected(selectedSpecies))
                showSpeciesPicker = false
            }
        )
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
                .fillMaxSize()
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
                    text = if (uiState.isEditing) stringResource(R.string.edit_obs) else stringResource(
                        R.string.add_obs
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        authState.currentUserInformation?.let { user ->
                            viewModel.onEvent(ObservationEvent.SaveObservation(user))
                        }
                    },
                    enabled = !uiState.isLoading && (uiState.description.isNotEmpty() || uiState.images.isNotEmpty()),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Check, "Lưu")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
                    placeholder = {
                        Text(
                            stringResource(R.string.obs_description),
                            fontStyle = FontStyle.Italic
                        )
                    },
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
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
                    paddingValues = if (uiState.description.isNotEmpty()) 15.dp else 0.dp,
                    shape = RoundedCornerShape(10)
                )
                Spacer(Modifier.height(16.dp))

                SpeciesInputRow(
                    speciesName = uiState.speciesName,
                    speciesId = uiState.speciesId, // <-- Truyền speciesId vào
                    isLocked = uiState.isSpeciesLocked, // <-- Truyền cờ khóa vào
                    onValueChange = { viewModel.onEvent(ObservationEvent.OnSpeciesNameChange(it)) },
                    onPickSpeciesClick = { showSpeciesPicker = true },
                    onClearClick = { viewModel.onEvent(ObservationEvent.OnSpeciesClear) }
                )
                Spacer(Modifier.height(8.dp))

                InfoRow(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Vị trí") },
                    label = stringResource(R.string.obs_location),
                    value = uiState.locationDisplayName.ifBlank {
                        uiState.locationName.ifBlank {
                            stringResource(
                                R.string.obs_choose_location
                            )
                        }
                    },
                    hasValue = uiState.location != null,
                    onValueClick = { navController.navigate(AppScreen.MapPickerScreen.route) },
                    onClearClick = { viewModel.onEvent(ObservationEvent.OnLocationClear) }
                )

                InfoRow(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Ngày giờ") },
                    label = stringResource(R.string.obs_time),
                    value = uiState.dateFoundText,
                    hasValue = uiState.dateFound != null,
                    onValueClick = { showDateTimePicker = true },
                    showClearIcon = false, // Không cho xóa
                    onClearClick = {}
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

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesInputRow(
    speciesName: String,
    speciesId: String,
    isLocked: Boolean,
    onValueChange: (String) -> Unit,
    onPickSpeciesClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val showAsChip = speciesId.isNotEmpty() && !isLocked
    val showAsLockedChip = speciesId.isNotEmpty() && isLocked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.otter_solid),
            contentDescription = "Loài",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(modifier = Modifier.weight(1f)) {
            if (showAsChip || showAsLockedChip) {
                InputChip(
                    enabled = !isLocked,
                    selected = true,
                    onClick = onPickSpeciesClick,
                    label = {
                        LazyRow {
                            item {
                                Text(
                                    speciesName,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 10.dp, end = 5.dp),
                                )
                            }
                        }

                    },

                    trailingIcon = {
                        if (!isLocked) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Xóa",
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onClearClick() }

                            )
                        }
                    },
                    modifier = Modifier,
                    colors = InputChipDefaults.inputChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

            } else {
                CustomTextField(
                    value = speciesName,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.obs_choose_species),
                            fontStyle = FontStyle.Italic
                        )
                    },
                    singleLine = true,
                    unfocusedBorderColor =
                        if (speciesName.isNotEmpty()) MaterialTheme.colorScheme.outline.copy(
                            0.2f
                        )
                        else Color.Transparent,
                    focusedBorderColor = if (speciesName.isNotEmpty()) MaterialTheme.colorScheme.outline.copy(
                        0.2f
                    )
                    else Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedPlaceholderColor = Color.Transparent,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
                    paddingValues = if (speciesName.isNotEmpty()) 15.dp else 5.dp,
                    shape = RoundedCornerShape(10),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                )
            }
        }

        // Nút chọn từ danh sách chỉ hiển thị khi không bị khóa
        if (!isLocked) {
            IconButton(
                onClick = onPickSpeciesClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chọn loài từ danh sách")
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable (() -> Unit)? = null,
    label: String,
    value: String,
    hasValue: Boolean,
    onValueClick: () -> Unit,
    showClearIcon: Boolean = true,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onValueClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    icon()
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(16.dp))

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
    context: Context,
    isEditing: Boolean,
) {
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
            val image = images[index]
            val mimeType = if (!isEditing) {
                try {
                    context.contentResolver.getType(Uri.decode(image.toString()).toUri()) ?: ""
                } catch (e: Exception) {
                    ""
                }
            } else {
                if (image.toString().contains("/video/upload/")) "video" else "image"
            }
            Box(modifier = Modifier.size(100.dp)) {
                GlideImage(
                    model = image,
                    contentDescription = "Ảnh quan sát",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(image) },
                    contentScale = ContentScale.Crop
                )
                if (mimeType.startsWith("video/") || mimeType == "video") {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
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
    val items = listOf(stringResource(R.string.obs_public), stringResource(R.string.obs_private))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(stringResource(R.string.set_privacy), style = MaterialTheme.typography.bodyMedium)
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
                Text(
                    if (currentPrivacy == "Public") stringResource(R.string.obs_public) else stringResource(
                        R.string.obs_private
                    ), style = MaterialTheme.typography.labelLarge
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (selectionOption == "Public") stringResource(R.string.obs_public) else stringResource(
                                    R.string.obs_private
                                )
                            )
                        },
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