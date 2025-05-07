package com.project.speciesdetection.core.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(){
    val navBarItemColors = NavigationBarItemColors(
        selectedIndicatorColor = Color.Transparent,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
        unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
        disabledIconColor = MaterialTheme.colorScheme.outlineVariant,
        disabledTextColor = MaterialTheme.colorScheme.outlineVariant)

    Row{
        FloatingActionButton(
            onClick = {},
            shape = CircleShape,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(Icons.Filled.Add, null)
        }
        NavigationBar(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(
                    topStart = 25.dp,
                    topEnd = 25.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )))
        {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = {
                    Icon(Icons.Filled.AccountBox,null)
                },
                colors = navBarItemColors,
                alwaysShowLabel = false,
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(Icons.Filled.Home,null)
                },
                colors = navBarItemColors,
                alwaysShowLabel = false,
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(Icons.Filled.Person,null)
                },
                colors = navBarItemColors,
                alwaysShowLabel = false,
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(Icons.Filled.Settings,null)
                },
                colors = navBarItemColors,
                alwaysShowLabel = false,
            )
        }
    }

}