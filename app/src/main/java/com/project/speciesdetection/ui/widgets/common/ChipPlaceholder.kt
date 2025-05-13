package com.project.speciesdetection.ui.widgets.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.core.theme.strokes
import com.valentinilk.shimmer.shimmer

@Composable
fun ChipPlacholder(){
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.shimmer()
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.m)
        ){}
    }
}