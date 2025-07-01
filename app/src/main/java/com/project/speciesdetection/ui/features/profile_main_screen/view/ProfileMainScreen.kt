package com.project.speciesdetection.ui.features.profile_main_screen.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.user.User
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.observation.view.species_observation.ObservationItem
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.UiState
import com.project.speciesdetection.ui.features.profile_main_screen.viewmodel.ProfileViewModel
import com.project.speciesdetection.ui.features.profile_main_screen.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileMainScreen(
    containerColor: Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    uid: String,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val selfCheckState by remember { mutableStateOf(uid.isEmpty() || (authState.currentUser != null && authState.currentUser?.uid == uid)) }
    val user by profileViewModel.user.collectAsStateWithLifecycle()
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val lazyPagingItems = profileViewModel.observationPagingData.collectAsLazyPagingItems()
    val updatedObservations by profileViewModel.updatedObservations.collectAsStateWithLifecycle()
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val observationSortState by profileViewModel.sortByDesc.collectAsStateWithLifecycle()
    val currentLang by profileViewModel.currentLanguage.collectAsStateWithLifecycle()
    val selectedTab by profileViewModel.selectedTab.collectAsStateWithLifecycle()
    val observationCount by profileViewModel.observationCount.collectAsStateWithLifecycle()
    val observedSpecies by profileViewModel.observedSpecies.collectAsStateWithLifecycle()
    val speciesSortState by profileViewModel.speciesSortState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.currentUserInformation) {
        if (selfCheckState) {
            profileViewModel.updateUser(authState.currentUserInformation ?: User())
        } else {
            profileViewModel.retrieveUserInformation(uid)
        }
    }
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val moderation_error = stringResource(R.string.moderation_error)
    val images_unappropriated = stringResource(R.string.images_unappropriated)
    val display_name_inappropriate = stringResource(R.string.display_name_inappropriate)
    val display_name_empty = stringResource(R.string.display_name_empty)

    LaunchedEffect(key1 = true) {
        profileViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.UpdateProfileSuccess -> {
                    showEditDialog = false
                    authViewModel.checkCurrentUser()
                }

                is UiEvent.ShowError -> {
                    val message = when(event.message) {
                        "moderation_error" -> moderation_error
                        "images_unappropriated" -> images_unappropriated
                        "name_inappropriate" -> display_name_inappropriate
                        "name_empty" -> display_name_empty
                        else -> ""
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isUpdating by profileViewModel.isUpdatingProfile.collectAsStateWithLifecycle()

    // ===================================================================
    // BẮT ĐẦU PHẦN CODE CỦA DIALOG ĐƯỢC GỘP VÀO
    // ===================================================================
    if (showEditDialog) {
        // State cho tên được quản lý cục bộ trong dialog và reset mỗi khi dialog mở
        val newName by profileViewModel.newNameInDialog.collectAsStateWithLifecycle()
        // State cho ảnh mới được lấy từ ViewModel để sống sót qua vòng đời
        val newPhotoUri by profileViewModel.newCroppedPhotoUri.collectAsStateWithLifecycle()

        var showImageSourceSelector by remember { mutableStateOf(false) }

        val hasChanges = remember(newName, newPhotoUri, user.name) {
            (newName.isNotBlank() && newName != user.name) || newPhotoUri != null
        }

        val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()){ result ->
            showImageSourceSelector = false
            if (result.isSuccessful) {
                // Cập nhật Uri vào ViewModel
                profileViewModel.onPhotoCropped(result.uriContent)
            }
        }

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                if (!isUpdating) {
                    showEditDialog = false
                    // Dọn dẹp Uri khi đóng dialog
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (hasChanges) {
                            // `newPhotoUri` ở đây là state từ ViewModel, luôn hợp lệ
                            profileViewModel.updateUserProfile(newName, newPhotoUri)
                        }
                    },
                    enabled = !isUpdating && hasChanges
                ) {
                    Box(
                        modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.update))
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUpdating) {
                            showEditDialog = false
                            profileViewModel.onPhotoCropped(null)
                        }
                    },
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
                        GlideImage(
                            model = newPhotoUri ?: user.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                                    loading = placeholder(R.drawable.error_image)

                        )
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
                        onValueChange = { profileViewModel.onNameInDialogChanged(it) },
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
        }
    }
    /*if (showEditDialog) {
        EditProfileDialog(
            user = user,
            isUpdating = isUpdating,
            onDismissRequest = {
                if (!isUpdating) {
                    showEditDialog = false
                }
            },
            onUpdateProfile = { newName, newPhotoUri ->
                profileViewModel.updateUserProfile(newName, newPhotoUri)
            },
            viewModel = profileViewModel
        )
    }*/

    when (uiState) {
        is UiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack, null
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Row(modifier = Modifier.align(Alignment.Center)) {
                        ErrorScreenPlaceholder {
                            if (selfCheckState) {
                                profileViewModel.updateUser(
                                    authState.currentUserInformation ?: User()
                                )
                            } else {
                                profileViewModel.retrieveUserInformation(uid)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        BottomNavigationBar(navController)
                    }
                }
            }
        }

        UiState.Init -> {
            Scaffold(

            ) {
                Box(
                    modifier = Modifier

                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        BottomNavigationBar(navController)
                    }
                }
            }
        }

        UiState.Loading -> {
            Scaffold(

            ) {
                Box(
                    modifier = Modifier

                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        BottomNavigationBar(navController)
                    }
                }
            }
        }

        UiState.Success -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
            ) { innerPadding ->
                val addedItems = remember(
                    updatedObservations,
                    lazyPagingItems.itemSnapshotList
                ) {
                    val pagedItemIds = lazyPagingItems.itemSnapshotList.items
                        .mapNotNull { it.id }
                        .toSet()

                    if (pagedItemIds.isEmpty() && lazyPagingItems.loadState.refresh is LoadState.Loading) {
                        emptyList()
                    } else {
                        updatedObservations.values
                            .filterNotNull()
                            .filter { obs ->
                                obs.id !in pagedItemIds
                            }
                            .sortedByDescending { it.dateCreated }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                    ) {
                        if (uid.isNotEmpty()) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack, null,
                                Modifier
                                    .clickable {
                                        navController.popBackStack()
                                    }
                                    .padding(horizontal = 15.dp)
                            )

                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 20.dp)
                                .padding(bottom = 15.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {


                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            ) {
                                if (user.photoUrl?.isNotEmpty() == true)
                                    GlideImage(
                                        model = user.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = placeholder(R.drawable.error_image),
                                        failure = placeholder(R.drawable.error_image)
                                    )
                                else {
                                    Icon(
                                        Icons.Default.AccountCircle, null,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f).padding(start=20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)


                            ) {
                                Text(
                                    user.name ?: "Unknown User",
                                    style = MaterialTheme.typography.titleLarge,
                                )


                                Row(
                                    modifier = Modifier.fillMaxWidth(0.9f), // Chiếm toàn bộ chiều rộng của Column cha
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$observationCount",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.obs),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            profileViewModel.selectTab(2)
                                        }
                                    ) {
                                        Text(
                                            text = "${observedSpecies.size}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.species),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }


                            Box(
                                modifier = Modifier
                                    .clickable { profileViewModel.onEditProfileDialogOpened() // Chuẩn bị state cho dialog
                                        showEditDialog = true }
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = CircleShape
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }


                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tabs = if (selfCheckState)
                                listOf(
                                    Pair(R.drawable.binoculars, 1),
                                    Pair(R.drawable.otter_solid, 2),
                                    Pair(R.drawable.star, 3),
                                    Pair(R.drawable.archive, 4)
                                )
                            else
                                listOf(
                                    Pair(R.drawable.binoculars, 1),
                                    Pair(R.drawable.otter_solid, 2),
                                )

                            tabs.forEach { (icon, tabIndex) ->
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { profileViewModel.selectTab(tabIndex) }
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            painterResource(icon),
                                            contentDescription = null,
                                            tint = if (selectedTab == tabIndex)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        if (selectedTab == tabIndex) {
                                            Box(
                                                Modifier
                                                    .padding(top = 8.dp)
                                                    .height(3.dp)
                                                    .width(24.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(
                                                            20.dp,
                                                            20.dp,
                                                            0.dp,
                                                            0.dp
                                                        )
                                                    )
                                            )
                                        } else {
                                        }
                                    }
                                }
                            }
                        }
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer.copy(0.8f),
                                    RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
                                )
                                .padding(top = 10.dp)
                                .padding(horizontal = 15.dp),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            if (selectedTab == 2) {
                                if (observedSpecies.isEmpty() && observationCount > 0) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                } else if (observedSpecies.isEmpty() && observationCount == 0) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.empty_species),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()

                                        ) {
                                            Row(
                                                Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .clickable {
                                                        profileViewModel.setSortOrder(
                                                            if (speciesSortState == ProfileViewModel.SortOrder.BY_NAME_ASC) ProfileViewModel.SortOrder.BY_NAME_DESC
                                                            else ProfileViewModel.SortOrder.BY_NAME_ASC
                                                        )
                                                    }
                                                    .padding(horizontal = 15.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                                                Icon(
                                                    if (speciesSortState == ProfileViewModel.SortOrder.BY_NAME_ASC) painterResource(
                                                        R.drawable.sort_az
                                                    ) else painterResource(R.drawable.sort_za),
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    stringResource(R.string.name_label),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )


                                            }
                                        }
                                    }
                                    items(
                                        count = observedSpecies.size,
                                        key = { index -> observedSpecies[index].id!! }
                                    ) { index ->
                                        val species = observedSpecies[index]
                                        SpeciesListItem(
                                            showObservationState = false,
                                            species = species,
                                            onClick = {
                                                navController.navigate(
                                                    AppScreen.EncyclopediaDetailScreen.createRoute(
                                                        species,
                                                        null
                                                    )
                                                )
                                            }
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(80.dp))
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()

                                    ) {
                                        Row(
                                            Modifier
                                                .align(Alignment.CenterEnd)
                                                .clickable {
                                                    profileViewModel.updateSortDirection()
                                                }
                                                .padding(horizontal = 15.dp)) {
                                            Text(
                                                if (observationSortState) stringResource(R.string.newest) else stringResource(
                                                    R.string.oldest
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                if (observationSortState) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )


                                        }
                                    }
                                }
                                if (!isRefreshing) {

                                    if (lazyPagingItems.itemCount == 0) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (selectedTab == 1) stringResource(R.string.no_obs) else {
                                                        if (selectedTab == 4) stringResource(R.string.empty_archived) else stringResource(
                                                            R.string.empty_saved
                                                        )
                                                    }, color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }

                                    items(
                                        count = addedItems.size,
                                        key = { index -> "added_${addedItems[index].id}" } // Key phải là duy nhất
                                    ) { index ->
                                        val newObservation = addedItems[index]
                                        ObservationItem(
                                            currentLang = currentLang,
                                            showSpecies = true,
                                            observation = newObservation,
                                            onclick = {
                                                navController.navigate(
                                                    AppScreen.ObservationDetailScreen.createRoute(
                                                        newObservation.id ?: ""
                                                    )
                                                )
                                            }
                                        )


                                    }

                                    items(
                                        count = lazyPagingItems.itemCount,
                                        key = lazyPagingItems.itemKey { it.id!! }
                                    ) { index ->
                                        val pagedObservation = lazyPagingItems[index]
                                        if (pagedObservation != null) {
                                            val finalObservation =
                                                updatedObservations.getOrDefault(
                                                    pagedObservation.id!!,
                                                    pagedObservation
                                                )

                                            if (finalObservation != null) {
                                                Column(
                                                ) {

                                                    if (selectedTab == 4) {
                                                        // Phần hiển thị "Sẽ bị xóa sau X ngày"
                                                        val millisIn30Days =
                                                            30 * 24 * 60 * 60 * 1000L
                                                        val now = System.currentTimeMillis()
                                                        val millisSinceUpdate =
                                                            now - (finalObservation.dateUpdated?.toDate()?.time
                                                                ?: 0)
                                                        val millisRemaining =
                                                            millisIn30Days - millisSinceUpdate
                                                        val daysRemaining =
                                                            (millisRemaining / (1000 * 60 * 60 * 24)).coerceAtLeast(
                                                                0
                                                            ) + 1

                                                        Column(
                                                            /*modifier =
                                                                Modifier.background(
                                                                    MaterialTheme.colorScheme.surface,
                                                                    RoundedCornerShape(20.dp)

                                                                )*/
                                                        ) {
                                                            ObservationItem(
                                                                currentLang = currentLang,
                                                                showSpecies = true,
                                                                observation = finalObservation,
                                                                onclick = {
                                                                    navController.navigate(
                                                                        AppScreen.ObservationDetailScreen.createRoute(
                                                                            finalObservation.id!!
                                                                        )
                                                                    )
                                                                }
                                                            )

                                                            /*HorizontalDivider(
                                                                modifier = Modifier.padding(
                                                                    bottom = 10.dp
                                                                )
                                                            )*/

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 15.dp)
                                                                    .padding(bottom = 15.dp, top=10.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = stringResource(
                                                                        R.string.expires_in_days,
                                                                        daysRemaining
                                                                    ),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.error
                                                                )

                                                                Row(
                                                                    modifier = Modifier.clickable {
                                                                        profileViewModel.restoreArchivedObservation(
                                                                            finalObservation.id!!
                                                                        )
                                                                    },
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(
                                                                        5.dp
                                                                    )
                                                                ) {
                                                                    Text(
                                                                        stringResource(R.string.restore),
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Icon(
                                                                        painterResource(R.drawable.resource_return),
                                                                        null,
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(15.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        ObservationItem(
                                                            currentLang = currentLang,
                                                            showSpecies = true,
                                                            observation = finalObservation,
                                                            onclick = {
                                                                navController.navigate(
                                                                    AppScreen.ObservationDetailScreen.createRoute(
                                                                        finalObservation.id!!
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(80.dp))
                                    }
                                }
                            }
                            lazyPagingItems.apply {
                                when (loadState.refresh) {
                                    is LoadState.Loading -> {
                                        items(3) { ListItemPlaceholder() }
                                    }

                                    is LoadState.Error -> {
                                        item {
                                            ErrorScreenPlaceholder { lazyPagingItems.refresh() }
                                        }
                                    }

                                    else -> {}
                                }
                                when (loadState.append) {
                                    is LoadState.Loading -> {
                                        item(3) {
                                            ListItemPlaceholder()
                                        }
                                    }

                                    is LoadState.Error -> {
                                        item {
                                            ItemErrorPlaceholder { lazyPagingItems.refresh() }
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }




                    if (uid.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                        ) {
                            BottomNavigationBar(navController)
                        }
                    }
                }
            }
        }
    }

}