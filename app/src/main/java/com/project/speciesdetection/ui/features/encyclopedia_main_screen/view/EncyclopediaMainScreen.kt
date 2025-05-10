package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel

@Composable
fun EncyclopediaMainScreen(
    containerColor : Color? = MaterialTheme.colorScheme.background,
    navController: NavHostController,
    viewModel : EncyclopediaMainScreenViewModel = hiltViewModel(),
){
    val speciesClassList by viewModel.speciesClassList.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = containerColor!!,
        bottomBar = { BottomNavigationBar(navController) }
    ){
            innerPadding ->
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