package com.project.speciesdetection.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.project.speciesdetection.R

sealed class AppScreen(
    val route: String,
    val icon: Int?,
) {
    object CameraScreen : AppScreen(
        route = "camera_screen",
        icon = R.drawable.camera
    )
    object EditImageForIdentificationScreen : AppScreen(
        route = "editScreen/{imageUri}",
        icon = null
    )
    object EncyclopediaMainScreen : AppScreen(
        route = "encyclopedia_main_screen",
        icon = R.drawable.book_open)
    object CommunityScreen : AppScreen(
        route = "community_screen",
        icon = R.drawable.message_circle)
    object ProfileMainScreen : AppScreen(
        route = "profile_main_screen",
        icon = R.drawable.user)
    object SettingMainScreen : AppScreen(
        route = "setting_main_screen",
        icon = R.drawable.setting)
}