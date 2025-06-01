package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.core.theme.strokes
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass

@Composable
fun SpeciesClassChip(
    transparentColor: androidx.compose.ui.graphics.Color? = null,
    speciesClass: DisplayableSpeciesClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier =
            Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary
                            else MaterialTheme.colorScheme.tertiary

        ),
        /*border = if (!isSelected) BorderStroke(
            MaterialTheme.strokes.xs,
            MaterialTheme.colorScheme.tertiary) else null,*/
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) MaterialTheme.strokes.l
                                else MaterialTheme.strokes.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs).fillMaxWidth()
        ) {

            if(speciesClass.getIcon()!=null)
                Icon(
                    painter = painterResource(speciesClass.getIcon()!!),
                    contentDescription = null
                )

            Text(
                text = speciesClass.localizedName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }

    }
}