package com.project.speciesdetection.ui.composable.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.project.speciesdetection.R
import com.project.speciesdetection.core.theme.spacing

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ErrorScreenPlaceholder(
    onClick : ()-> Unit
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {

        GlideImage(
            model = R.drawable.page_eaten,
            contentDescription = null,
            modifier = Modifier.size(300.dp).padding(bottom = MaterialTheme.spacing.m),
        )

        Text(
            text = stringResource(R.string.error_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.error_screen_description_1))
            Text(stringResource(R.string.error_screen_description_2))
        }

        Text(stringResource(R.string.error_screen_description_solution))

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

        Button(
            onClick = onClick,
            modifier = Modifier.width(250.dp)
        ) {
            Text(
                text = stringResource(R.string.try_again),
                fontWeight = FontWeight.Bold)
        }
    }
}