package com.project.speciesdetection.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.features.identification_image_source.view.ImageSourceSelectionBottomSheet

@Composable
fun BottomNavigationBar(
    navController : NavHostController
){
    val screens = listOf(
        AppScreen.EncyclopediaMainScreen,
        AppScreen.CommunityScreen,
        AppScreen.ProfileMainScreen,
        AppScreen.SettingMainScreen
    )
    val navBarItemColors = NavigationBarItemColors(
        selectedIndicatorColor = Color.Transparent,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.inversePrimary,
        unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
        disabledIconColor = MaterialTheme.colorScheme.outlineVariant,
        disabledTextColor = MaterialTheme.colorScheme.outlineVariant)

    var showImageSourcePicker by remember { mutableStateOf(false) }

    ImageSourceSelectionBottomSheet(
        showBottomSheet = showImageSourcePicker,
        onDismissRequest = {
            showImageSourcePicker = false
        },
        onImageSelected = { uri ->
            navController.popBackStack(AppScreen.EditImageForIdentificationScreen.createRoute(uri), inclusive = true, saveState = false)
            navController.navigate(AppScreen.EditImageForIdentificationScreen.createRoute(uri)) {
                launchSingleTop = true
            }
        }
    )

    Row(
    ) {
        FloatingActionButton(
            onClick = {
                showImageSourcePicker = true
            },
            shape = CircleShape,
            modifier = Modifier.padding(10.dp).size(60.dp),
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                painterResource(R.drawable.identify_icon),
                null,
                Modifier.size(30.dp))
        }
        NavigationBar(
            modifier = Modifier
                .clip(
                    shape = RoundedCornerShape(
                        topStart = 25.dp,
                        topEnd = 25.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                ).height(80.dp)
                ,
            containerColor = MaterialTheme.colorScheme.surface
        )
        {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            screens.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItem(
                    modifier = Modifier,
                    icon = {
                        Icon(
                            painter = painterResource(screen.icon!!),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)) },
                    colors = navBarItemColors,
                    alwaysShowLabel = false,
                    selected = selected,
                    onClick = {
                        if (selected) {
                            // Người dùng nhấn vào tab đang được chọn -> làm mới tab đó
                            // Pop màn hình hiện tại và navigate lại, không lưu state để nó tạo mới
                            navController.popBackStack(screen.route, inclusive = true, saveState = false) // Quan trọng: saveState = false
                            navController.navigate(screen.route) {
                                // Không cần popUpTo vì chúng ta đã pop màn hình trước đó
                                launchSingleTop = true // Vẫn giữ để tránh tạo nhiều instance nếu có lỗi logic
                                // Không cần restoreState vì mục tiêu là tạo mới
                            }
                        } else {
                            // Người dùng nhấn vào tab khác -> điều hướng như bình thường với save/restore state
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}