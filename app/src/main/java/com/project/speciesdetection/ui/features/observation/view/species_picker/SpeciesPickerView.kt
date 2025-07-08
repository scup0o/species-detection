package com.project.speciesdetection.ui.features.observation.view.species_picker

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.AppSearchBar
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.ListItemPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.observation.viewmodel.species_picker.AnalysisUiState
import com.project.speciesdetection.ui.features.observation.viewmodel.species_picker.SpeciesPickerViewModel

@Composable
fun SpeciesPickerView(
    onDismissRequest: () -> Unit,
    onSpeciesSelected: (DisplayableSpecies) -> Unit,
    imageUri: Uri? = null,
    viewModel: SpeciesPickerViewModel = hiltViewModel()
) {
    // Thu thập state từ ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val lazyPagingItems: LazyPagingItems<DisplayableSpecies> =
        viewModel.searchPagingData.collectAsLazyPagingItems()

    // Kích hoạt phân tích hình ảnh một lần khi dialog mở với imageUri
    LaunchedEffect(key1 = imageUri) {
        if (imageUri != null) {
            viewModel.startImageAnalysis(imageUri)
        }
    }

    // Dọn dẹp state của ViewModel khi dialog bị hủy
    DisposableEffect(key1 = Unit) {
        onDispose {
            viewModel.resetAndClear()
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(if (imageUri != null) 0.9f else 0.8f), // Dialog cao hơn nếu có ảnh
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                // --- Header của Dialog ---
                //DialogHeader(onDismissRequest = onDismissRequest)

                // --- Thanh tìm kiếm ---
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 15.dp)){
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onSearchAction = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                        onClearQuery = { viewModel.onSearchQueryChanged("") },
                        modifier = Modifier.fillMaxWidth(),
                        hint = stringResource(R.string.species_search_hint)
                    )
                }


                // --- Khu vực nội dung chính ---
                // Ưu tiên hiển thị kết quả tìm kiếm nếu searchQuery có giá trị
                if (searchQuery.isNotBlank()) {
                    var hasStartedLoading by remember(searchQuery) { mutableStateOf(false) }

                    val loadState = lazyPagingItems.loadState
                    val isLoading = loadState.refresh is LoadState.Loading

// Đánh dấu là đã bắt đầu loading sau khi query thay đổi và refresh bắt đầu
                    LaunchedEffect(isLoading) {
                        if (isLoading) hasStartedLoading = true
                    }

                    val shouldShowEmptyState = !isLoading &&
                            hasStartedLoading &&
                            loadState.append !is LoadState.Loading &&
                            loadState.prepend !is LoadState.Loading &&
                            lazyPagingItems.itemCount == 0
                    when {
                        loadState.refresh is LoadState.Error -> {
                            ErrorScreenPlaceholder(
                                onClick = { lazyPagingItems.refresh() }
                            )
                        }
                        loadState.refresh is LoadState.Loading -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(7) { ListItemPlaceholder() }
                            }
                        }
                        loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                            if (shouldShowEmptyState){
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No species found.", // String resource
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                                    )
                                    Button(onClick = { lazyPagingItems.refresh() }) {
                                        Text(stringResource(R.string.try_again))
                                    }
                                }
                            }
                            else{
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(7) { ListItemPlaceholder() }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    count = lazyPagingItems.itemCount,
                                    key = lazyPagingItems.itemKey { it.id }
                                ) { index ->
                                    val species = lazyPagingItems[index]
                                    species?.let {
                                        SpeciesListItem(
                                            observationState = species.haveObservation,
                                            species = it, onClick = {
                                                onSpeciesSelected(it)
                                                onDismissRequest()
                                            })
                                    }
                                }
                                item {
                                    when (val appendState = lazyPagingItems.loadState.append) {
                                        is LoadState.Loading -> ListItemPlaceholder(modifier = Modifier.padding(vertical = 8.dp))
                                        is LoadState.Error -> ItemErrorPlaceholder(onClick = { lazyPagingItems.retry() })
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Nếu không, hiển thị kết quả phân tích hoặc màn hình chờ
                    AnalysisContent(
                        analysisState = analysisState,
                        imageUri = imageUri,
                        onSpeciesSelected = { onSpeciesSelected(it) },
                        onDismissRequest = onDismissRequest,
                        onRetry = { viewModel.startImageAnalysis(imageUri) }
                    )
                }
            }
        }
    }
}

// --- Các Composable con để cấu trúc giao diện ---
/*
@Composable
private fun DialogHeader(onDismissRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.search_encyclopedia),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDismissRequest) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close_dialog),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}*/

/*@Composable
private fun SearchContent(
    lazyPagingItems: LazyPagingItems<DisplayableSpecies>,
    onSpeciesSelected: (DisplayableSpecies) -> Unit,
    onDismissRequest: () -> Unit
) {
    val loadState = lazyPagingItems.loadState
    when {
        loadState.refresh is LoadState.Error -> {
            ErrorScreenPlaceholder(
                onClick = { lazyPagingItems.refresh() }
            )
        }
        loadState.refresh is LoadState.Loading -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(7) { ListItemPlaceholder() }
            }
        }
        loadState.refresh is LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No species found.", // String resource
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                )
                Button(onClick = { lazyPagingItems.refresh() }) {
                    Text(stringResource(R.string.try_again))
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.id }
                ) { index ->
                    val species = lazyPagingItems[index]
                    species?.let {
                        SpeciesListItem(
                            observationState = species.haveObservation,
                            species = it, onClick = {
                            onSpeciesSelected(it)
                            onDismissRequest()
                        })
                    }
                }
                item {
                    when (val appendState = lazyPagingItems.loadState.append) {
                        is LoadState.Loading -> ListItemPlaceholder(modifier = Modifier.padding(vertical = 8.dp))
                        is LoadState.Error -> ItemErrorPlaceholder(onClick = { lazyPagingItems.retry() })
                        else -> Unit
                    }
                }
            }
        }
    }
}*/

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AnalysisContent(
    analysisState: AnalysisUiState,
    imageUri: Uri?,
    onSpeciesSelected: (DisplayableSpecies) -> Unit,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /*if (imageUri != null) {
            GlideImage(
                model = imageUri,
                contentDescription = stringResource(R.string.analyzed_image),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )
        }*/

        when (analysisState) {
            is AnalysisUiState.ClassifierInitializing, is AnalysisUiState.ImageProcessing -> {
                LinearProgressIndicator()
                val text = if (analysisState is AnalysisUiState.ImageProcessing)
                    "Analyzing your first image..."//stringResource(R.string.processing_image)
                else
                    "Analyzing your first image..."//stringResource(R.string.initializing_engine)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            is AnalysisUiState.Success -> {
                Text(
                    text = stringResource(R.string.top_suggestion),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                        analysisState.recognitions.size,
                        key = { index -> analysisState.recognitions[index].species.id }) { index ->
                        SpeciesListItem(
                            showAnalysisResult = true,
                            analysisResult = analysisState.recognitions[index].confidence,
                            observationState = analysisState.recognitions[index].species.haveObservation,
                            species = analysisState.recognitions[index].species, onClick = {
                            onSpeciesSelected(analysisState.recognitions[index].species)
                            onDismissRequest()
                        })
                    }
                }
            }
            is AnalysisUiState.Error, is AnalysisUiState.NoResults, is AnalysisUiState.Initial -> {
                // Nếu có imageUri, đây là trạng thái lỗi/không có kết quả từ việc phân tích
                if (imageUri != null) {
                    val errorMessage = (analysisState as? AnalysisUiState.Error)?.message ?: "empty"//stringResource(R.string.no_results_found)
                    Text(text = errorMessage, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    ItemErrorPlaceholder(onClick = onRetry)
                } else {
                    // Nếu không, đây là màn hình chờ mặc định để tìm kiếm bằng văn bản
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your search query is empty, please type something...",//stringResource(R.string.start_searching_prompt),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}