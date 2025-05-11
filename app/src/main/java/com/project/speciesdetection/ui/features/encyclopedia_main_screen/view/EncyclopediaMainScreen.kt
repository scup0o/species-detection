package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EncyclopediaMainScreen(
    containerColor : Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    viewModel : EncyclopediaMainScreenViewModel = hiltViewModel(),
){
    val speciesClassList by viewModel.speciesClassList.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = containerColor!!,
        bottomBar = { BottomNavigationBar(navController) }
    ){
            innerPadding ->
        when (uiState){
            is EncyclopediaMainScreenViewModel.SpeciesScreenUiState.Success -> {
                GlideImage(
                    model = "https://res.cloudinary.com/degflo4pi/image/upload/ar_1:1,c_fill,g_auto/v1746955323/"+(uiState as EncyclopediaMainScreenViewModel.SpeciesScreenUiState.Success).speciesList[0].imageURL,
                    contentDescription = null,
                    loading = placeholder(R.drawable.error_image),
                    failure = placeholder(R.drawable.error_image)
                )
            }
            is EncyclopediaMainScreenViewModel.SpeciesScreenUiState.Loading -> {

            }

            is EncyclopediaMainScreenViewModel.SpeciesScreenUiState.Error -> {

            }

            is EncyclopediaMainScreenViewModel.SpeciesScreenUiState.Empty -> {}
        }


        Text(
            modifier = Modifier.padding(innerPadding),
            text="encyclopedia")

        speciesClassList.forEach{ speciesClass ->
            Text(
                modifier = Modifier.padding(innerPadding),
                text=speciesClass.localizedName)
        }
    }
}