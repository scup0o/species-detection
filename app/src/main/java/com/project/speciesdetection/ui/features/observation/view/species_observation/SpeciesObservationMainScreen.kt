package com.project.speciesdetection.ui.features.observation.view.species_observation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.features.observation.viewmodel.species_observation.SpeciesObservationViewModel

import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesObservationMainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    speciesId: String,
    speciesName: String,
    viewModel: SpeciesObservationViewModel = hiltViewModel()
) {
    val tabs = listOf("Tất cả", "Của tôi")
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTab.collectAsStateWithLifecycle()

    // Thu thập Paging Items một lần duy nhất ở đây
// <-- THAY ĐỔI: Thu thập cả 2 luồng dữ liệu và viewMode ---
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.observationPagingData.collectAsLazyPagingItems()
    val allObservationsForMap by viewModel.allObservationsForMap.collectAsStateWithLifecycle()
    var selectedObservations by remember { mutableStateOf<List<Observation>?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Thu thập Map chứa các thay đổi real-time
    val updatedObservations by viewModel.updatedObservations.collectAsStateWithLifecycle()
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    // Thiết lập các bộ lọc cho ViewModel khi màn hình được tạo hoặc tham số thay đổi
    LaunchedEffect(speciesId, authState.currentUser) {
        viewModel.setFilters(speciesId, authState.currentUser?.uid)
        //viewModel.listenForRealtimeUpdates()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(speciesName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                    }
                }
            )
        },
        /*floatingActionButton = {
            if(selectedTabIndex==1)
                FloatingActionButton(
                    onClick = {
                        navController.navigate(AppScreen.UpdateObservationScreen.buildRouteForCreate(
                            DisplayableSpecies(localizedName = speciesName, id=speciesId),
                            imageUri = null,
                        ))
                    }
                ) {
                    Icon(
                        Icons.Default.Add, null
                    )
                }
        }*/
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            viewModel.selectTab(index)
                            //lazyPagingItems.refresh()
                                  },
                        text = { Text(title, style = MaterialTheme.typography.bodyLarge) },
                        enabled = !(index == 1 && authState.currentUser == null)
                    )
                }
            }

            // Giao diện không cần khối `when` nữa.
            // Nó chỉ cần hiển thị ObservationList với dữ liệu đã được cung cấp.
            val shouldShowList = !isRefreshing || lazyPagingItems.itemCount > 0

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxSize()
            ) {


                when (viewMode) {
                    SpeciesObservationViewModel.ViewMode.LIST -> {
                        ObservationList(
                            lazyPagingItems = lazyPagingItems,
                            updatedObservations = updatedObservations,
                            navController = navController,
                            isRefreshing = isRefreshing
                        )
                    }

                    SpeciesObservationViewModel.ViewMode.MAP -> {
                        // Chỉ hiển thị bản đồ khi tab là "Tất cả"
                        if (selectedTabIndex == 0) {
                            // Giả sử MapView có thể xử lý một danh sách rỗng hoặc loading
                            if (allObservationsForMap.isEmpty()) {
                                CircularProgressIndicator()
                            } else {
                                SpeciesObservationMapView(
                                    observationList = allObservationsForMap,
                                    onMarkerClick = { clickedGeoPoint ->

                                        // --- LOGIC KHI MARKER ĐƯỢC NHẤN ---
                                        // Lọc danh sách observation có cùng GeoPoint
                                        selectedObservations = allObservationsForMap.filter {
                                            Log.i("geopoint", "$clickedGeoPoint, ${it.location}")
                                            it.location?.latitude == clickedGeoPoint.latitude && it.location.longitude == clickedGeoPoint.longitude
                                        }
                                        // Kích hoạt hiển thị BottomSheet
                                        showBottomSheet = true
                                    }
                                )
                            }
                        } else {
                            ObservationList(
                                lazyPagingItems = lazyPagingItems,
                                updatedObservations = updatedObservations,
                                navController = navController,
                                isRefreshing = isRefreshing
                            )
                        }
                    }
                }

                if (selectedTabIndex == 0)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .padding(
                                top = if (viewMode == SpeciesObservationViewModel.ViewMode.MAP) 70.dp else 0.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd
                                )
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(0.3f),
                                    RoundedCornerShape(30)
                                )
                        ) {

                            IconButton(onClick = { viewModel.setViewMode(SpeciesObservationViewModel.ViewMode.LIST) }) {
                                Icon(
                                    imageVector = Icons.Default.List, // Ví dụ
                                    contentDescription = "List View",
                                    tint = if (viewMode == SpeciesObservationViewModel.ViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.setViewMode(SpeciesObservationViewModel.ViewMode.MAP) }) {
                                Icon(
                                    imageVector = Icons.Default.Place, // Ví dụ
                                    contentDescription = "Map View",
                                    tint = if (viewMode == SpeciesObservationViewModel.ViewMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                )
                            }


                        }
                    }


            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // Khi người dùng vuốt xuống để đóng hoặc nhấn ra ngoài
                showBottomSheet = false
                selectedObservations = null // Reset danh sách đã chọn
            },
            sheetState = sheetState
        ) {
            // Nội dung bên trong BottomSheet
            // Kiểm tra null để đảm bảo an toàn
            selectedObservations?.let { observationsAtPoint ->
                LazyColumn {
                    items(
                        observationsAtPoint.size,
                        key = { index -> observationsAtPoint[index].id ?: "" }
                    ) { index ->
                        val newObservation = selectedObservations!![index]
                        ObservationItem(
                            observation = newObservation,
                            onclick = {
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ObservationList(
    isRefreshing: Boolean,
    lazyPagingItems: LazyPagingItems<Observation>,
    updatedObservations: Map<String, Observation?>,
    navController: NavController
) {
    // Lấy các item được THÊM MỚI (chưa có trong Paging) để hiển thị ở trên cùng
    val addedItems = remember(updatedObservations, lazyPagingItems.itemSnapshotList) { // <-- THAY ĐỔI: Dùng itemSnapshotList
        // Lấy danh sách ID đã được tải bởi Paging một cách ổn định hơn
        val pagedItemIds = lazyPagingItems.itemSnapshotList.items
            .mapNotNull { it.id }
            .toSet()

        if (pagedItemIds.isEmpty() && lazyPagingItems.loadState.refresh is LoadState.Loading) {
            // Nếu Paging đang tải lại từ đầu và chưa có item nào,
            // thì không nên có item "thêm mới" nào cả.
            emptyList()
        } else {
            updatedObservations.values
                .filterNotNull()
                .filter { obs ->
                    obs.id !in pagedItemIds
                }
                .sortedByDescending { it.dateCreated } // <-- NÊN CÓ: Sắp xếp các item mới theo thời gian tạo
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
    ) {
        if (!isRefreshing) {
            // --- Hiển thị các item mới được thêm vào ---
            items(
                count = addedItems.size,
                key = { index -> "added_${addedItems[index].id}" } // Key phải là duy nhất
            ) { index ->
                val newObservation = addedItems[index]
                ObservationItem(
                    observation = newObservation,
                    onclick = {
                        navController.navigate(
                            AppScreen.UpdateObservationScreen.buildRouteForEdit(newObservation)
                        )
                    }
                )
            }

            // --- Hiển thị các item từ Paging (với các cập nhật đã được áp dụng) ---
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id!! }
            ) { index ->
                val pagedObservation = lazyPagingItems[index]
                if (pagedObservation != null) {
                    // Kiểm tra xem có bản cập nhật cho item này không
                    // .getOrDefault sẽ trả về giá trị trong map, hoặc giá trị mặc định (pagedObservation) nếu không có
                    val finalObservation =
                        updatedObservations.getOrDefault(pagedObservation.id!!, pagedObservation)

                    // Nếu finalObservation là null, nghĩa là nó đã bị xóa, không hiển thị gì cả
                    if (finalObservation != null) {
                        ObservationItem(
                            observation = finalObservation,
                            onclick = {
                                navController.navigate(
                                    AppScreen.UpdateObservationScreen.buildRouteForEdit(
                                        finalObservation
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
        // --- Xử lý trạng thái loading/error của Paging ---
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
                    item(1) {
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

// ObservationItem không thay đổi
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ObservationItem(observation: Observation, onclick: () -> Unit, showLocation: Boolean = true) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            /*.shadow(
                elevation = MaterialTheme.spacing.m,
                shape = RoundedCornerShape(percent = 10),
                spotColor = Color.Transparent,
                ambientColor = MaterialTheme.colorScheme.surface)*/
            .clickable(
                onClick = onclick
            ),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            if (observation.imageURL.isNotEmpty()) {
                Box {
                    GlideImage(
                        model = observation.imageURL.firstOrNull(),
                        contentDescription = null,
                        loading = placeholder(R.drawable.error_image),
                        failure = placeholder(R.drawable.error_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(MaterialTheme.spacing.xxxs)
                            .clip(MaterialTheme.shapes.small)
                    )
                    if (observation.imageURL.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(0.8f),
                                    RoundedCornerShape(20)
                                )
                        ) {
                            Text(
                                "+ ${observation.imageURL.size - 1}",
                                modifier = Modifier.padding(5.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start=10.dp)
                )
            {
                Text(
                    observation.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically){
                    Icon(
                        Icons.Default.LocationOn,null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(24.dp).padding(end=5.dp)
                    )
                    Text(
                        observation.locationTempName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ){
                    Row(verticalAlignment = Alignment.CenterVertically){
                        Icon(
                            Icons.Default.Favorite,null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(24.dp).padding(end=5.dp)
                        )
                        Text(
                            observation.point.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically){
                        Icon(
                            painterResource(R.drawable.message_circle), null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(24.dp).padding(end=5.dp)
                        )
                        Text(
                            observation.commentCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                }
            }


        }
    }
}
