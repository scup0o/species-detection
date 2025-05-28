package com.project.speciesdetection.ui.features.identification_analysis.view

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.domain.provider.image_classifier.Recognition // Đảm bảo import đúng
import com.project.speciesdetection.ui.composable.common.ItemErrorPlaceholder
import com.project.speciesdetection.ui.composable.common.species.SpeciesListItem
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisUiState
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun AnalysisResultPBS(
    navController: NavHostController,
    onDismiss: () -> Unit,
    analysisImage: Uri,
    analysisViewModel: AnalysisViewModel = hiltViewModel(),)
{
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentAnalysisState by analysisViewModel.uiState.collectAsState() // Lấy state từ ViewModel

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            analysisViewModel.resetState()
            onDismiss()},
        //modifier = Modifier.defaultMinSize(minHeight = 200.dp)
    ) {
        Row(){
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,null
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Text("Analysis Result", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            GlideImage(
                model = analysisImage,
                contentDescription = "Analyzed Image",
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            when (val state = currentAnalysisState) {
                AnalysisUiState.ClassifierInitializing -> {
                    CircularProgressIndicator()
                }
                AnalysisUiState.ImageProcessing -> {
                    CircularProgressIndicator()
                }
                is AnalysisUiState.Success -> {
                    if (state.recognitions.isNotEmpty()) {
                        Text(
                            "Top suggestions:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding =
                                PaddingValues(
                                    vertical = MaterialTheme.spacing.xxxs),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                        ) {
                            items(state.recognitions, key = { it.id }) { species ->
                                SpeciesListItem(
                                    species=species,
                                    onClick = {
                                        navController.popBackStack(
                                            AppScreen.EncyclopediaDetailScreen.createRoute(
                                                species = species,
                                                imageUri = analysisImage
                                            ),
                                            inclusive = true,
                                            saveState = false)
                                        navController.navigate(
                                            AppScreen.EncyclopediaDetailScreen.createRoute(
                                                species = species,
                                                imageUri = analysisImage
                                            )
                                        ) {
                                            launchSingleTop = true
                                        }
                                    })
                            }

                        }
                        //Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                    } else {
                        ItemErrorPlaceholder(onClick ={analysisViewModel.startImageAnalysis(analysisImage)})
                    }
                }
                is AnalysisUiState.Error -> {
                    ItemErrorPlaceholder(onClick ={analysisViewModel.startImageAnalysis(analysisImage)})
                }
                AnalysisUiState.NoResults -> {
                    ItemErrorPlaceholder(onClick ={analysisViewModel.startImageAnalysis(analysisImage)})
                }
                AnalysisUiState.Initial -> {
                    ItemErrorPlaceholder(onClick ={analysisViewModel.startImageAnalysis(analysisImage)})
                }
            }
        }
    }
}
