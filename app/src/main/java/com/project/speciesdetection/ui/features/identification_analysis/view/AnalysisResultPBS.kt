package com.project.speciesdetection.ui.features.identification_analysis.view

import android.net.Uri
import androidx.compose.foundation.layout.*
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
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.domain.provider.image_classifier.Recognition // Đảm bảo import đúng
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisUiState
import com.project.speciesdetection.ui.features.identification_analysis.viewmodel.AnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun AnalysisResultPBS(
    onDismiss: () -> Unit,
    analysisImage: Uri,
    analysisViewModel: AnalysisViewModel // Nhận AnalysisViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val currentAnalysisState by analysisViewModel.uiState.collectAsState() // Lấy state từ ViewModel

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = 200.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Analysis Result", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            GlideImage(
                model = analysisImage,
                contentDescription = "Analyzed Image",
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            when (val state = currentAnalysisState) {
                AnalysisUiState.ClassifierInitializing -> {
                    CircularProgressIndicator()
                    Text("Initializing analysis engine...", modifier = Modifier.padding(top = 8.dp))
                }
                AnalysisUiState.ImageProcessing -> {
                    CircularProgressIndicator()
                    Text("Analyzing image...", modifier = Modifier.padding(top = 8.dp))
                }
                is AnalysisUiState.Success -> {
                    if (state.recognitions.isNotEmpty()) {
                        Text("Top Predictions:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        state.recognitions.forEach { recognition ->
                            RecognitionItemInBottomSheet(recognition)
                        }
                    } else { // Trường hợp này nên được xử lý bởi NoResults
                        Text("Analysis complete, but no specific organisms recognized.")
                    }
                }
                is AnalysisUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                AnalysisUiState.NoResults -> {
                    Text("No organisms could be identified in this image.", textAlign = TextAlign.Center)
                }
                AnalysisUiState.Initial -> {
                    Text("Analysis has not started or was reset.", textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss) { Text("Close") }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RecognitionItemInBottomSheet(recognition: Recognition) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = recognition.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = String.format("%.1f%%", recognition.confidence * 100), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}