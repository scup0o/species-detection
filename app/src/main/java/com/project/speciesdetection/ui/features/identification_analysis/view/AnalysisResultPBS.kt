package com.project.speciesdetection.ui.features.identification_analysis.view

import MultiSystemConservationStatusView
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.HyperlinkText
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesClassification
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AiIdentificationResult
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisUiState
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun AnalysisResultPBS(
    authViewModel: AuthViewModel,
    navController: NavHostController,
    onDismiss: () -> Unit,
    analysisImage: Uri,
    analysisViewModel: AnalysisViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentAnalysisState by analysisViewModel.uiState.collectAsState()
    val speciesObservationState by analysisViewModel.speciesDateFound.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    if (showAiDialog) {
        AiIdentificationDialog(
            onDismiss = { showAiDialog = false },
            onConfirm = { description ->
                showAiDialog = false
                analysisViewModel.identifyWithGoogleAI(
                    imageUri = analysisImage,
                    description = description
                )
            }
        )
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            analysisViewModel.resetState()
            onDismiss()
        },
    ) {
        Row {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlideImage(
                model = analysisImage,
                contentDescription = "Analyzed Image",
                modifier = Modifier
                    .height(150.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

            when (val state = currentAnalysisState) {
                AnalysisUiState.ClassifierInitializing, AnalysisUiState.ImageProcessing -> {
                    Text(stringResource(R.string.analyzing), modifier = Modifier.padding(vertical = 16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is AnalysisUiState.Success -> {
                    if (state.recognitions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.top_suggestion),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            contentPadding = PaddingValues(vertical = MaterialTheme.spacing.xxxs),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            items(state.recognitions, key = { it.species.id }) { recognition ->
                                SpeciesListItem(
                                    showAnalysisResult = true,
                                    analysisResult = recognition.confidence,
                                    observationState = speciesObservationState[recognition.species.id] != null,
                                    species = recognition.species,
                                    onClick = {
                                        navController.navigate(
                                            AppScreen.EncyclopediaDetailScreen.createRoute(
                                                species = recognition.species,
                                                imageUri = analysisImage
                                            )
                                        ) {
                                            launchSingleTop = true
                                        }
                                    })
                            }
                        }

                        if (authState.currentUser != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showAiDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.need_another_opinion_try_ai))
                            }
                        }
                    } else {
                        ItemErrorPlaceholder(onClick = { analysisViewModel.startImageAnalysis(analysisImage) })
                    }
                }

                is AnalysisUiState.Error, AnalysisUiState.NoResults, AnalysisUiState.Initial -> {
                    ItemErrorPlaceholder(onClick = { analysisViewModel.startImageAnalysis(analysisImage) })
                    if (authState.currentUser != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showAiDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.identify_with_ai))
                        }
                    }
                }

                AnalysisUiState.AiIdentifying -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 32.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.ai_is_thinking), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                is AnalysisUiState.AiSuccess -> {
                    Text(
                        stringResource(R.string.ai_suggestion),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Sử dụng weight để AiResultView có thể mở rộng và cuộn
                    Box(modifier = Modifier.weight(1f)) {
                        AiResultView(
                            result = state.result,
                            navController = navController,
                            imageUri = analysisImage
                        )
                    }
                }

                is AnalysisUiState.AiError -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAiDialog = true }) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            }
        }
    }
}

@Composable
fun AiResultView(
    result: AiIdentificationResult,
    navController: NavHostController,
    imageUri: Uri
) {
    if (result.speciesFromServer != null) {
        // Trường hợp 1: Tìm thấy trong CSDL của bạn (giữ nguyên)
        SpeciesListItem(
            showObservationState = false,
            species = result.speciesFromServer,
            onClick = {
                navController.navigate(
                    AppScreen.EncyclopediaDetailScreen.createRoute(
                        species = result.speciesFromServer,
                        imageUri = imageUri
                    )
                ) {
                    launchSingleTop = true
                }
            }
        )
    } else {
        // Trường hợp 2: Không tìm thấy, hiển thị chi tiết từ AI
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    // Header (Tên, tên khoa học)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            result.aiInfo.commonName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            ", " + stringResource(R.string.species_family_description) + " ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            result.aiInfo.family?:"",
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
                            result.aiInfo.scientificName?:"",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    /*Text(
                        text = result.aiInfo.commonName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.aiInfo.scientificName,
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )*/
                    Spacer(Modifier.height(MaterialTheme.spacing.m))

                    // Tóm tắt
                    Text(
                        text = result.aiInfo.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.m)
                    )

                    // Tình trạng bảo tồn
                    Text(
                        stringResource(R.string.species_detail_conservation_status),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.xs)
                    )
                    MultiSystemConservationStatusView(
                        iucnStatusCode = result.aiInfo.conservationStatus,
                        otherSystems = emptyMap()
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.l))
                }
            }

            // Phân loại
            item {
                DetailSection(title = stringResource(R.string.species_detail_classification)) {
                    SpeciesClassification(
                        domain = result.aiInfo.domain,
                        kingdom = result.aiInfo.kingdom,
                        phylum = result.aiInfo.phylum,
                        scientificClass = result.aiInfo.aClass, // Đổi tên để không trùng keyword
                        order = result.aiInfo.order,
                        scientificFamily = result.aiInfo.family, // Đổi tên để không trùng keyword
                        genus = result.aiInfo.genus,
                        speciesName = result.aiInfo.scientificName
                    )
                }
            }

            // Các mục chi tiết
            result.aiInfo.physicalDescription?.takeIf { it.isNotBlank() }?.let {
                item { DetailSection(title = stringResource(R.string.species_detail_physical), content = it) }
            }
            result.aiInfo.distribution?.takeIf { it.isNotBlank() }?.let {
                item { DetailSection(title = stringResource(R.string.species_detail_distribution), content = it) }
            }
            result.aiInfo.habitat?.takeIf { it.isNotBlank() }?.let {
                item { DetailSection(title = stringResource(R.string.species_detail_habitat), content = it) }
            }
            result.aiInfo.behavior?.takeIf { it.isNotBlank() }?.let {
                item { DetailSection(title = stringResource(R.string.species_detail_behavior), content = it) }
            }

            // Links
            if (result.aiInfo.links.any { it.value != null }) {
                item {
                    DetailSection(title = stringResource(R.string.species_detail_more_info)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.aiInfo.links.forEach { (site, url) ->
                                if (url != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        HyperlinkText(
                                            fullText = "${site.replaceFirstChar { it.titlecase(Locale.ROOT) }}: " + url,
                                            linkText = url,
                                            url = url
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.s)
        )
        content()
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: String
) {
    DetailSection(title = title) {
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AiIdentificationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.provide_more_info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ai_will_give_better_results_simple))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    placeholder = { Text(stringResource(R.string.description_placeholder_with_location)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(description) }) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}