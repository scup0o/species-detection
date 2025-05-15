package com.project.speciesdetection.core.navigation

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.speciesdetection.ui.features.community_main_screen.view.CommunityFeed
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.view.EncyclopediaMainScreen
import com.project.speciesdetection.ui.features.identification_camera_screen.view.CameraScreen
import com.project.speciesdetection.ui.features.profile_main_screen.view.ProfileMainScreen
import com.project.speciesdetection.ui.features.setting_main_screen.view.SettingMainScreen
import androidx.core.net.toUri
import com.project.speciesdetection.ui.features.identification_edit_image_screen.view.EditImageForIdentificationScreen

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

    val containerColor = MaterialTheme.colorScheme.surfaceContainer

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
                navController = navController,
                containerColor = containerColor)
        }
        composable(
            route = AppScreen.EncyclopediaMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ) {
            EncyclopediaMainScreen(
                navController = navController,
                containerColor = containerColor
            )
        }

        composable(
            route = AppScreen.ProfileMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            ProfileMainScreen(
                navController = navController,
                containerColor = containerColor)
        }

        composable(
            route = AppScreen.SettingMainScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            SettingMainScreen(
                navController = navController)
        }

        composable(
            route = AppScreen.CameraScreen.route
        ) {
            CameraScreen(
                navigateToEditScreen = { uri ->
                    // Chuyển Uri dưới dạng string vì Navigation không hỗ trợ trực tiếp Uri phức tạp
                    // Screen 2 sẽ cần parse lại Uri này
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("editScreen/$encodedUri")
                }
            )
        }

        composable(
            route = AppScreen.EditImageForIdentificationScreen.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { EditImageForIdentificationScreen(
            onNavigateBack = { navController.popBackStack() },
        )
        }




    }

}