package com.project.speciesdetection.ui.features.setting_main_screen.view

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.setting.SettingItem
import com.project.speciesdetection.ui.features.auth.view.LogoutButton
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.setting_main_screen.viewmodel.SettingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingMainScreen(
    navController : NavHostController,
    settingViewModel: SettingViewModel,
    authViewModel : AuthViewModel,
    containerColor : Color = MaterialTheme.colorScheme.surfaceContainer
){
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLanguagePicker by remember { mutableStateOf(false) }

    if (showLanguagePicker)
        LanguagePickerDialog(
            onDismissRequest = {showLanguagePicker = false},
            viewModel = settingViewModel
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {navController.popBackStack()}
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = containerColor
    ){
        innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SettingItem(
                onClickAction = {showLanguagePicker = true},
                title = R.string.language,
                painterIcon = R.drawable.globe_solid
            )
            if (authState.currentUser != null)
                LogoutButton(
                    authViewModel = authViewModel,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Logout",
                            Toast.LENGTH_LONG).show()
                    })
        }
    }
}