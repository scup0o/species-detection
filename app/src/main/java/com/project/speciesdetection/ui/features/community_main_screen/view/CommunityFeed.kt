package com.project.speciesdetection.ui.features.community_main_screen.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.ui.composable.common.NotificationPermissionEffect
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.CommunityFeedViewModel
import com.project.speciesdetection.ui.features.auth.view.AuthScreen
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.ObservationListViewModel


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CommunityFeed(
    navController: NavHostController,
    viewModel: CommunityFeedViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
    observationList: ObservationListViewModel = hiltViewModel(),
) {
    //val open by viewModel.searchQuery.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val observations = observationList.hotObservations.collectAsLazyPagingItems()

    //var selectedObservationForComments by remember { mutableStateOf<Observation?>(null) }

    var init by rememberSaveable { mutableStateOf(true) }

    /*selectedObservationForComments?.let { observation ->
        ObservationCommentsSheet(
            observation = observation,
            navController = navController,
            onDismissRequest = { selectedObservationForComments = null }
        )
    }*/


    if (authState.currentUserInformation == null) {
        AuthScreen(navController, authViewModel)
    } else {
        /*val context = LocalContext.current
        val fcm = FirebaseMessaging.getInstance()*/
        val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()

        if (init) {
            authViewModel.reloadCurrentUser(authState.currentUser!!)
            NotificationPermissionEffect(
                key = authState.currentUserInformation!!.uid,
                onPermissionGranted = {
                    authViewModel.updateFcmToken()
                },
                onPermissionDenied = {
                }
            )

            LaunchedEffect(key1 = true) {
                viewModel.checkUserNotificationState(authState.currentUser?.uid ?: "")
            }

            init = false
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    stickyHeader {
                        Surface(
                            shadowElevation = 5.dp,
                            shape =
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    bottomStart = 20.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 20.dp
                                ),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp)
                                    .padding(bottom = 15.dp)
                            ) {
                                /*Text(
                                    "Explore",
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )*/
                                Row(
                                    modifier =
                                        Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainer,
                                                CircleShape
                                            )
                                            .align(Alignment.CenterStart),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(end = 10.dp)
                                            .clickable {
                                                navController.navigate(
                                                    AppScreen.UpdateObservationScreen
                                                        .buildRouteForCreate(
                                                            null, null
                                                        )
                                                )
                                            }

                                    ) {
                                        GlideImage(
                                            model = authState.currentUserInformation?.photoUrl,
                                            contentDescription = null,
                                            loading = placeholder(R.drawable.error_image),
                                            failure = placeholder(R.drawable.error_image),
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .size(40.dp)
                                        )
                                        Text(
                                            stringResource(R.string.community_add_post),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(10.dp)
                                        )
                                        Icon(
                                            Icons.Filled.Add, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .clickable {
                                            navController.navigate(AppScreen.NotificationScreen.route)
                                        }
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            RoundedCornerShape(100)
                                        ),
                                ) {
                                    BadgedBox(
                                        modifier = Modifier.padding(10.dp),
                                        badge = {
                                            if (notificationState) {
                                                Badge()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }


                            }
                        }

                    }

                    items(
                        count = observations.itemCount,
                        key = observations.itemKey {
                            it.id ?: it.hashCode()
                        }
                    ) { index ->
                        val observation = observations[index]
                        if (observation != null) {
                            ObservationListItem(
                                initialObservation = observation,
                                navController = navController,
                                authViewModel = authViewModel,
                                onCommentClicked = {
                                    navController.navigate(
                                        AppScreen.ObservationDetailScreen.createRoute(
                                            it.id ?: ""
                                        )
                                    )
                                },
                                onMenuClicked = {
                                },
                                onUserClicked = { userId ->
                                    navController.navigate(
                                        AppScreen.CommunityProfileMainScreen.createRoute(
                                            userId
                                        )
                                    )
                                },
                                onObservationClick = {
                                    navController.navigate(
                                        AppScreen.ObservationDetailScreen.createRoute(
                                            it.id ?: ""
                                        )
                                    )
                                }
                            )
                            HorizontalDivider(
                                thickness = 8.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    observations.loadState.apply {
                        when {
                            // Trạng thái làm mới (lần tải đầu)
                            refresh is LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            // Trạng thái tải thêm (cuộn xuống cuối danh sách)
                            append is LoadState.Loading -> {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            // Lỗi khi làm mới
                            refresh is LoadState.Error -> {
                                val e = observations.loadState.refresh as LoadState.Error
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "Loi",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                            text = e.error.localizedMessage ?: "Loi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Button(
                                            onClick = { observations.retry() },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text("retry")
                                        }
                                    }
                                }
                            }
                            // Lỗi khi tải thêm
                            append is LoadState.Error -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("no more")
                                        Button(onClick = { observations.retry() }) {
                                            Text("retry")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
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
}