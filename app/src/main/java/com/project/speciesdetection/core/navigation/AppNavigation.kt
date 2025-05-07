package com.project.speciesdetection.core.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.speciesdetection.ui.features.community_main_screen.view.CommunityFeed
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.view.EncyclopediaMainScreen
import com.project.speciesdetection.ui.features.profile_main_screen.view.ProfileMainScreen
import com.project.speciesdetection.ui.features.setting_main_screen.view.SettingMainScreen

@Composable
fun AppNavigation(
    activity : Activity
){
    val navController = rememberNavController()

    // Danh sách các route là tab gốc
    val rootDestinations = listOf(
        AppScreen.CommunityScreen.route,
        AppScreen.EncyclopediaMainScreen.route,
        AppScreen.ProfileMainScreen.route,
        AppScreen.SettingMainScreen.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Nếu màn hình hiện tại là một trong các tab gốc VÀ không có gì phía trước nó trong back stack
    // của NavHost này, thì BackHandler sẽ được kích hoạt để đóng app.
    if (currentRoute in rootDestinations) {
        BackHandler(enabled = true) {
            activity.finish()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppScreen.CommunityScreen.route
    ){
        composable(
            route = AppScreen.CommunityScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            CommunityFeed(
                navController = navController)
        }
        composable(
            route = AppScreen.EncyclopediaMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ) {
            EncyclopediaMainScreen(
                navController = navController
            )
        }

        composable(
            route = AppScreen.ProfileMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            ProfileMainScreen(
                navController = navController)
        }

        composable(
            route = AppScreen.SettingMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            SettingMainScreen(
                navController = navController)
        }


    }

}