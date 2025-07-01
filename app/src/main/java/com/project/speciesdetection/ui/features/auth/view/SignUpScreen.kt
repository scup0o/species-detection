package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
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
fun SignupScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activity = LocalActivity.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val resendEmailState by authViewModel.resendEmailState.collectAsStateWithLifecycle()

    val successMessage = stringResource(R.string.signup_success)
    val errorMessage = stringResource(R.string.signup_error)

    LaunchedEffect(true) {
        authViewModel.clearError()
        authViewModel.uiEvent.collectLatest {
            if (it is UiEvent.ShowSnackbar)
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    /*LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
                navController.popBackStack(/*AppScreen.CommunityScreen.route, inclusive = true, saveState = false*/) // Quan trọng: saveState = false
                /*navController.navigate(AppScreen.CommunityScreen.route) {
                    launchSingleTop = true
                }*/


            /*navController.navigate(AppScreen.CommunityScreen.route) {
                popUpTo(0) // clear stack
            }*/
        }
    }*/

    if (authState.isLoading) {
        CircularProgressIndicatorScrim()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        authViewModel.clearError()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
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
                    stringResource(R.string.signup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Text(
                    stringResource(R.string.signup_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuthTextField(
                        label = stringResource(R.string.name_label),
                        value = name,
                        onValueChange = { name = it },
                        error = authState.error,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.padding(horizontal = 15.dp)
                            )
                        }
                    )

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

                    PasswordField(
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        error = authState.error,

                        )

                    if (resendEmailState != "none") {
                        if (resendEmailState == "success") {
                            Text(
                                stringResource(R.string.send_verification_email_success),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { authViewModel.resendVerificationEmail() },
                                enabled = !authState.isLoading && authState.resendCooldownSeconds == 0
                            ) {
                                Text(
                                    "Not seeing any email? Try ",
                                    color = MaterialTheme.colorScheme.outline.copy(0.6f)
                                )
                                Text(
                                    if (authState.resendCooldownSeconds > 0)
                                        "Resend in ${authState.resendCooldownSeconds}s"
                                    else
                                        "Resend verification email",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Text(
                                stringResource(R.string.error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                    }
                }


                CustomActionButton(
                    text = stringResource(R.string.signup_with_email),
                    onClick = {
                        keyboardController?.hide()
                        authViewModel.clearError()
                        authViewModel.signUpWithEmail(
                            email = email,
                            pass = password,
                            name = name,
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
                            AppScreen.LoginScreen.route,
                            inclusive = true,
                            saveState = false
                        )
                        navController.navigate(AppScreen.LoginScreen.route) {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.have_account))
                    Text(
                        stringResource(R.string.login_title),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

            }
        }
    }

}