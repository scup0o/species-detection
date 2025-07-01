package com.project.speciesdetection.ui.features.community_main_screen.view

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.observation.Comment
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.CustomTextField
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthState
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet
import com.project.speciesdetection.ui.features.observation.view.species_picker.SpeciesPickerView
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.ObservationDetailViewModel
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.UiEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ObservationCommentsSheet(
    observation: Observation,
    navController: NavController,
    onDismissRequest: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    detailViewModel: ObservationDetailViewModel = hiltViewModel(key = "comments_${observation.id}")
) {
    val comments by detailViewModel.commentsState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val commentSortState by detailViewModel.commentSortState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val commentSpecies by detailViewModel.commentsSpeciesState.collectAsStateWithLifecycle()
    var newCommentContent by rememberSaveable { mutableStateOf("") }
    var newCommentImage by rememberSaveable { mutableStateOf("") }
    val newCommentSpecies by detailViewModel.newCommentSpecies.collectAsStateWithLifecycle()
    // var newCommentImage by rememberSaveable { mutableStateOf("") }
    // val newCommentSpecies by detailViewModel.newCommentSpecies.collectAsStateWithLifecycle()
    var showSpeciesPicker by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = observation.id) {
        detailViewModel.startListeningToComments(observation.id?:"")
    }

    LaunchedEffect(key1 = true) {
        detailViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.PostCommentSuccess -> {
                    newCommentContent = ""
                    newCommentImage=""
                    detailViewModel.updateNewCommentSpecies(DisplayableSpecies())
                    val index = if (commentSortState) 0 else comments.size
                    coroutineScope.launch {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (comments.isNotEmpty()) listState.animateScrollToItem(index)
                    }
                }
                is UiEvent.ShowSnackbar -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                UiEvent.DeleteSuccess -> {}
            }
        }
    }
    var selectedComment by remember { mutableStateOf("") }


    if (selectedComment.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { selectedComment = "" }
        ) {
            TextButton(
                onClick = {
                    detailViewModel.deleteComment(observation.id ?: "", selectedComment)
                    selectedComment =""
                },
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(
                    painterResource(R.drawable.trash_can_solid), null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    stringResource(R.string.delete_comment),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
            onSpeciesSelected = { detailViewModel.updateNewCommentSpecies(it) }
        )
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Scaffold(
            topBar = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Close")
                    }
                    if (comments.isNotEmpty()) {
                        Row(
                            Modifier
                                .clickable {
                                    detailViewModel.sortComment()
                                }
                                .padding(horizontal = 15.dp)
                        ) {
                            Text(
                                if (commentSortState) "Newest" else "Oldest",
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
                        placeholder = { Text(stringResource(R.string.comment_placeholder), fontStyle = FontStyle.Italic) },
                        minLines = 1,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.outlineVariant,
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
                            enabled = newCommentSpecies.id != "" || newCommentContent.isNotEmpty() || newCommentImage.isNotEmpty(),
                            onClick = {
                                detailViewModel.postCommentForList(
                                    observationId = observation.id?:"",
                                    authorId = authState.currentUser?.uid?:"",
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
                            Icon(
                                Icons.AutoMirrored.Filled.Send, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
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
                                onClick = {}
                            )
                            IconButton(
                                onClick = {
                                    detailViewModel.updateNewCommentSpecies(
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
                }}
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                if (comments.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No comments yet.\nBe the first one to comment!",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(items = comments, key = { it.id?:"" }) { comment ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(horizontal = 15.dp)
                                .fillMaxWidth()
                                .combinedClickable
                                    (onLongClick = {
                                    if (authState.currentUser != null)
                                        if ((authState.currentUser?.uid ?: "") == (observation?.uid
                                                ?: "") || authState.currentUser?.uid == comment.userId
                                        ) selectedComment = comment.id ?: ""
                                }) {}
                        ) {
                            Row {
                                GlideImage(
                                    model = comment.userImage ?: "",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .size(40.dp),
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
                                                comment.userName ?: "Unknown User",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (comment.content.isNotEmpty()) {
                                            Row() {
                                                Text(
                                                    comment.content,
                                                )
                                            }
                                        }
                                    }

                                    if (comment.speciesId != "") {
                                        Text(
                                            "is suggested this species",
                                            color = MaterialTheme.colorScheme.outline,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (commentSpecies == null) {
                                            CircularProgressIndicator()
                                        } else {
                                            SpeciesListItem(
                                                observationState = commentSpecies[comment.id]?.haveObservation
                                                    ?: false,
                                                species = commentSpecies[comment.id]
                                                    ?: DisplayableSpecies(),
                                                onClick = {}
                                            )
                                        }
                                    }
                                    if (comment.imageUrl.isNotEmpty()) {
                                        Row() {
                                            val mimeType =
                                                if (comment.imageUrl.contains("/video/upload/")
                                                ) "video" else "image"
                                            Box {
                                                GlideImage(
                                                    model = comment.imageUrl,
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
                                                                    comment.imageUrl
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
                                                comment.timestamp?.toDate() ?: 0
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        IconButton(
                                            onClick = {
                                                detailViewModel.likeComment(comment.id?:"", authState.currentUser?.uid?:"")

                                            },
                                            enabled = authState.currentUser != null
                                        ) {

                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = if ((authState.currentUser != null && comment.likeUserIds.contains(
                                                        authState.currentUser?.uid?:""
                                                    )
                                                            )
                                                ) Color(9, 184, 73)
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }

                                        Text(
                                            comment.likeCount.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                detailViewModel.dislikeComment(comment.id?:"", authState.currentUser?.uid?:"")
                                                Log.i("clicked", "dislike comment")
                                            },
                                            enabled = authState.currentUser != null
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = if (authState.currentUser != null && comment.dislikeUserIds.contains(
                                                        authState.currentUser?.uid?:""
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
                }
            }
        }
    }

}
