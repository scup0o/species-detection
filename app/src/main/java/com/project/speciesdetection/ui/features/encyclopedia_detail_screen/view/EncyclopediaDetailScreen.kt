package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.project.speciesdetection.data.model.species.DisplayableSpecies

@Composable
fun EncyclopediaDetailScreen(
    species: DisplayableSpecies,
    observationImage : Uri? = null,
    navController: NavController,
){
    IconButton(onClick = { navController.popBackStack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
    Button(
        onClick ={}
    ) {
        Text("add observation for ${species.localizedName}")
    }
}