package com.project.speciesdetection.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.speciesdetection.ui.features.community_main_screen.view.CommunityFeed

@Composable
fun AppNavigation(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppScreen.CommunityScreen.route
    ){
        composable(
            route = AppScreen.CommunityScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = { ExitTransition.None}
        ){
            CommunityFeed()
        }
    }

}