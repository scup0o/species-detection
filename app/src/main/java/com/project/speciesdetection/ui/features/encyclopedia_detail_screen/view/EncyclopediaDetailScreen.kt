package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view

import MultiSystemConservationStatusView
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.helpers.CloudinaryImageURLHelper
import com.project.speciesdetection.core.helpers.MediaHelper
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.HyperlinkText
import com.project.speciesdetection.ui.composable.common.species.IUCNConservationStatusView
import com.project.speciesdetection.ui.composable.common.species.SpeciesClassification
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.encyclopedia_detail_screen.viewmodel.EncyclopediaDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class,
)
@Composable
fun EncyclopediaDetailScreen(
    //species: DisplayableSpecies,
    observationImage: Uri? = null,
    navController: NavController,
    authViewModel: AuthViewModel,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    speciesDetailViewModel: EncyclopediaDetailViewModel = hiltViewModel()
) {
    val uiState by speciesDetailViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val sourceList by speciesDetailViewModel.sourceList.collectAsStateWithLifecycle()
    val observationState by speciesDetailViewModel.speciesDateFound.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val listCurrentState by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val chipListState: LazyListState = rememberLazyListState()
    val indexList = listOf(
        stringResource(R.string.species_detail_summary) to 0,
        stringResource(R.string.species_detail_classification) to 1,
        stringResource(R.string.species_detail_physical) to 2,
        stringResource(R.string.species_detail_distribution) to 3,
        stringResource(R.string.species_detail_habitat) to 4,
        stringResource(R.string.species_detail_behavior) to 5,
        stringResource(R.string.species_detail_source) + " & " + stringResource(R.string.species_detail_more_info) to 6,
    )

    //val detailedDescriptionContainer: Color = MaterialTheme.colorScheme.surfaceContainer

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser == null) {
            speciesDetailViewModel.clearObservationState()
        } else {
            if (uiState is EncyclopediaDetailViewModel.UiState.Success)
                speciesDetailViewModel.observeDateFoundForUidAndSpecies(
                    (uiState as EncyclopediaDetailViewModel.UiState.Success).species.id,
                    authState.currentUser?.uid ?: ""
                )
        }
    }


    /*LaunchedEffect(listCurrentState) {
        Log.i("check chekc", listCurrentState.toString())
        if (listCurrentState == 3 || listCurrentState == 6 || listCurrentState==0)
            coroutineScope.launch {
                chipListState.animateScrollToItem(index = listCurrentState)
            }

    }*/

    when (uiState) {
        EncyclopediaDetailViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor)
            ) {
                Row(modifier = Modifier.align(Alignment.Center)) {
                    CircularProgressIndicator()
                }
            }

        }

        is EncyclopediaDetailViewModel.UiState.Success -> {
            val species = (uiState as EncyclopediaDetailViewModel.UiState.Success).species
            val pagerState = rememberPagerState(pageCount = { species.imageURL.size })

            Scaffold(
                containerColor = containerColor,
            ) { innerPadding ->
                Box {
                    LazyColumn(
                        modifier = Modifier.padding(innerPadding),
                        state = listState
                    ) {
                        item {
                            Box(Modifier.fillMaxWidth()) {
                                HorizontalPager(pagerState) { page ->
                                    // Lấy URL gốc cho trang hiện tại
                                    val originalUrl = species.imageURL[page]

                                    // Kiểm tra xem URL có phải là của video hay không
                                    val isVideo = MediaHelper.isVideoFile(originalUrl)

                                    // Xác định URL sẽ được hiển thị:
                                    // - Nếu là video, lấy URL thumbnail .jpg
                                    // - Nếu là ảnh, dùng URL gốc
                                    val displayUrl = if (isVideo) {
                                        CloudinaryImageURLHelper.getVideoThumbnailUrl(originalUrl)
                                    } else {
                                        originalUrl
                                    }

                                    // Sử dụng Box để có thể đặt icon Play lên trên ảnh
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .clip(RoundedCornerShape(0, 0, 60, 0))
                                            .clickable {
                                                // QUAN TRỌNG: Luôn sử dụng URL gốc khi điều hướng
                                                // để trình xem toàn màn hình nhận được file video .mp4, không phải file ảnh .jpg
                                                navController.navigate(
                                                    AppScreen.FullScreenImageViewer.createRoute(displayUrl)
                                                )
                                            },
                                        // Căn giữa mọi thứ bên trong Box theo mặc định
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // GlideImage luôn được hiển thị, làm nền
                                        GlideImage(
                                            model = displayUrl, null,
                                            failure = placeholder(R.drawable.image_not_available),
                                            loading = placeholder(R.drawable.image_not_available),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize() // Điền đầy Box cha
                                        )

                                        // Nếu là video, hiển thị icon Play lên trên
                                        if (isVideo) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play Video", // Dành cho hỗ trợ tiếp cận
                                                modifier = Modifier
                                                    .size(64.dp), // Kích thước của icon
                                                tint = Color.White.copy(alpha = 0.8f) // Màu trắng với độ trong suốt nhẹ
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.padding(5.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Back",
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        navController.navigate(
                                            AppScreen.SpeciesObservationMainScreen.createRoute(
                                                species
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .align(Alignment.BottomEnd)
                                        .size(48.dp),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Icon(
                                        painterResource(R.drawable.binoculars), null,
                                        modifier = Modifier.size(24.dp)
                                    )
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


                                /*
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val previousPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                            pagerState.animateScrollToPage(previousPage)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous Image")
                                }

                                // Next Button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val nextPage = (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount-1)
                                            pagerState.animateScrollToPage(nextPage)
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Image")
                                }*/
                            }

                            Column(modifier = Modifier.padding(MaterialTheme.spacing.m)) {
                                if (observationState != null)
                                    Text(
                                        stringResource(R.string.obs_detail_state) + " " +
                                                SimpleDateFormat(
                                                    "HH:mm, dd/MM/yyyy",
                                                    Locale.getDefault()
                                                ).format(
                                                    observationState?.toDate() ?: 0
                                                ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        color = MaterialTheme.colorScheme.tertiary.copy(0.8f),
                                        fontStyle = FontStyle.Italic,
                                        textAlign = TextAlign.End
                                    )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        species.localizedName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        ", " + stringResource(R.string.species_family_description) + " ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        species.localizedFamily,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        stringResource(R.string.species_detail_scientific_name) + ": ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    Text(
                                        species.getScientificName()!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }


                                Text(
                                    species.localizedSummary.firstOrNull()
                                        ?: stringResource(R.string.iucn_status_unknown),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 10.dp)

                                )
                                Row {
                                    sourceList.forEach { source ->
                                        if (source.listIndex == 0) {
                                            Text(
                                                "[${source.orderAdded}]",
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.clickable {
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(
                                                            index = indexList.size - 1,
                                                            scrollOffset = 1000
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    stringResource(R.string.species_detail_conservation_status),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        top = MaterialTheme.spacing.m,
                                        bottom = MaterialTheme.spacing.xs
                                    )
                                )

                               /* Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(15)
                                        ).fillMaxWidth()
                                ) {
                                    IUCNConservationStatusView(species.conservation)
                                }*/
                                MultiSystemConservationStatusView(
                                    iucnStatusCode = species.conservation,
                                    otherSystems = species.otherConvo
                                )


                            }

                        }



                        stickyHeader {
                            Surface(
                                shadowElevation = if (listCurrentState >= 1) 5.dp else 0.dp,
                                shape =
                                    RoundedCornerShape(
                                        topStart = 0.dp,
                                        bottomStart = 20.dp,
                                        topEnd = 0.dp,
                                        bottomEnd = 20.dp
                                    ),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                LazyRow(
                                    modifier = Modifier
                                        .background(
                                            if (listCurrentState >= 1) MaterialTheme.colorScheme.surfaceContainer.copy(
                                                0.5f
                                            ) else MaterialTheme.colorScheme.surface,
                                        )
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    //contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.m),
                                    state = chipListState,
                                ) {
                                    items(
                                        indexList,
                                        key = { it.second }) { section ->
                                        val isSelected = listCurrentState == section.second
                                        Text(
                                            section.first,
                                            color = if (isSelected) MaterialTheme.colorScheme.onTertiary
                                            else MaterialTheme.colorScheme.tertiary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.tertiary
                                                    else MaterialTheme.colorScheme.surfaceContainer,
                                                    RoundedCornerShape(15.dp)
                                                )
                                                .clickable {
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(
                                                            index = section.second,
                                                            scrollOffset = if (section.second == indexList.size - 1) 1000 else 0
                                                        )
                                                    }
                                                }
                                                .padding(horizontal = 20.dp, vertical = 8.dp)

                                        )


                                    }

                                }
                            }
                        }

                        items(6) { index ->
                            Box(
                                modifier = Modifier
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = MaterialTheme.spacing.m)
                                ) {
                                    if (index > 0)
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.padding(vertical = 15.dp)
                                        )
                                    Text(
                                        if (indexList[index + 1].first == stringResource(R.string.species_detail_source) + " & " + stringResource(
                                                R.string.species_detail_more_info
                                            )
                                        ) stringResource(R.string.species_detail_source) else indexList[index + 1].first,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            top = MaterialTheme.spacing.m,
                                            bottom = MaterialTheme.spacing.xs
                                        )
                                    )
                                    if (indexList[index + 1].first == stringResource(R.string.species_detail_classification)) {
                                        Box(
                                            modifier = Modifier
                                            /*.background(
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                RoundedCornerShape(15)
                                            )*/
                                        ) { SpeciesClassification(species) }
                                    } else {
                                        if (indexList[index + 1].first == stringResource(R.string.species_detail_source) + " & " + stringResource(
                                                R.string.species_detail_more_info
                                            )
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                sourceList.forEachIndexed { index, source ->
                                                    Row {
                                                        HyperlinkText(
                                                            fullText = "[${index + 1}] " + source.firstValue + " " + source.secondValue,
                                                            linkText = source.secondValue,
                                                            url = source.secondValue
                                                        )
                                                    }

                                                }

                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.padding(vertical = 15.dp)
                                                )

                                                Text(
                                                    stringResource(R.string.species_detail_more_info),
                                                    modifier = Modifier.padding(top = MaterialTheme.spacing.m),
                                                    fontWeight = FontWeight.Bold,
                                                )

                                                species.info.forEach { info ->
                                                            HyperlinkText(
                                                                fullText = info.key+": " + info.value,
                                                                linkText = info.value,
                                                                url = info.value,
                                                            )
                                                }
                                            }

                                        } else {
                                            Text(
                                                text = when (indexList[index + 1].first) {
                                                    //"Summary" -> species.localizedSummary.firstOrNull() ?: ""
                                                    //"Taxonomy" -> species.localizedPhysical.firstOrNull() ?: ""
                                                    stringResource(R.string.species_detail_physical) -> species.localizedPhysical.firstOrNull()
                                                        ?: ""

                                                    stringResource(R.string.species_detail_distribution) -> species.localizedDistribution.firstOrNull()
                                                        ?: ""

                                                    stringResource(R.string.species_detail_habitat) -> species.localizedHabitat.firstOrNull()
                                                        ?: ""

                                                    stringResource(R.string.species_detail_behavior) -> species.localizedBehavior.firstOrNull()
                                                        ?: ""
                                                    //"Source" -> species.localizedBehavior.firstOrNull() ?: ""
                                                    //"More Info" -> species.localizedBehavior.firstOrNull() ?: ""
                                                    else -> "Đang cập nhật..."
                                                },
                                                style = MaterialTheme.typography.bodyMedium
                                            )


                                            Row {
                                                sourceList.forEach { source ->
                                                    if (source.listIndex == indexList[index + 1].second) {
                                                        Text(
                                                            "[${source.orderAdded}]",
                                                            color = MaterialTheme.colorScheme.tertiary,
                                                            modifier = Modifier.clickable {
                                                                coroutineScope.launch {
                                                                    listState.scrollToItem(
                                                                        index = indexList.size - 1,
                                                                        scrollOffset = 1000
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
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
                            .padding(vertical = 20.dp, horizontal = 10.dp)
                    ) {
                        if (authState.currentUser != null) {
                            CustomActionButton(
                                onClick = {
                                    navController.navigate(
                                        AppScreen.UpdateObservationScreen.buildRouteForCreate(
                                            species,
                                            observationImage
                                        )
                                    )
                                },
                                text =
                                    if (observationImage != null) stringResource(R.string.species_detail_record)
                                    else stringResource(R.string.species_detail_add),
                                vectorLeadingIcon = Icons.Default.Add
                            )

                        } else {

                            CustomActionButton(
                                onClick = {
                                    /*navController.popBackStack(
                                        AppScreen.LoginScreen.route,
                                        inclusive = true,
                                        saveState = false
                                    )*/
                                    navController.navigate(AppScreen.LoginScreen.route) {
                                        launchSingleTop = true
                                    }
                                },
                                text =
                                    if (observationImage != null) stringResource(R.string.species_detail_login_to_record)
                                    else stringResource(R.string.species_detail_login_to_add),

                                )

                        }
                    }
                }

            }
        }


        is EncyclopediaDetailViewModel.UiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerColor)
                        .padding(innerPadding)
                ) {
                    Row(modifier = Modifier.align(Alignment.Center)) {
                        ErrorScreenPlaceholder(
                            onClick = {
                                speciesDetailViewModel.getSpeciesDetailed()
                            }
                        )
                    }
                }
            }

        }
    }
}