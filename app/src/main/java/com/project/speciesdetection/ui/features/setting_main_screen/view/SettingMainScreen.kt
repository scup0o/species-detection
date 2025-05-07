package com.project.speciesdetection.ui.features.setting_main_screen.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar

@Composable
fun SettingMainScreen(
    navController : NavHostController
){
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ){
            innerPadding ->
        Text(
            modifier = Modifier.padding(innerPadding),
            text="setting")
    }
}