package com.project.speciesdetection.ui.features.profile_main_screen.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar

@Composable
fun ProfileMainScreen(
    containerColor : Color? = MaterialTheme.colorScheme.background,
    navController : NavHostController
){
    Scaffold(
        containerColor = containerColor!!,
        bottomBar = { BottomNavigationBar(navController) }
    ){
            innerPadding ->
        Text(
            modifier = Modifier.padding(innerPadding),
            text="profile")
    }
}