package com.project.speciesdetection.ui.composable.common.species

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
fun SpeciesListItem(
    species: DisplayableSpecies,
    observationState : Boolean = false,
    onClick : () -> Unit,
    showObservationState : Boolean = true,
    analysisResult: Float = 0f,
    showAnalysisResult : Boolean = false,
    ) {
    val color = MaterialTheme.colorScheme.outline.copy(0.5f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 4.dp.toPx()
                ambientShadowColor =  color// Màu của bóng (phát sáng)
                spotShadowColor = color
                clip=true
                shape = RoundedCornerShape(20.dp)
            }
            /*.shadow(
                elevation = MaterialTheme.spacing.m,
                shape = RoundedCornerShape(percent = 10),
                spotColor = Color.Transparent,
                ambientColor = MaterialTheme.colorScheme.surface)*/
            .clickable(
                onClick = onClick
            ),
    ) {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
            ) {
                GlideImage(
                    model = species.thumbnailImageURL,
                    contentDescription = species.localizedName,
                    loading = placeholder(R.drawable.error_image),
                    failure = placeholder(R.drawable.error_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(MaterialTheme.spacing.xxxs)
                        .clip(MaterialTheme.shapes.small)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = species.localizedName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                // Mô tả với style bình thường
                                withStyle(style = MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                                    color = MaterialTheme.colorScheme.outline
                                )) {
                                    append(stringResource(R.string.species_family_description) + " ")
                                }

                                // Family name với style đậm
                                withStyle(style = MaterialTheme.typography.bodyMedium.toSpanStyle().copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )) {
                                    append(species.localizedFamily)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = species.getScientificName()?:"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (showAnalysisResult){
                        Row(Modifier.fillMaxWidth(),
                            ) {
                            Text(stringResource(R.string.confidence)+": %.2f%%".format(analysisResult*100),
                                style=MaterialTheme.typography.bodyMedium,
                                color=MaterialTheme.colorScheme.tertiary,
                                fontStyle = FontStyle.Italic)
                        }
                    }
                }
                if (showObservationState){
                    Image(
                        painter = if (observationState) painterResource(R.drawable.butterfly_net) else painterResource(R.drawable.butterfly_net_disabeld),
                        contentDescription = null,
                        modifier = Modifier.size(45.dp).padding(horizontal = 5.dp)
                    )
                }
            }


    }
}