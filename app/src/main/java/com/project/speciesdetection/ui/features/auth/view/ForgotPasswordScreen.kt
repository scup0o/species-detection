package com.project.speciesdetection.ui.features.auth.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.auth.AuthTextField
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var email by remember { mutableStateOf("") }

    val forgotPasswordState by authViewModel.forgotPasswordState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        authViewModel.clearError()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_password_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(15.dp)
                .fillMaxSize()
        ) {
            AuthTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                error = authState.error
            )

            if (forgotPasswordState!="none"){
                if (forgotPasswordState=="success")
                    Text(
                        stringResource(R.string.send_reset_password_email_successful),
                        color= MaterialTheme.colorScheme.primary,
                        style=MaterialTheme.typography.bodyMedium
                    )
                else
                    Text(
                        stringResource(R.string.error),
                        color = MaterialTheme.colorScheme.error,
                        style=MaterialTheme.typography.bodyMedium
                    )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomActionButton(
                text =
                    if (authState.forgotPasswordCooldownSeconds > 0)
                    stringResource(R.string.resend_verification_email) +" ${authState.forgotPasswordCooldownSeconds}s"
                    else
                    stringResource(R.string.send_recovery_password),
                isLoading = authState.isLoading,
                onClick = {
                    keyboardController?.hide()
                    authViewModel.clearError()
                    authViewModel.resetPassword(email)
                },
                enabled = !authState.isLoading && authState.forgotPasswordCooldownSeconds == 0
            )


        }
    }
}