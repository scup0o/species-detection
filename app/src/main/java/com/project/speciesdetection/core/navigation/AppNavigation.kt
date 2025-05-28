package com.project.speciesdetection.core.navigation

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.features.community_main_screen.view.CommunityFeed
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.view.EncyclopediaMainScreen
import com.project.speciesdetection.ui.features.profile_main_screen.view.ProfileMainScreen
import com.project.speciesdetection.ui.features.setting_main_screen.view.SettingMainScreen
import com.project.speciesdetection.ui.features.identification_edit_image_screen.view.EditImageForIdentificationScreen
import kotlinx.serialization.json.Json
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.speciesdetection.ui.features.auth.view.AuthScreen
import com.project.speciesdetection.ui.features.auth.view.ForgotPasswordScreen
import com.project.speciesdetection.ui.features.auth.view.LoginScreen
import com.project.speciesdetection.ui.features.auth.view.SignupScreen
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view.EncyclopediaDetailScreen

@Composable
fun AppNavigation(
    activity : Activity
){
    val json = Json { ignoreUnknownKeys = true }
    val navController = rememberNavController()
    val authViewModel : AuthViewModel = hiltViewModel()

    // Danh sách các route là tab gốc
    val rootDestinations = listOf(
        AppScreen.CommunityScreen.route,
        AppScreen.EncyclopediaMainScreen.route,
        AppScreen.ProfileMainScreen.route,
        //AppScreen.SettingMainScreen.route
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
                    containerColor = containerColor,
                    authViewModel = authViewModel)

        }
        composable(
            route = AppScreen.LoginScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){

            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.SignUpScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            SignupScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.ForgotPasswordScreen.route,
            enterTransition = {EnterTransition.None},
            exitTransition = {ExitTransition.None}
        ){
            ForgotPasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
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
                navController = navController,
                authViewModel = authViewModel)
        }

        /*composable(
            route = AppScreen.CameraScreen.route
        ) {
            CameraScreen(
                onNavigateToEditScreen = { uri ->
                    // Chuyển Uri dưới dạng string vì Navigation không hỗ trợ trực tiếp Uri phức tạp
                    // Screen 2 sẽ cần parse lại Uri này
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("editScreen/$encodedUri")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }*/

        composable(
            route = AppScreen.EditImageForIdentificationScreen.route, // imageUri là placeholder
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { // backStackEntry -> không cần dùng trực tiếp ở đây vì ViewModel lấy từ SavedStateHandle
            EditImageForIdentificationScreen(
                navController = navController
            )
        }

        composable(
            route = AppScreen.EncyclopediaDetailScreen.route,
            arguments = listOf(
                navArgument("speciesJson") { type = NavType.StringType },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {backStackEntry ->
            val speciesJsonEncoded = backStackEntry.arguments?.getString("speciesJson")
            val speciesJson = speciesJsonEncoded?.let { Uri.decode(it) }
            val species = speciesJson?.let {
                try {
                    json.decodeFromString<DisplayableSpecies>(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val imageUriEncoded = backStackEntry.arguments?.getString("imageUri")
            val imageUri = imageUriEncoded?.let { Uri.decode(it).toUri() }

            if (species != null) {
                EncyclopediaDetailScreen(
                    species = species,
                    observationImage = imageUri,
                    navController = navController
                )
            } else {
                // Text("Error: Could not load species data.")
                navController.popBackStack()
            }
        }
    }

}