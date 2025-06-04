package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species_class.DisplayableSpeciesClass

@Composable
fun CustomChip(
    transparentColor: androidx.compose.ui.graphics.Color? = null,
    title : String,
    vectorIcon : ImageVector? = null,
    painterIcon : Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
){
    Card(
        modifier =
            Modifier.clickable(onClick = onClick),
        shape = /*RoundedCornerShape(
            topStartPercent = 65,
            topEndPercent = 5,
            bottomStartPercent = 10,
            bottomEndPercent = 65
        )customCardShape()*/
            RoundedCornerShape(
                topStartPercent = 50,
                topEndPercent = 50,
                bottomStartPercent = 0,
                bottomEndPercent = 0),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary
            else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary
            else MaterialTheme.colorScheme.tertiary

        ),
        /*border = if (!isSelected) BorderStroke(
            MaterialTheme.strokes.xs,
            MaterialTheme.colorScheme.tertiary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) MaterialTheme.strokes.l
                                else MaterialTheme.strokes.xs)*/
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.m, vertical = MaterialTheme.spacing.xs).fillMaxWidth()
        ) {

            if (vectorIcon!=null){
                Icon(
                    vectorIcon,
                    contentDescription = null
                )
            }
            if(painterIcon!=null)
                Icon(
                    painter = painterResource(painterIcon),
                    contentDescription = null
                )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }

    }
}