package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorText(
    errorMessage : String
){
    Spacer(modifier = Modifier.height(8.dp))
    Text(errorMessage,
        color = MaterialTheme.colorScheme.error)
}