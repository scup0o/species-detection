package com.project.speciesdetection.ui.features.identification_analysis.view

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.speciesdetection.R
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.domain.provider.image_classifier.Recognition
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisUiState
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisViewModel

@Composable
fun AnalysisButton(
    modifier: Modifier = Modifier,
    currentImageUriToAnalyze: Uri?,
    onAnalysisActionTriggered: () -> Unit, // Callback khi nút được nhấn và action bắt đầu
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val analysisUiState by viewModel.uiState.collectAsState()

    val isLoading = analysisUiState is AnalysisUiState.ClassifierInitializing ||
            analysisUiState is AnalysisUiState.ImageProcessing

    val isButtonClickable = currentImageUriToAnalyze != null && !isLoading

    ExtendedFloatingActionButton(
        onClick = {
            if (isButtonClickable) {
                onAnalysisActionTriggered() // Thông báo cho parent Composable (EditImageScreen)
                viewModel.startImageAnalysis(currentImageUriToAnalyze)
            }
        },
        modifier = modifier,
        icon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start Analysis", tint = MaterialTheme.colorScheme.onTertiary)
            }
        },
        text = {
            if (isLoading) {
                Text(stringResource(R.string.is_identifying_button))
            } else {
                Text(stringResource(R.string.identify_button), color = MaterialTheme.colorScheme.onTertiary)
            }
        },
        containerColor = if (isButtonClickable) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        },
        contentColor = if (isButtonClickable) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if (isButtonClickable) 6.dp else 0.dp)
    )
}