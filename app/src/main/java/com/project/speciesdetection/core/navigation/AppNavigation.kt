package com.project.speciesdetection.core.navigation

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.speciesdetection.ui.features.community_main_screen.view.CommunityFeed
import com.project.speciesdetection.ui.features.encyclopedia_main_screen.view.EncyclopediaMainScreen
import com.project.speciesdetection.ui.features.profile_main_screen.view.ProfileMainScreen
import com.project.speciesdetection.ui.features.setting_main_screen.view.SettingMainScreen
import com.project.speciesdetection.ui.features.identification_edit_image_screen.view.EditImageForIdentificationScreen
import kotlinx.serialization.json.Json
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.project.speciesdetection.ui.features.media_screen.view.FullScreenImageViewer
import com.project.speciesdetection.ui.features.auth.view.ForgotPasswordScreen
import com.project.speciesdetection.ui.features.auth.view.LoginScreen
import com.project.speciesdetection.ui.features.auth.view.SignupScreen
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.community_main_screen.view.notification.NotificationView
import com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view.EncyclopediaDetailScreen
import com.project.speciesdetection.ui.features.observation.view.UpdateObservation
import com.project.speciesdetection.ui.features.observation.view.detail.ObservationDetailView
import com.project.speciesdetection.ui.features.observation.view.map.MapPickerScreen
import com.project.speciesdetection.ui.features.observation.view.species_observation.SpeciesObservationMainScreen
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationEvent
import com.project.speciesdetection.ui.features.observation.viewmodel.ObservationViewModel
import com.project.speciesdetection.ui.features.observation.viewmodel.detail.ObservationDetailViewModel
import com.project.speciesdetection.ui.features.setting_main_screen.viewmodel.SettingViewModel

@Composable
fun AppNavigation(
    activity: Activity,
    navController: NavHostController,
) {
    val json = Json { ignoreUnknownKeys = true }

    val authViewModel: AuthViewModel = hiltViewModel()
    val settingViewModel: SettingViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

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
    ) {
        composable(
            route = AppScreen.CommunityScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            CommunityFeed(
                navController = navController,
                //containerColor = containerColor,
                authViewModel = authViewModel
            )

        }
        composable(
            route = AppScreen.LoginScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {

            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.SignUpScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SignupScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.ForgotPasswordScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            ForgotPasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.EncyclopediaMainScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            EncyclopediaMainScreen(
                navController = navController,
                containerColor = containerColor,
                settingViewModel = settingViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.ProfileMainScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            if (authState.currentUser!=null)
            ProfileMainScreen(
                navController = navController,
                containerColor = containerColor,
                authViewModel = authViewModel,
                uid = ""
            )
            else
                LaunchedEffect(Unit) {
                    navController.navigate(AppScreen.LoginScreen.route) {
                        popUpTo(AppScreen.ProfileMainScreen.route) { inclusive = true }
                    }
                }
        }

        composable(
            route = AppScreen.CommunityProfileMainScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            arguments = listOf(
                navArgument("uid"){
                    type=NavType.StringType
                    nullable=true
                }
            )
        ) { navBackStackEntry->
            val uid = navBackStackEntry.arguments?.getString("uid")
            ProfileMainScreen(
                navController = navController,
                containerColor = containerColor,
                authViewModel = authViewModel,
                uid = uid?:""
            )
        }

        composable(
            route = AppScreen.SettingMainScreen.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SettingMainScreen(
                navController = navController,
                authViewModel = authViewModel,
                settingViewModel = settingViewModel
            )
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
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.EncyclopediaDetailScreen.route,
            arguments = listOf(
                navArgument("baseSpeciesJson") { type = NavType.StringType },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            /*val speciesJsonEncoded = backStackEntry.arguments?.getString("speciesJson")
            val speciesJson = speciesJsonEncoded?.let { Uri.decode(it) }
            val species = speciesJson?.let {
                try {
                    json.decodeFromString<DisplayableSpecies>(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }*/

            val imageUriEncoded = backStackEntry.arguments?.getString("imageUri")
            val imageUri = imageUriEncoded?.let { Uri.decode(it).toUri() }

            EncyclopediaDetailScreen(
                observationImage = imageUri,
                navController = navController,
                authViewModel = authViewModel
            )

        }

        composable(
            route = AppScreen.FullScreenImageViewer.route,
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val imageUriEncoded = backStackEntry.arguments?.getString("imageUri")
            val imageUri = imageUriEncoded?.let { Uri.decode(it).toUri() }
            val transform = if (imageUriEncoded?.contains("/upload/")==true) true else false

            FullScreenImageViewer(
                image = imageUri!!,
                onNavigateBack = {navController.popBackStack()},
                transform = transform
            )

        }

        composable(
            route = AppScreen.SpeciesObservationMainScreen.route,
            arguments = listOf(
                navArgument("speciesId") {
                    type = NavType.StringType
                },
                navArgument("speciesName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val speciesId = backStackEntry.arguments?.getString("speciesId")
            val speciesName = backStackEntry.arguments?.getString("speciesName")

            SpeciesObservationMainScreen(
                navController,
                authViewModel,
                speciesId!!,
                speciesName!!
            )

        }

        composable(
            route = AppScreen.UpdateObservationScreen.route,
            arguments = listOf(
                navArgument("speciesId") { type = NavType.StringType; nullable = true },
                navArgument("speciesName") { type = NavType.StringType; nullable = true },
                navArgument("speciesSN") { type = NavType.StringType; nullable = true },
                navArgument("imageUri") { type = NavType.StringType; nullable = true },
                navArgument("observationJson") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            // QUAN TRỌNG: Lấy ViewModel được chia sẻ từ NavGraph
            // Hilt sẽ tạo ra 1 instance của ViewModel và gắn nó vào backStackEntry này
            val observationViewModel: ObservationViewModel = hiltViewModel(backStackEntry)

            UpdateObservation(
                authViewModel = authViewModel,
                viewModel = observationViewModel, // Truyền shared ViewModel
                navController = navController,
                onDismiss = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() } // Đơn giản là quay lại
            )
        }

        // 2. Màn hình MapPicker, sẽ chia sẻ ViewModel với UpdateObservation
        composable(
            route = AppScreen.MapPickerScreen.route
        ) { backStackEntry ->
            // QUAN TRỌNG: Tìm back stack entry của màn hình TRƯỚC ĐÓ
            // và lấy ra instance ViewModel đã được tạo ở đó.
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(AppScreen.UpdateObservationScreen.route)
            }
            val observationViewModel: ObservationViewModel = hiltViewModel(parentEntry)

            MapPickerScreen(
                navController = navController,
                onLocationPicked = { lan, lon, name, displayName, address ->
                        observationViewModel.onEvent(ObservationEvent.OnLocationSelected(
                            lan = lan,
                            lon = lon,
                            name = name,
                            displayName = displayName,
                            address = address
                        ))
                }
            )
        }

        composable(
            route = AppScreen.ObservationDetailScreen.route,
            arguments = listOf(
                navArgument("observationId"){
                    type = NavType.StringType;
                    nullable = false
                }
            )
        ){backStackEntry ->
            val observationViewModel : ObservationDetailViewModel = hiltViewModel()
            val observationId = backStackEntry.arguments?.getString("observationId")
            ObservationDetailView(

                navController,
                observationId,
                observationViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreen.NotificationScreen.route,
        ){
            NotificationView(
                navController = navController,
                authViewModel = authViewModel
            )
        }

    }

}