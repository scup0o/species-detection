package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.ui.composable.common.CircularProgressIndicatorScrim
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.Divider
import com.project.speciesdetection.ui.composable.common.auth.AuthTextField
import com.project.speciesdetection.ui.composable.common.auth.PasswordField
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.auth.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activity = LocalActivity.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val successMessage = stringResource(R.string.login_success)
    val errorMessage = stringResource(R.string.login_error)

    var passwordVisible by remember { mutableStateOf(false) }
    var showAccountNotVerifyDiaglog by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        authViewModel.clearError()
        authViewModel.uiEvent.collectLatest {
            if (it is UiEvent.ShowSnackbar)
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(authState.error) {
        if (authState.error.equals("Email not verified.")){
            showAccountNotVerifyDiaglog = true
        }
    }

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
            navController.popBackStack(
                /*AppScreen.CommunityScreen.route,
                inclusive = true,
                saveState = false*/
            )
            /*navController.navigate(AppScreen.CommunityScreen.route) {
                launchSingleTop = true
            }*/


            /*navController.navigate(AppScreen.CommunityScreen.route) {
                popUpTo(0) // clear stack
            }*/
        }
    }

    if (authState.isLoading){
        CircularProgressIndicatorScrim()
    }

    if (authState.error.equals("Email not verified.")){
        AlertDialog(
            icon = {
                Icon(Icons.Default.Info, null)
            },
            title = {
                Text(
                    stringResource(R.string.unverified_account),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()

                )
            },
            text={
              Text(
                  stringResource(R.string.unverified_account_text)
              )
            },
            onDismissRequest = {authViewModel.clearError()},
            confirmButton = {
                TextButton(
                    onClick = {authViewModel.resendVerificationEmail()},
                    enabled = !authState.isLoading && authState.resendCooldownSeconds == 0
                ) {
                    Text(
                        if (authState.resendCooldownSeconds > 0)
                            stringResource(R.string.is_resend_verification_email_state)+" ${authState.resendCooldownSeconds}s"
                        else
                            stringResource(R.string.resend_verification_email)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {authViewModel.clearError()}
                ) {
                    Text(
                        stringResource(R.string.dismiss)
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .padding(15.dp)
                .fillMaxSize(),
        ) {


            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Text(
                    stringResource(R.string.login_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center).padding(top=20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column {
                    AuthTextField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        error = authState.error,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, null,
                                modifier = Modifier.padding(horizontal = 15.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PasswordField(
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        error = authState.error,
                    )
                    Text(
                        stringResource(R.string.forgot_password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                authViewModel.clearError()
                                navController.popBackStack(
                                    AppScreen.ForgotPasswordScreen.route,
                                    inclusive = true,
                                    saveState = false
                                )
                                navController.navigate(AppScreen.ForgotPasswordScreen.route) {
                                    launchSingleTop = true
                                }
                            },
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                }

                CustomActionButton(
                    text = stringResource(R.string.login_with_email),
                    onClick = {
                        keyboardController?.hide()
                        authViewModel.clearError()
                        authViewModel.signInWithEmail(
                            email, password,
                            successMessage = successMessage,
                            errorMessage = errorMessage
                        )
                    }
                )

                Divider()

                CustomActionButton(
                    text = stringResource(R.string.login_with_google),
                    onClick = {
                        authViewModel.clearError()
                        activity?.let {
                            authViewModel.initiateGoogleSignIn(
                                it,
                                errorMessage,
                                successMessage
                            )
                        }
                    },
                    painterLeadingIcon = R.drawable.google_icon_logo,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )


                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        authViewModel.clearError()
                        navController.popBackStack(
                            AppScreen.SignUpScreen.route,
                            inclusive = true,
                            saveState = false
                        )
                        navController.navigate(AppScreen.SignUpScreen.route) {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.need_an_account),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.signup_title),
                        fontWeight = FontWeight.ExtraBold

                    )
                }
            }


        }
    }
}