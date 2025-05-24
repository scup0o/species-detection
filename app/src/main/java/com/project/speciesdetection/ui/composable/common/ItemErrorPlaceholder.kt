package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R

@Composable
fun ItemErrorPlaceholder(
    onClick : () -> Unit,
){
    Text(
        stringResource(R.string.error_screen_title),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onClick) {
        Text(stringResource(R.string.try_again))
    }
}