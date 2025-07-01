package com.project.speciesdetection.ui.features.auth.view

import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.composable.common.setting.SettingItem
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel

@Composable
fun LogoutButton(
    authViewModel: AuthViewModel,
    onClick : () -> Unit,
){
    SettingItem(
        onClickAction = {
            authViewModel.signOut()
            onClick()},
        title = R.string.log_out,
        painterIcon = R.drawable.log_out
    )
}