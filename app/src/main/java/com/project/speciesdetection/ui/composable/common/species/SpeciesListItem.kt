package com.project.speciesdetection.ui.composable.common.species

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.core.theme.strokes
import com.project.speciesdetection.data.model.species.DisplayableSpecies

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SpeciesListItem(species: DisplayableSpecies) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
            .shadow(
                elevation = MaterialTheme.strokes.m,
                shape = MaterialTheme.shapes.small,
                spotColor = MaterialTheme.colorScheme.outline,
                ambientColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            GlideImage(
                model = species.imageURL,
                contentDescription = species.localizedName,
                loading = placeholder(R.drawable.error_image),
                failure = placeholder(R.drawable.error_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .padding(MaterialTheme.spacing.xxxs)
                    .clip(MaterialTheme.shapes.small)
            )
            Column {
                Text(
                    text = species.localizedName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Row(){
                    Text(
                        text = stringResource(R.string.species_family_description)+" ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = species.localizedFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = species.getScientificName()!!,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}