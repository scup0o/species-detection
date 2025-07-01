package com.project.speciesdetection.ui.features.setting_main_screen.view

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.R
import com.project.speciesdetection.app.MainActivity
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.setting.SettingItem
import com.project.speciesdetection.ui.features.auth.view.ChangePasswordBottomSheet
import com.project.speciesdetection.ui.features.auth.view.LogoutButton
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.setting_main_screen.viewmodel.SettingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingMainScreen(
    navController : NavHostController,
    settingViewModel: SettingViewModel,
    authViewModel : AuthViewModel,
    containerColor : Color = MaterialTheme.colorScheme.surfaceContainer.copy(0.8f)
){
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val showLanguagePicker by settingViewModel.showLanguagePicker.collectAsStateWithLifecycle()
    var showDataManagementDialog by remember { mutableStateOf(false) }

    if (showDataManagementDialog) {
        DataManagementDialog(
            onDismissRequest = { showDataManagementDialog = false }
        )
    }

    var resetState = false
    /*BackHandler(enabled = true) { // Luôn bật để bắt sự kiện
        if (resetState) {
            val intent = Intent(context, MainActivity::class.java) // Hoặc (context as Activity).javaClass
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            (context as? Activity)?.startActivity(intent)
            (context as? Activity)?.finishAffinity()
        } else {
            navController.popBackStack()
        }
    }*/

    var showChangePasswordSheet by remember { mutableStateOf(false) }


    if (showChangePasswordSheet) {
        ChangePasswordBottomSheet(
            onDismissRequest = { showChangePasswordSheet = false }
        )
    }

    if (showLanguagePicker)
        LanguagePickerDialog(
            viewModel = settingViewModel,
            onBackPressed = {resetState = it}
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.setting))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {if (resetState) {
                            val intent = Intent(context, MainActivity::class.java) // Hoặc (context as Activity).javaClass
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            (context as? Activity)?.startActivity(intent)
                            (context as? Activity)?.finishAffinity()
                        } else {
                            navController.popBackStack()
                        }}
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
            modifier = Modifier.padding(innerPadding).padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Box(Modifier.background(MaterialTheme.colorScheme.background,RoundedCornerShape(10.dp)).padding(vertical = 10.dp)){
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingItem(
                        onClickAction = {settingViewModel.onOpenLanguagePicker()},
                        title = R.string.language,
                        painterIcon = R.drawable.language
                    )
                    Row(Modifier.padding(horizontal = 20.dp)){
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                    }
                    SettingItem(
                        onClickAction = {showDataManagementDialog = true},
                        title = R.string.download_content,
                        painterIcon = R.drawable.package_alt
                    )
                }
            }

            if (authState.currentUser != null){
                Box(Modifier.background(MaterialTheme.colorScheme.background,RoundedCornerShape(10.dp)).padding(vertical = 10.dp)){
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (authState.currentUserInformation?.source != "google.com") {
                    SettingItem(
                        onClickAction = { showChangePasswordSheet = true },
                        title = R.string.change_password,
                        vectorIcon = Icons.Default.Lock
                    )
                    Row(Modifier.padding(horizontal = 20.dp)){
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                    }
                }
                LogoutButton(
                    authViewModel = authViewModel,
                    onClick = {
                        Toast.makeText(
                            context,
                            R.string.log_out,
                            Toast.LENGTH_LONG).show()
                    })}}
            }

        }
    }
}