package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel
import com.project.speciesdetection.ui.composable.common.AppSearchBar
import com.project.speciesdetection.ui.composable.common.ChipPlacholder
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaMainScreen(
    containerColor: Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    viewModel: EncyclopediaMainScreenViewModel = hiltViewModel(),
) {
    val speciesClassList by viewModel.speciesClassList.collectAsStateWithLifecycle()
    val lazyPagingItems: LazyPagingItems<DisplayableSpecies> =
        viewModel.speciesPagingDataFlow.collectAsLazyPagingItems()
    val selectedClassId by viewModel.selectedClassId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle() // Lấy searchQuery

    //config UI
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    //ui management state
    val loadState = lazyPagingItems.loadState
    var showEmptyState by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = searchQuery, key2 = loadState.refresh) {
        if (loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0) {
            delay(500) // Delay 500ms trước khi cho phép hiển thị empty screen
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        text = stringResource(R.string.encyclopedia_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = containerColor!!,
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            AppSearchBar(
                query = searchQuery,
                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onSearchAction = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    // Bạn có thể thêm logic khác ở đây nếu cần khi search được thực hiện
                },
                onClearQuery = {
                    viewModel.onSearchQueryChanged("")
                    keyboardController?.hide() // Tùy chọn: ẩn bàn phím khi xóa
                    focusManager.clearFocus() // Tùy chọn: xóa focus khi xóa
                },
                modifier = Modifier
                    .padding(
                        start = MaterialTheme.spacing.m,
                        end = MaterialTheme.spacing.m,
                        top = MaterialTheme.spacing.xs,
                        bottom = MaterialTheme.spacing.xs
                    ),
                hint = stringResource(R.string.species_search_hint)
            )

            // Thanh chọn Species Class
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MaterialTheme.spacing.xs),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.m),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                if (speciesClassList.isNotEmpty() || selectedClassId == "0") { // Hiển thị "Tất cả" ngay cả khi class list chưa load xong
                    item {
                        SpeciesClassChip(
                            speciesClass = DisplayableSpeciesClass("0", stringResource(R.string.all)),
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
                } else if (selectedClassId != "0") { // Chỉ hiển thị placeholder nếu không phải "Tất cả" và list rỗng
                    item { ChipPlacholder() }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                loadState.refresh is LoadState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = MaterialTheme.spacing.m),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ErrorScreenPlaceholder(onClick = { lazyPagingItems.retry() })
                    }
                }

                loadState.refresh is LoadState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = MaterialTheme.spacing.m,
                            vertical = MaterialTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        items(3) { ListItemPlaceholder() }
                    }
                }

                // 3. HIỂN THỊ MÀN HÌNH RỖNG KHI TẢI XONG, KHÔNG LỖI, VÀ KHÔNG CÓ ITEM
                loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                    if (showEmptyState){
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = MaterialTheme.spacing.m),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "no item", // String resource
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                                )
                                Button(onClick = {lazyPagingItems.retry()}){
                                    Text("retry")
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = MaterialTheme.spacing.m),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    ErrorScreenPlaceholder(onClick = { lazyPagingItems.retry() })
                                }
                            }
                        }
                    }

                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding =
                            PaddingValues(
                                horizontal = MaterialTheme.spacing.m,
                                vertical = MaterialTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id }
                        ) { index ->
                            val species = lazyPagingItems[index]
                            species?.let {
                                SpeciesListItem(species = it)
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
                                            ItemErrorPlaceholder(onClick = {lazyPagingItems.retry()})
                                        }
                                    }
                                    is LoadState.NotLoading -> {
                                        if (appendState.endOfPaginationReached && lazyPagingItems.itemCount > 0) {
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
                            }}
                    }
                }
            }
        }
    }
}



