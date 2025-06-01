package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CircularProgressIndicatorScrim(){
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false // Nếu bạn muốn toàn màn hình
        )
    ) {
        // Scrim + dialog content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.outline.copy(0.1f)) // ✅ Màu scrim tùy chọn
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                tonalElevation = 4.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}