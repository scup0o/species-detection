package com.project.speciesdetection.core.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.project.speciesdetection.R
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class AppScreen(
    val route: String,
    val icon: Int? = null,
    val agruments: List<String>? = emptyList()
) {
    /*object CameraScreen : AppScreen(
        route = "camera_screen",
        icon = R.drawable.camera
    )*/
    val json = Json { ignoreUnknownKeys = true }

    object EditImageForIdentificationScreen : AppScreen(
        route = "edit_image_screen/{imageUri}",
        icon = null
    ) {
        fun createRoute(imageUri: Uri): String {
            val encodedUri = Uri.encode(imageUri.toString())
            return "edit_image_screen/$encodedUri"
        }
    }

    object EncyclopediaMainScreen : AppScreen(
        route = "encyclopedia_main_screen",
        icon = R.drawable.book_open
    )

    object EncyclopediaDetailScreen : AppScreen(
        route = "encyclopedia_detail_screen/{baseSpeciesJson}?imageURi={imageUri}",
        icon = null
    ) {
        fun createRoute(species: DisplayableSpecies, imageUri: Uri?): String {
            val speciesJsonString = json.encodeToString(species)
            val speciesJsonEncoded = Uri.encode(speciesJsonString)

            var route = "encyclopedia_detail_screen/$speciesJsonEncoded"

            imageUri?.let {
                val encodedImageUri = Uri.encode(it.toString())
                route += "?imageURi=$encodedImageUri"
            }
            return route
        }
    }

    object LoginScreen : AppScreen(
        route = "login_screen",
        icon = null,
    )

    object SignUpScreen : AppScreen(
        route = "signup_screen",
        icon = null,
    )

    object ForgotPasswordScreen : AppScreen(
        route = "forgot_password_screen",
        icon = null,
    )

    object CommunityScreen : AppScreen(
        route = "community_screen",
        icon = R.drawable.message_circle
    )

    object ProfileMainScreen : AppScreen(
        route = "profile_main_screen",
        icon = R.drawable.user
    )

    object SettingMainScreen : AppScreen(
        route = "setting_main_screen",
        icon = R.drawable.setting
    )

    object FullScreenImageViewer : AppScreen(
        route = "fullscreen_image_viewer/{imageUri}",
        icon = null
    ) {
        fun createRoute(imageUri: Any): String {
            val encodedUri = Uri.encode(imageUri.toString())
            return "fullscreen_image_viewer/$encodedUri"
        }

    }
    object UpdateObservationScreen : AppScreen("update_observation_screen?speciesId={speciesId}&speciesName={speciesName}&speciesSN={speciesSN}&imageUri={imageUri}&observationJson={observationJson}") {
        // Hàm helper để tạo route khi tạo mới từ Species và Image
        fun buildRouteForCreate(species: DisplayableSpecies?, imageUri: Uri?): String {
            val params = mutableListOf<String>()
            species?.let {
                params.add("speciesId=${it.id}")
                params.add("speciesName=${Uri.encode(it.localizedName)}")
                params.add("speciesSN=${Uri.encode(it.getScientificName())}")
            }
            imageUri?.let {
                params.add("imageUri=${Uri.encode(it.toString())}")
            }
            return if (params.isNotEmpty()) "update_observation_screen?${params.joinToString("&")}" else "update_observation_screen"
        }

        // Hàm helper để tạo route khi chỉnh sửa Observation đã có
        fun buildRouteForEdit(observationJson: String): String {
            return "update_observation_screen?observationJson=${Uri.encode(observationJson)}"
        }
    }

    object MapPickerScreen : AppScreen(
        route = "map_picker"
    )
}