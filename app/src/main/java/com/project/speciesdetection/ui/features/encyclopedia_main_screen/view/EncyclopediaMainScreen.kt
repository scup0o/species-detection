package com.project.speciesdetection.ui.features.encyclopedia_main_screen.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.viewmodel.EncyclopediaMainScreenViewModel

@Composable
fun EncyclopediaMainScreen(
    navController: NavHostController,
    viewModel : EncyclopediaMainScreenViewModel = hiltViewModel()
){
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ){
            innerPadding ->
        Text(
            modifier = Modifier.padding(innerPadding),
            text="encyclopedia")
    }
}