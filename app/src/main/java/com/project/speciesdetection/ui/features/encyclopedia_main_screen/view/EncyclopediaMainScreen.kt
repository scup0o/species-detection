package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.ui.composable.common.AppSearchBar
import com.project.speciesdetection.ui.composable.common.ChipPlacholder
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel
import com.project.speciesdetection.ui.features.setting_main_screen.viewmodel.SettingViewModel

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaMainScreen(
    containerColor: Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    viewModel: EncyclopediaMainScreenViewModel = hiltViewModel(),
    settingViewModel: SettingViewModel,
    authViewModel: AuthViewModel
) {
    //val scaledHeight = LocalConfiguration.current.screenHeightDp.dp
    //val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val sortByDesc by viewModel.sortByDesc.collectAsStateWithLifecycle()
    val speciesClassList by viewModel.speciesClassList.collectAsStateWithLifecycle()
    val lazyPagingItems: LazyPagingItems<DisplayableSpecies> =
        viewModel.speciesPagingDataFlow.collectAsLazyPagingItems()
    val selectedClassId by viewModel.selectedClassId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle() // Lấy searchQuery
    val speciesObservationState by viewModel.speciesDateFound.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    //config UI
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    //ui management state
    val loadState = lazyPagingItems.loadState
    val languageState by settingViewModel.currentLanguageCode.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguageState.collectAsStateWithLifecycle()
    /*var showEmptyState by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = searchQuery, key2 = loadState.refresh) {
        if (loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0) {
            delay(500) // Delay 500ms trước khi cho phép hiển thị empty screen
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }*/

    LaunchedEffect(languageState) {
        if (languageState != currentLanguage) {
            viewModel.setLanguage(languageState)
            if (currentLanguage != "none") {
                viewModel.loadInitialSpeciesClasses()
                lazyPagingItems.refresh()
            }
        }
    }

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
            authViewModel.reloadCurrentUser(authState.currentUser!!)
        }
    }



    Scaffold(
        bottomBar = { Row() {} },
        containerColor = MaterialTheme.colorScheme.primary.copy(0.5f),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxWidth()) {
            /*GlideImage(
                model = R.drawable.banner_8,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                loading = placeholder(R.drawable.image_not_available),
                failure = placeholder(R.drawable.image_not_available)
            )*/

            /*Image(
                painterResource(R.drawable.banner_9), null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth

            )*/

            Column(
                modifier = Modifier.padding(innerPadding),
            ) {

                Row(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onSearchAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        onClearQuery = {
                            viewModel.onSearchQueryChanged("")
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.fillMaxWidth(0.87f),
                        hint = stringResource(R.string.species_search_hint)
                    )

                    val color = MaterialTheme.colorScheme.primary
                    Icon(
                        if (sortByDesc)
                            painterResource(R.drawable.sort_za)
                        else
                            painterResource(R.drawable.sort_az),
                        null,
                        modifier = Modifier
                            .clickable {
                                viewModel.updateSortDirection()
                            }
                            .size(34.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(5.dp)
                            .graphicsLayer {
                                shadowElevation = 5.dp.toPx()
                                ambientShadowColor = color
                                spotShadowColor = color
                                clip = true
                                shape = RoundedCornerShape(10.dp)
                            },
                        tint = MaterialTheme.colorScheme.onTertiary
                    )

                }
                // Thanh chọn Species Class
                LazyRow(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (speciesClassList.isNotEmpty() || selectedClassId == "0") { // Hiển thị "Tất cả" ngay cả khi class list chưa load xong
                        item {
                            SpeciesClassChip(
                                speciesClass = DisplayableSpeciesClass(
                                    "0",
                                    stringResource(R.string.all)
                                ),
                                transparentColor = containerColor,
                                isSelected = selectedClassId == "0",
                                onClick = { viewModel.selectSpeciesClass("0") })
                        }
                    }

                    if (speciesClassList.isNotEmpty()) {
                        items(speciesClassList, key = { it.id }) { sClass ->
                            SpeciesClassChip(
                                transparentColor = containerColor,
                                speciesClass = sClass,
                                isSelected = sClass.id == selectedClassId,
                                onClick = { viewModel.selectSpeciesClass(sClass.id) }
                            )
                        }
                    } else if (selectedClassId != "0") {
                        item(3) { ChipPlacholder() }
                    }
                }



                Column(
                    modifier = Modifier
                        //.padding(innerPadding)
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(
                                //topStartPercent = 2,
                                topEndPercent = 2,
                                bottomEndPercent = 0,
                                bottomStartPercent = 0
                            )
                        )

                ) {


                    Spacer(modifier = Modifier.height(2.dp))

                    when {
                        loadState.refresh is LoadState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = MaterialTheme.spacing.m),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ErrorScreenPlaceholder(onClick = { lazyPagingItems.refresh() })
                            }
                        }

                        loadState.refresh is LoadState.Loading -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    horizontal = MaterialTheme.spacing.m,
                                    vertical = MaterialTheme.spacing.l
                                ),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                            ) {
                                items(3) { ListItemPlaceholder() }
                            }
                        }

                        // 3. HIỂN THỊ MÀN HÌNH RỖNG KHI TẢI XONG, KHÔNG LỖI, VÀ KHÔNG CÓ ITEM
                        loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = MaterialTheme.spacing.m),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_species), // String resource
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                                )
                                Button(onClick = { lazyPagingItems.refresh() }) {
                                    Text(stringResource(R.string.try_again))
                                }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = MaterialTheme.spacing.m),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        ErrorScreenPlaceholder(onClick = { lazyPagingItems.refresh() })
                                    }
                                }
                            }
                        }

                        else -> {
                            val initState by viewModel.init.collectAsStateWithLifecycle()

                            var lastProcessedSize by rememberSaveable { mutableStateOf(0) }

                            LaunchedEffect(lazyPagingItems.itemSnapshotList.items.size) {
                                val currentUser = authState.currentUser
                                val currentItems = lazyPagingItems.itemSnapshotList.items
                                Log.i("ss", "$lastProcessedSize, $currentItems")

                                if (currentUser != null && currentItems.size > lastProcessedSize) {
                                    val newItems =
                                        currentItems.subList(lastProcessedSize, currentItems.size)
                                    viewModel.getSpeciesListState(newItems, currentUser.uid)
                                    lastProcessedSize = currentItems.size
                                }
                            }

                            LaunchedEffect(authState.currentUser) {
                                if (authState.currentUser == null) viewModel.clearObservationState()
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding =
                                    PaddingValues(
                                        horizontal = MaterialTheme.spacing.m,
                                        vertical = MaterialTheme.spacing.xs
                                    ),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                            ) {
                                item {

                                }
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = lazyPagingItems.itemKey { it.id }
                                ) { index ->
                                    val species = lazyPagingItems[index]



                                    species?.let {
                                        SpeciesListItem(
                                            observationState = if (initState) species.haveObservation else speciesObservationState[species.id] != null,
                                            species = it,
                                            onClick = {

                                                navController.popBackStack(
                                                    AppScreen.EncyclopediaDetailScreen.createRoute(
                                                        species = it,
                                                        imageUri = null
                                                    ),
                                                    inclusive = true,
                                                    saveState = false
                                                )
                                                navController.navigate(
                                                    AppScreen.EncyclopediaDetailScreen.createRoute(
                                                        species = it,
                                                        imageUri = null
                                                    )
                                                ) {
                                                    launchSingleTop = true
                                                }
                                            })

                                    }
                                }

                                // Xử lý trạng thái APPEND (tải thêm khi cuộn xuống)
                                item {
                                    lazyPagingItems.loadState.append.let { appendState ->
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
                                                    ItemErrorPlaceholder(onClick = { lazyPagingItems.refresh() })
                                                }
                                            }

                                            is LoadState.NotLoading -> {
                                                if (appendState.endOfPaginationReached && lazyPagingItems.itemCount > 0) {
                                                    Spacer(modifier = Modifier.height(100.dp - innerPadding.calculateBottomPadding() * 2))
                                                    //Text(
                                                    //    text = "end", // String resource
                                                    //    modifier = Modifier
                                                    //        .fillMaxWidth()
                                                    //        .padding(16.dp),
                                                    //    textAlign = TextAlign.Center,
                                                    //    style = MaterialTheme.typography.bodySmall
                                                    //)
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
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp)
            ) {
                BottomNavigationBar(
                    navController,

                    )
            }
        }

    }
}



