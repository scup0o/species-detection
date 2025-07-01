package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.core.theme.spacing
import com.valentinilk.shimmer.shimmer

@Composable
fun ListItemPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .shimmer()
                .padding(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.s))
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(0.7f)
                        .background(MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .height(MaterialTheme.spacing.m)
                        .fillMaxWidth(0.5f)
                        .background(MaterialTheme.colorScheme.outlineVariant, shape = MaterialTheme.shapes.small)
                )
            }
        }
    }
}
