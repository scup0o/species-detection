package com.project.speciesdetection.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreen(
    val route: String,
    val icon: ImageVector?,
) {
    object CameraScreen : AppScreen(
        route = "camera_screen",
        icon = null)
    object EncyclopediaMainScreen : AppScreen(
        route = "encyclopedia_main_screen",
        icon = Icons.Filled.AccountBox)
    object CommunityScreen : AppScreen(
        route = "community_screen",
        icon = Icons.Filled.Home)
    object ProfileMainScreen : AppScreen(
        route = "profile_main_screen",
        icon = Icons.Filled.Person)
    object SettingMainScreen : AppScreen(
        route = "setting_main_screen",
        icon = Icons.Filled.Settings)
}