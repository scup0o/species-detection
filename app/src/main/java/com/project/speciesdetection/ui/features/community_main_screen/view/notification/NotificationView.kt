package com.project.speciesdetection.ui.features.community_main_screen.view.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.notification.Notification
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.notification.NotificationViewModel
import com.project.speciesdetection.ui.features.community_main_screen.viewmodel.notification.UiEvent
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NotificationView(
    navController: NavController,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val notificationLazyListItem: LazyPagingItems<Notification> =
        notificationViewModel.notificationPaging.collectAsLazyPagingItems()
    val loadState = notificationLazyListItem.loadState
    var loadingState by remember { mutableStateOf(false) }

    val notificationSortState by notificationViewModel.sortByDesc.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = true) {
        if (authState.currentUser != null) {
            notificationViewModel.updateUserId(authState.currentUser?.uid ?: "")
        }
    }
    LaunchedEffect(key1 = true) {
        notificationViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.MoveToPost -> {
                    navController.navigate(AppScreen.ObservationDetailScreen.createRoute(event.observationId))
                }
            }
        }
    }

    if (loadingState) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(containerColor = MaterialTheme.colorScheme.surface) { innerPadding ->
            LazyColumn(
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 15.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stickyHeader {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack, null,
                            modifier = Modifier.clickable {
                                navController.popBackStack()
                            }
                        )
                        Text(
                            stringResource(R.string.notification),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when {
                    loadState.refresh is LoadState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ErrorScreenPlaceholder(onClick = { notificationLazyListItem.refresh() })
                            }
                        }
                    }

                    loadState.refresh is LoadState.Loading -> {
                        items(3) { ListItemPlaceholder() }

                    }

                    loadState.refresh is LoadState.NotLoading && notificationLazyListItem.itemCount == 0 -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_notification),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                                )
                                /*Button(onClick = { notificationLazyListItem.refresh() }) {
                                    Text("retry")
                                }*/

                            }
                        }
                    }

                    else -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .clickable {
                                            notificationViewModel.updateSortDirection()
                                        }
                                        .padding(horizontal = 15.dp)) {
                                    Text(
                                        if (notificationSortState) stringResource(R.string.newest) else stringResource(
                                            R.string.oldest
                                        ),
                                        //style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        if (notificationSortState) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                }
                            }
                        }


                        items(
                            count = notificationLazyListItem.itemCount,
                            key = notificationLazyListItem.itemKey { it.id }
                        ) { index ->
                            val notification = notificationLazyListItem[index]

                            var newBody = ""
                            when (notification?.type) {
                                "lock" -> {
                                    newBody = stringResource(R.string.noti_body_lock)
                                }

                                "violation" -> {
                                    newBody = stringResource(R.string.noti_body_warning)
                                }

                                "like_observation" -> {
                                    newBody = stringResource(R.string.noti_body_like_observation)
                                }

                                "dislike_observation" -> {
                                    newBody = stringResource(R.string.noti_body_dislike_observation)
                                }

                                "comment" -> {
                                    newBody = stringResource(R.string.noti_body_comment)
                                }

                                "like_comment" -> {
                                    newBody = stringResource(R.string.noti_body_like_comment)
                                }

                                "dislike_comment" -> {
                                    newBody = stringResource(R.string.noti_body_dislike_comment)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        if (notification?.type != "violation") {
                                            loadingState = true
                                            notificationViewModel.markNotificationAsRead(
                                                notification ?: Notification(isRead = false),
                                                authState.currentUser?.uid ?: ""
                                            )
                                            notification?.isRead = true
                                        }
                                    }
                                    .background(
                                        if (notification?.isRead == true) {
                                            Color.Transparent
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainer
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)

                            ) {
                                Box(
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    if (notification?.senderImage.isNullOrEmpty() || notification?.type == "violation") {
                                        Icon(
                                            Icons.Default.AccountCircle, null,
                                            modifier = Modifier.fillMaxSize(),
                                            tint = MaterialTheme.colorScheme.outline
                                        )

                                    } else {
                                        GlideImage(
                                            model = notification?.senderImage,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    val color =
                                        if (notification?.type?.contains("dislike") == true) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            if (notification?.type == "violation") {
                                                Color(240, 168, 0)
                                            } else {
                                                if (notification?.type?.contains("like") == true) {
                                                    MaterialTheme.colorScheme.tertiary

                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            }
                                        }
                                    Box(

                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(25.dp)
                                            .background(
                                                color,
                                                CircleShape
                                            )
                                    ) {
                                        val icon =
                                            if (notification?.type?.contains("dislike") == true) {
                                                Icons.Default.KeyboardArrowDown
                                            } else {
                                                if (notification?.type == "violation") {
                                                    Icons.Default.Warning
                                                } else {
                                                    if (notification?.type?.contains("like") == true) {
                                                        Icons.Default.KeyboardArrowUp

                                                    } else {
                                                        Icons.Default.Edit
                                                    }
                                                }

                                            }
                                        Icon(
                                            icon, null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(if (icon == Icons.Default.Edit || icon == Icons.Default.Warning) 15.dp else 20.dp)
                                        )
                                    }

                                }
                                Column {
                                    Text(
                                        text = buildAnnotatedString {
                                            // Phần tên người gửi
                                            if (notification?.type != "violation") {
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append("${notification?.senderUsername ?: "Unknown User"} ")
                                                }
                                            }

                                            if (notification?.count!=null){
                                                if (notification.count>0)
                                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append(stringResource(R.string.like_dislike_count,notification.count ))
                                                    }
                                            }

                                            // Phần thông báo
                                            append(" $newBody ")

                                            // Phần nội dung thông báo, kiểm tra nếu quá dài thì thêm ba chấm
                                            val content = "'${notification?.content}'" ?: ""
                                            if (content.length > 50) {  // Kiểm tra nếu nội dung dài hơn 50 ký tự (có thể thay đổi theo nhu cầu)
                                                append(content.take(50)) // Lấy 50 ký tự đầu tiên
                                                append("...'") // Thêm ba chấm
                                            } else {
                                                append(content) // Nếu không dài, chỉ đơn giản thêm vào
                                            }

                                            if (notification?.type == "comment") {
                                                append(" ${stringResource(R.string.noti_body_to_your_post)}")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = if (notification?.type == "violation") 3 else 2,  // Giới hạn số dòng cho toàn bộ chuỗi
                                        overflow = TextOverflow.Ellipsis // Thêm dấu ba chấm nếu toàn bộ nội dung vượt quá số dòng
                                    )
                                    Text(
                                        SimpleDateFormat(
                                            "HH:mm, dd/MM/yy",
                                            Locale.getDefault()
                                        ).format(
                                            notification?.dateCreated?.toDate() ?: 0
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                        }

                        item {
                            notificationLazyListItem.loadState.append.let { appendState ->
                                when (appendState) {
                                    is LoadState.Loading -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            ListItemPlaceholder()
                                        }
                                    }

                                    is LoadState.Error -> {
                                        val e = appendState
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            ItemErrorPlaceholder(onClick = { notificationLazyListItem.refresh() })
                                        }
                                    }

                                    is LoadState.NotLoading -> {
                                        if (appendState.endOfPaginationReached && notificationLazyListItem.itemCount > 0) {
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
