package com.project.speciesdetection.ui.features.observation.view.detail

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ExpandableText
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.observation.view.species_picker.SpeciesPickerView
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.ObservationDetailViewModel
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.UiEvent
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ObservationDetailView(
    navController: NavController,
    observationId: String?,
    observationDetailViewModel: ObservationDetailViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by observationDetailViewModel.uiState.collectAsStateWithLifecycle()
    val observation by observationDetailViewModel.observationState.collectAsStateWithLifecycle()
    val comment by observationDetailViewModel.commentsState.collectAsStateWithLifecycle()
    val commentSpecies by observationDetailViewModel.commentsSpeciesState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val currentLang by observationDetailViewModel.currentLanguage.collectAsStateWithLifecycle()

    var newCommentContent by rememberSaveable { mutableStateOf("") }
    var newCommentImage by rememberSaveable { mutableStateOf("") }
    val newCommentSpecies by observationDetailViewModel.newCommentSpecies.collectAsStateWithLifecycle()
    val commentSortState by observationDetailViewModel.commentSortState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val pagerState =
        rememberPagerState(pageCount = { observation?.imageURL?.size ?: 0 })
    var selectedComment by remember { mutableStateOf("") }
    var showSpeciesPicker by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    val isPostingComment by observationDetailViewModel.isPostingComment.collectAsStateWithLifecycle()


    LaunchedEffect(key1 = true) {
        if (uiState === UiState.Init) {
            observationDetailViewModel.startListening(observationId ?: "")
        }
    }

    val deleteMessage = stringResource(R.string.obs_delete_success)
    val moderation_error = stringResource(R.string.moderation_error)
    val comment_inappropriate = stringResource(R.string.comment_inappropriate)
    val image_inappropriate = stringResource(R.string.image_inappropriate)

    LaunchedEffect(key1 = true) {
        observationDetailViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.DeleteSuccess -> {
                    Toast.makeText(context, deleteMessage,Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }

                is UiEvent.PostCommentSuccess -> {
                    newCommentContent = ""
                    newCommentImage = ""
                    observationDetailViewModel.updateNewCommentSpecies(DisplayableSpecies())
                    val index = if (commentSortState) 1 else comment.size
                    coroutineScope.launch {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        listState.scrollToItem(
                            index = index,
                            scrollOffset = if (!commentSortState) 1000 else 0
                        )
                    }
                }

                is UiEvent.ShowSnackbar -> {
                    val message = when (event.message) {
                        "image_inappropriate" -> image_inappropriate
                        "moderation_error" -> moderation_error
                        "comment_inappropriate" -> comment_inappropriate
                        else -> ""
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    if (selectedComment.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { selectedComment = "" }
        ) {
            TextButton(
                onClick = {
                    observationDetailViewModel.deleteComment(
                        observationId ?: "",
                        selectedComment
                    )
                    selectedComment = ""
                },
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(
                    painterResource(R.drawable.trash_can_solid), null,
                    tint = MaterialTheme.colorScheme.error.copy(0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    stringResource(R.string.delete_comment),
                    modifier = Modifier.padding(start=20.dp)
                )
            }
        }
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(R.string.confirm_delete_obs)) },
            text = { Text(text = stringResource(R.string.confirm_delete_obs_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        coroutineScope.launch {
                            observationDetailViewModel.deleteObservation(observationId!!)
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showImagePicker) {
        ImageSourceSelectionBottomSheet(
            showBottomSheet = true,
            onDismissRequest = { showImagePicker = false },
            onImageSelected = { uri ->
                newCommentImage = uri.toString()
                showImagePicker = false
            },
            chooseVideo = true
        )
    }

    if (showSpeciesPicker) {
        SpeciesPickerView(
            onDismissRequest = { showSpeciesPicker = false },
            onSpeciesSelected = { observationDetailViewModel.updateNewCommentSpecies(it) }
        )
    }


    when (uiState) {
        UiState.Loading -> {

            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }


        }

        UiState.Success -> {

            LaunchedEffect(Unit) {
                observationDetailViewModel.toastMessage.collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            Scaffold(
                modifier = Modifier.navigationBarsPadding(),
                topBar = {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = {},
                        navigationIcon = {

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = {
                                        navController.popBackStack()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Default.ArrowBack, null
                                    )
                                }
                                if (authState.currentUser != null) {
                                    if (authState.currentUser!!.uid == observation?.uid) {
                                        var expanded by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier.padding(5.dp)
                                        ) {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "More options"
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.edit)) },
                                                    onClick = {
                                                        navController.navigate(
                                                            AppScreen.UpdateObservationScreen.buildRouteForEdit(
                                                                observation!!
                                                            )
                                                        )

                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete_obs)) },
                                                    onClick = {
                                                        showDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 15.dp, horizontal = 10.dp)
                            .imePadding()
                    ) {
                        CustomTextField(
                            value = newCommentContent,
                            onValueChange = { newCommentContent = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    stringResource(R.string.comment_placeholder),
                                    fontStyle = FontStyle.Italic
                                )
                            },
                            minLines = 1,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedPlaceholderColor = Color.Transparent,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.primary.copy(0.8f),
                            paddingValues = 15.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                if (newCommentImage.isEmpty() && newCommentSpecies.id.isEmpty()) {
                                    IconButton(
                                        onClick = {
                                            showImagePicker = true
                                        }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.camera), null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showSpeciesPicker = true }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.otter_solid), null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                enabled = (newCommentSpecies.id.isNotEmpty() || newCommentContent.isNotEmpty() || newCommentImage.isNotEmpty()) && !isPostingComment,
                                onClick = {
                                    observationDetailViewModel.postComment(
                                        comment = Comment(
                                            userId = authState.currentUserInformation?.uid ?: "",
                                            userImage = authState.currentUserInformation?.photoUrl
                                                ?: "",
                                            userName = authState.currentUserInformation?.name ?: "",
                                            content = newCommentContent,
                                            imageUrl = newCommentImage,
                                            speciesId = newCommentSpecies.id
                                        )
                                    )
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    if (isPostingComment) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = if ((newCommentSpecies.id.isNotEmpty() || newCommentContent.isNotEmpty() || newCommentImage.isNotEmpty()) && !isPostingComment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (newCommentImage.isNotEmpty()) {
                            val mimeType =
                                context.contentResolver.getType(
                                    Uri.decode(newCommentImage.toString()).toUri()
                                ) ?: ""
                            Box(
                                modifier = Modifier.size(100.dp)
                            ) {
                                GlideImage(
                                    model = newCommentImage,
                                    contentDescription = "Ảnh quan sát",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            navController.navigate(
                                                AppScreen.FullScreenImageViewer.createRoute(
                                                    newCommentImage
                                                )
                                            ) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
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
                                    onClick = { newCommentImage = "" },
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
                        if (newCommentSpecies.id.isNotEmpty()) {
                            Box {
                                SpeciesListItem(
                                    newCommentSpecies,
                                    newCommentSpecies.haveObservation,
                                    onClick = {
                                        navController.navigate(
                                            AppScreen.EncyclopediaDetailScreen.createRoute(
                                                species = commentSpecies[newCommentSpecies.id]
                                                    ?: DisplayableSpecies(),
                                                imageUri = null
                                            )
                                        ) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        observationDetailViewModel.updateNewCommentSpecies(
                                            DisplayableSpecies()
                                        )
                                    },
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
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    state = listState
                )
                {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {


                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlideImage(
                                    model = observation?.userImage ?: "",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .size(40.dp)
                                        .clickable {
                                            navController.navigate(
                                                AppScreen.CommunityProfileMainScreen.createRoute(
                                                    uid = observation?.uid ?: ""
                                                )
                                            )

                                        },
                                    loading = placeholder(R.drawable.error_image),
                                    failure = placeholder(R.drawable.error_image)
                                )
                                Column {
                                    Text(
                                        observation?.userName ?: "Unknown User",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            navController.navigate(
                                                AppScreen.CommunityProfileMainScreen.createRoute(
                                                    uid = observation?.uid ?: ""
                                                )
                                            )
                                        }
                                    )
                                    if (observation?.dateUpdated != null)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            when (observation?.privacy) {
                                                "Public" -> {
                                                    Icon(
                                                        painterResource(R.drawable.globe_solid),
                                                        null,
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                "Private" -> {
                                                    Icon(
                                                        Icons.Default.Lock,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                stringResource(R.string.updated_on) + " " +
                                                        SimpleDateFormat(
                                                            "dd/MM/yy",
                                                            Locale.getDefault()
                                                        ).format(
                                                            observation?.dateUpdated?.toDate() ?: 0
                                                        ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                }
                            }


                        }
                    }
                    if (observation?.content?.isNotEmpty() == true) {
                        item {
                            Row(modifier = Modifier.padding(horizontal = 15.dp)) {
                                ExpandableText(
                                    Modifier,
                                    observation?.content ?: ""
                                )
                            }
                        }
                    }
                    if (pagerState.pageCount > 0)
                        item {

                            Row(modifier = Modifier.padding(horizontal = 15.dp)) {
                                Box(Modifier.fillMaxWidth()) {
                                    HorizontalPager(pagerState) { page ->
                                        Box {
                                            GlideImage(
                                                model = CloudinaryImageURLHelper.restoreCloudinaryOriginalUrl(
                                                    observation?.imageURL?.get(page) ?: ""
                                                ),
                                                contentDescription = null,
                                                failure = placeholder(R.drawable.image_not_available),
                                                loading = placeholder(R.drawable.image_not_available),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(300.dp)
                                                    .clickable {
                                                        navController.navigate(
                                                            AppScreen.FullScreenImageViewer.createRoute(
                                                                observation?.imageURL?.get(page)
                                                                    ?: ""
                                                            )
                                                        ) {
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                            if (observation?.imageURL?.get(page)
                                                    ?.contains("/video/") == true
                                            ) {

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


                                    }
                                    if (pagerState.pageCount > 1) {
                                        LazyRow(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter),
                                            contentPadding = PaddingValues(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(pagerState.pageCount) { index ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(100))
                                                        .size(
                                                            if (pagerState.currentPage == index) 12.dp
                                                            else 10.dp
                                                        )
                                                        .background(
                                                            if (pagerState.currentPage == index) Color.White
                                                            else Color.Transparent
                                                        )
                                                        .border(
                                                            width = if (pagerState.currentPage == index) 4.dp else 2.dp,
                                                            color = Color.White,
                                                            shape = RoundedCornerShape(100)
                                                        )
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                pagerState.animateScrollToPage(index)
                                                            }

                                                        }
                                                )
                                                Spacer(Modifier.width(5.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    item {
                        if (observation?.location != null) {
                            val gmmIntentUri =
                                "geo:${observation?.location?.latitude},${observation?.location?.longitude}?q=${
                                    Uri.encode(observation?.locationName)
                                }".toUri()
                            LazyRow(modifier = Modifier
                                .padding(horizontal = 15.dp)
                                .clickable {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                                    // Chỉ định rõ ràng mở bằng Google Maps
                                    // Nếu không set, hệ thống sẽ hỏi người dùng chọn ứng dụng bản đồ nào
                                    //mapIntent.setPackage("com.google.android.apps.maps")

                                    // Kiểm tra xem có ứng dụng nào có thể xử lý Intent này không
                                    // để tránh bị crash app
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                    }
                                }) {
                                item {
                                    Icon(
                                        Icons.Default.LocationOn, null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 5.dp)
                                    )
                                    Text(
                                        observation?.locationName ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                }
                            }
                        }


                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 15.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row {
                                Icon(
                                    Icons.Default.DateRange, null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 5.dp)
                                )
                                Text(
                                    SimpleDateFormat("HH:mm, dd/MM/yy", Locale.getDefault()).format(
                                        observation?.dateFound?.toDate() ?: 0
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row() {
                                Icon(
                                    painterResource(R.drawable.otter_solid), null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 5.dp)
                                )
                                Text(
                                    observation?.speciesName?.get(currentLang)
                                        ?: observation?.speciesName?.get("default")
                                        ?: observation?.speciesId ?: "Unknown Species",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Like / Point / Dislike group
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        observationDetailViewModel.likeObservation(
                                            observationId = observation?.id ?: "",
                                            authState.currentUser!!.uid
                                        )
                                    },
                                    enabled = authState.currentUser != null
                                ) {

                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = if ((authState.currentUser != null &&
                                                    observation?.likeUserIds?.contains(authState.currentUser!!.uid) == true
                                                    )
                                        ) Color(9, 184, 73)
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Text(
                                    observation?.point.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        observationDetailViewModel.dislikeObservation(
                                            observationId = observation?.id ?: "",
                                            authState.currentUser!!.uid
                                        )
                                    },
                                    enabled = authState.currentUser != null
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (authState.currentUser != null &&
                                            observation?.dislikeUserIds?.contains(authState.currentUser!!.uid) == true
                                        ) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Comment count
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.message_circle),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(25.dp)
                                )

                                Text(
                                    observation?.commentCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            val isSaved =
                                observation?.savedBy?.contains(authState.currentUser?.uid) == true
                            var isSaving by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    enabled = authState.currentUser != null && !isSaving,
                                    onClick = {
                                        isSaving = true
                                        observationDetailViewModel.saveObservation(
                                            observation?.id ?: "", authState.currentUser!!.uid
                                        )
                                        // Reset lại sau delay giả hoặc sau khi ViewModel cập nhật (tuỳ bạn chọn)
                                        coroutineScope.launch {
                                            delay(800)
                                            isSaving = false
                                        }


                                    }
                                )
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 4.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = if (isSaved) Color(
                                            212,
                                            145,
                                            30
                                        ) else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    stringResource(R.string.save),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (!isSaved) MaterialTheme.colorScheme.outline
                                    else Color(212, 145, 30),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontWeight = if (isSaved) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    //Comment-section
                    if (comment.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .clickable {

                                            observationDetailViewModel.sortComment()
                                        }
                                        .padding(horizontal = 15.dp)) {
                                    Text(
                                        if (commentSortState) stringResource(R.string.newest) else stringResource(R.string.oldest),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        if (commentSortState) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                }
                            }
                        }


                        items(
                            items = comment,
                            key = { commentItem -> commentItem.id!! }
                        ) { currentComment ->

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .padding(horizontal = 15.dp)
                                    .fillMaxWidth()
                                    .combinedClickable
                                        (onLongClick = {
                                        if (authState.currentUser != null)
                                            if (authState.currentUser!!.uid == (observation?.uid
                                                    ?: "") || authState.currentUser!!.uid == currentComment.userId
                                            ) selectedComment = currentComment.id ?: ""
                                    }) {}
                            ) {
                                Row {
                                    GlideImage(
                                        model = currentComment.userImage ?: "",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100))
                                            .size(40.dp)
                                            .clickable {
                                                navController.navigate(
                                                    AppScreen.CommunityProfileMainScreen.createRoute(
                                                        uid = currentComment.userId ?: ""
                                                    )
                                                )

                                            },
                                        loading = placeholder(R.drawable.error_image),
                                        failure = placeholder(R.drawable.error_image)
                                    )
                                    Column(
                                        modifier = Modifier.padding(horizontal = 15.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainer,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(vertical = 10.dp, horizontal = 15.dp)
                                        ) {
                                            Row() {
                                                Text(
                                                    currentComment.userName ?: "Unknown User",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        navController.navigate(
                                                            AppScreen.CommunityProfileMainScreen.createRoute(
                                                                uid = currentComment.userId ?: ""
                                                            )
                                                        )
                                                    }
                                                )
                                            }
                                            if (currentComment.content.isNotEmpty()) {
                                                Row() {
                                                    Text(
                                                        currentComment.content,
                                                    )
                                                }
                                            }
                                        }

                                        if (currentComment.speciesId != "") {
                                            Text(
                                                stringResource(R.string.comment_species_suggestion),
                                                color = MaterialTheme.colorScheme.outline,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (commentSpecies[currentComment.id] == null) {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 4.dp
                                                    )
                                                }

                                            } else {
                                                SpeciesListItem(
                                                    observationState = commentSpecies[currentComment.id]?.haveObservation
                                                        ?: false,
                                                    species = commentSpecies[currentComment.id]
                                                        ?: DisplayableSpecies(),
                                                    onClick = {
                                                        navController.navigate(
                                                            AppScreen.EncyclopediaDetailScreen.createRoute(
                                                                species = commentSpecies[currentComment.id]
                                                                    ?: DisplayableSpecies(),
                                                                imageUri = null
                                                            )
                                                        ) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        if (currentComment.imageUrl.isNotEmpty()) {
                                            Row() {
                                                val mimeType =
                                                    if (currentComment.imageUrl.contains("/video/upload/")
                                                    ) "video" else "image"
                                                Box {
                                                    GlideImage(
                                                        model = currentComment.imageUrl,
                                                        contentDescription = null,
                                                        loading = placeholder(R.drawable.error_image),
                                                        failure = placeholder(R.drawable.error_image),
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(100.dp)
                                                            .padding(MaterialTheme.spacing.xxxs)
                                                            .clip(MaterialTheme.shapes.small)
                                                            .clickable {
                                                                navController.navigate(
                                                                    AppScreen.FullScreenImageViewer.createRoute(
                                                                        currentComment.imageUrl
                                                                    )
                                                                ) {
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                    )
                                                    if (mimeType == "video") {
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
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                SimpleDateFormat(
                                                    "dd/MM/yy",
                                                    Locale.getDefault()
                                                ).format(
                                                    currentComment.timestamp?.toDate() ?: 0
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            IconButton(
                                                onClick = {
                                                    observationDetailViewModel.likeComment(
                                                        currentComment.id ?: "",
                                                        authState.currentUser!!.uid
                                                    )
                                                },
                                                enabled = authState.currentUser != null
                                            ) {

                                                Icon(
                                                    Icons.Default.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = if ((authState.currentUser != null && currentComment.likeUserIds.contains(
                                                            authState.currentUser!!.uid
                                                        )
                                                                )
                                                    ) Color(9, 184, 73)
                                                    else MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }

                                            Text(
                                                currentComment.likeCount.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    observationDetailViewModel.dislikeComment(
                                                        currentComment.id ?: "",
                                                        authState.currentUser!!.uid
                                                    )
                                                },
                                                enabled = authState.currentUser != null
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = if (authState.currentUser != null && currentComment.dislikeUserIds.contains(
                                                            authState.currentUser!!.uid
                                                        )
                                                    ) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }


                                        }
                                    }
                                }


                            }


                        }

                    } else {
                        item {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.comment_empty),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                    }
                }


            }
        }

        UiState.Error("violated") -> {
            if (authState.currentUser?.uid != observation?.uid) {
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
                                observationDetailViewModel.startListening(observationId ?: "")
                            }
                        }
                    }
                }
            } else {

            }

        }

        else -> {
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
                            observationDetailViewModel.startListening(observationId ?: "")
                        }
                    }
                }
            }
        }
    }

}