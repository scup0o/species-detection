package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.speciesdetection.core.theme.spacing
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
                .padding(
                    horizontal = MaterialTheme.spacing.l,
                    vertical = MaterialTheme.spacing.xs)
        ){
            Text(
                text = "",
            )
        }
    }
}