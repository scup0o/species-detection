package com.project.speciesdetection.ui.composable.common.species

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies

@Composable
fun SpeciesClassification(
    species: DisplayableSpecies,
    modifier: Modifier = Modifier
) {
    val classificationData = listOf(
        Triple(R.string.species_classification_domain, species.localizedDomain, species.getScientificDomain()),
        Triple(R.string.species_classification_kingdom, species.localizedKingdom, species.getScientificKingdom()),
        Triple(R.string.species_classification_phylum, species.localizedPhylum, species.getScientificPhylum()),
        Triple(R.string.species_classification_class, species.localizedClass, species.getScientificClass()),
        Triple(R.string.species_classification_order, species.localizedOrder, species.getScientificOrder()),
        Triple(R.string.species_classification_family, species.localizedFamily, species.getScientificFamily()),
        Triple(R.string.species_classification_genus, species.localizedGenus, species.getScientificGenus()),
        Triple(R.string.species_classification_species, null, species.getScientificName())
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.m, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        classificationData.forEach { (labelResId, localizedNameData, scientificNameData) ->
            val finalLocalizedName = localizedNameData?.takeIf { it.isNotBlank() }
            val finalScientificName = scientificNameData?.takeIf { it.isNotBlank() }

            // Chỉ hiển thị dòng nếu có ít nhất một trong hai thông tin
            // Hoặc bạn có thể bỏ điều kiện if này nếu muốn luôn hiển thị "N/A"
            if (finalLocalizedName != null || finalScientificName != null) {
                ClassificationDataRow(
                    labelResId = labelResId,
                    localizedName = finalLocalizedName,
                    scientificName = finalScientificName
                )
            }
            // Nếu bạn muốn hiển thị "N/A" ngay cả khi cả hai đều null:
            // ClassificationDataRow(
            // labelResId = labelResId,
            // localizedName = finalLocalizedName,
            // scientificName = finalScientificName
            // )
        }
    }
}

@Composable
fun ClassificationDataRow(
    labelResId: Int,
    localizedName: String?,
    scientificName: String?,
    notAvailableTextResId: Int = R.string.iucn_status_unknown
) {
    val label = stringResource(id = labelResId)
    val notAvailableText = stringResource(id = notAvailableTextResId)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(0.2f)
                .padding(end = 4.dp),
            color = MaterialTheme.colorScheme.tertiary
        )
        Column(
            modifier = Modifier.weight(0.8f)
        ) {
            val displayLocalized = localizedName?.takeIf { it.isNotBlank() }
            val displayScientific = scientificName?.takeIf { it.isNotBlank() }

            if (displayLocalized != null) {
                Row(){
                    Text(
                        text = displayLocalized,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (displayScientific != null) {
                        Text(
                            text = "  ($displayScientific)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

            } else if (displayScientific != null) {
                Text(
                    text = displayScientific,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = notAvailableText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}