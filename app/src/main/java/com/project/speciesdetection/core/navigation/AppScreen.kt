package com.project.speciesdetection.core.navigation

sealed class AppScreen(
    val route: String
) {
    object CameraScreen : AppScreen(route = "camera_screen")
    object EncyclopediaScreen : AppScreen(route = "encyclopedia_screen")
    object CommunityScreen : AppScreen(route = "community_screen")
    object ProfileScreen : AppScreen(route = "profile_screen")
    object SettingScreen : AppScreen(route = "setting_screen")
}