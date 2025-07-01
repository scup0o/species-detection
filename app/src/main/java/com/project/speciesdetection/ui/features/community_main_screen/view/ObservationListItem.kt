package com.project.speciesdetection.ui.features.community_main_screen.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.ui.composable.common.ExpandableText
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.ObservationDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun ObservationListItem(
    initialObservation: Observation,
    navController: NavController,
    authViewModel: AuthViewModel,
    onCommentClicked: (Observation) -> Unit,
    onMenuClicked: (Observation) -> Unit,
    onUserClicked: (String) -> Unit,
    onObservationClick: (Observation) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val detailViewModel: ObservationDetailViewModel =
        hiltViewModel(key = "obs_${initialObservation.id}")

    LaunchedEffect(key1 = initialObservation.id) {
        detailViewModel.startListeningToObservation(initialObservation.id ?: "")
    }

    val observationState by detailViewModel.observationState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentLang by detailViewModel.currentLanguage.collectAsStateWithLifecycle()

    val observation = observationState ?: initialObservation

    var menuExpanded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { observation.imageURL.size })

    if (observation.state == "normal" && observation.privacy == "Public") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 12.dp)
                .clickable {
                    onObservationClick(observation)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(

                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlideImage(
                        model = observation.userImage,
                        contentDescription = "${observation.userName}'s avatar",
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(40.dp)
                            .clickable { onUserClicked(observation.uid) },
                        loading = placeholder(R.drawable.error_image),
                        failure = placeholder(R.drawable.error_image)
                    )
                    Column {
                        Text(
                            observation?.userName ?: "Unknown User",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onUserClicked(observation.uid) },
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
                /*if (authState.currentUser?.uid == observation.uid) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Observation options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    menuExpanded = false
                                    // navController.navigate(AppScreen.UpdateObservationScreen.buildRouteForEdit(observation))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    onMenuClicked(observation)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }*/
            }

            if (observation.content.isNotBlank()) {
                Row(modifier = Modifier.padding(horizontal = 15.dp)) {
                    ExpandableText(text = observation.content)
                }
            }

            if (observation.imageURL.isNotEmpty()) {
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
                                                    observation?.imageURL?.get(page) ?: ""
                                                )
                                            ) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                if (observation?.imageURL?.get(page)?.contains("/video/") == true) {

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
            if (observation?.location != null)
                LazyRow(modifier = Modifier.padding(horizontal = 15.dp)) {
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
                Row {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentUser = authState.currentUser
                // Like / Point / Dislike
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hasLiked = currentUser?.uid in observation.likeUserIds
                    val hasDisliked = currentUser?.uid in observation.dislikeUserIds

                    IconButton(
                        onClick = {
                            if (currentUser != null) detailViewModel.likeObservation(
                                observation.id ?: "",
                                currentUser.uid
                            )
                        },
                        enabled = currentUser != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp, "Like",
                            tint = if (hasLiked) Color(
                                9,
                                184,
                                73
                            ) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        observation.point.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    IconButton(
                        onClick = {
                            if (currentUser != null) detailViewModel.dislikeObservation(
                                observation.id ?: "",
                                currentUser.uid
                            )
                        },
                        enabled = currentUser != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown, "Dislike",
                            tint = if (hasDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Comments Button
                TextButton(onClick = { onCommentClicked(observation) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.message_circle),
                            "Comments",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            observation.commentCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Save Button
                val isSaved = observation.savedBy.contains(currentUser?.uid)
                TextButton(
                    onClick = {
                        if (currentUser != null) detailViewModel.saveObservation(
                            observation.id ?: "", currentUser.uid
                        )
                    },
                    enabled = currentUser != null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.obs_is_hided),
                color = MaterialTheme.colorScheme.outline,
                fontStyle = FontStyle.Italic
            )
        }
    }
}