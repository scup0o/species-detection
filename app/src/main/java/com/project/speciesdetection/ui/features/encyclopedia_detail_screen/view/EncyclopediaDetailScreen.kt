package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.project.speciesdetection.data.model.species.DisplayableSpecies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaDetailScreen(
    species: DisplayableSpecies,
    observationImage : Uri? = null,
    navController: NavController,
){
    Scaffold(
        bottomBar = {
            Button(
                onClick ={}
            ) {
                Text(
                    if (observationImage!=null) "Record Observation"
                    else "Add Observation")
            }
        }
    ){ innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ){
            Row(
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }


        }
    }

}