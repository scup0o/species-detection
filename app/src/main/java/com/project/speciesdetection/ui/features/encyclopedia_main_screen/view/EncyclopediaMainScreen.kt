package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import android.util.Log
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel
import com.project.speciesdetection.ui.widgets.common.encyclopedia.SpeciesClassChip

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

    //config UI
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

            // Thanh chọn Species Class
            if (speciesClassList.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //item{
                    //    SpeciesClassChip(
                    //        speciesClass = DisplayableSpeciesClass("0", stringResource(R.string.all),""),
                    //        transparentColor = containerColor,
                    //        isSelected = selectedClassId == "0",
                    //        onClick = { viewModel.selectSpeciesClass("0")})
                    //}

                    items(speciesClassList, key = { it.id }) { sClass ->
                        SpeciesClassChip(
                            transparentColor = containerColor,
                            speciesClass = sClass,
                            isSelected = sClass.id == selectedClassId,
                            onClick = { viewModel.selectSpeciesClass(sClass.id) }
                        )
                    }
                }
            } else {
                // Có thể hiển thị loading indicator cho species class ở đây
                Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center){
                    Text("Loading classes...")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách Species sử dụng Paging
            LazyColumn(
                modifier = Modifier.weight(1f), // Để LazyColumn chiếm không gian còn lại
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sử dụng itemKey để Paging 3 theo dõi item hiệu quả
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id } // Sử dụng ID của DisplayableSpecies
                ) { index ->
                    Log.d("a",lazyPagingItems[index].toString())
                    val species = lazyPagingItems[index]
                    species?.let { // Vẫn cần check null phòng trường hợp Paging 3 có thay đổi
                        SpeciesListItem(species = it)
                    }
                }

                // Xử lý các trạng thái của Paging
                lazyPagingItems.loadState.apply {
                    when {
                        refresh is LoadState.Loading -> {
                            item {
                                SpeciesListItemPlaceholder()
                            }
                        }
                        refresh is LoadState.Error -> {
                            val e = refresh as LoadState.Error
                            item {
                                Column(
                                    modifier = Modifier.fillParentMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("Error: ${e.error.localizedMessage}", color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { lazyPagingItems.retry() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        append is LoadState.Loading -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    SpeciesListItemPlaceholder()
                                    //CircularProgressIndicator(strokeWidth = 2.dp)
                                }
                            }
                        }
                        append is LoadState.Error -> {
                            val e = append as LoadState.Error
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Error loading more: ${e.error.localizedMessage}", color = MaterialTheme.colorScheme.error)
                                    Button(onClick = { lazyPagingItems.retry() }) {
                                        Text("Retry Load More")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Kiểm tra nếu danh sách rỗng SAU KHI tải xong và KHÔNG có lỗi refresh
            if (lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
                lazyPagingItems.itemCount == 0 &&
                (lazyPagingItems.loadState.append.endOfPaginationReached || selectedClassId == null) // Nếu không có classId thì cũng là rỗng
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedClassId == null && speciesClassList.isEmpty()) "Loading species classes..."
                    else if (selectedClassId == null && speciesClassList.isNotEmpty()) "Please select a class."
                    else "No species found in this class.")
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SpeciesListItem(species: DisplayableSpecies) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = species.imageURL,
                contentDescription = species.localizedName,
                loading = placeholder(R.drawable.error_image), // Thay bằng placeholder của bạn
                failure = placeholder(R.drawable.error_image), // Thay bằng error image của bạn
                contentScale = ContentScale.Crop,
                modifier = Modifier.size((80.dp))
            )
            Spacer(modifier = Modifier.width((12.dp)))
            Column {
                Text(
                    text = species.localizedName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = species.scientificName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
                // Thêm các thông tin khác nếu muốn
            }
        }
    }
}

@Composable
fun SpeciesListItemPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Shimmer thường không có đổ bóng
    ) {
        Row(
            modifier = Modifier
                .shimmer() // <<<<< ÁP DỤNG SHIMMER
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.7f)
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.5f)
                        .background(Color.LightGray, shape = MaterialTheme.shapes.small)
                )
            }
        }
    }
}
