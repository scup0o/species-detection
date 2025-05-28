package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.ErrorText
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

    LaunchedEffect(true) {
        authViewModel.uiEvent.collectLatest {
            if (it is UiEvent.ShowSnackbar)
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
            if (authState.currentUser!!.isEmailVerified){
                navController.popBackStack(AppScreen.CommunityScreen.route, inclusive = true, saveState = false) // Quan trọng: saveState = false
                navController.navigate(AppScreen.CommunityScreen.route) {
                    launchSingleTop = true
                }
            }

            /*navController.navigate(AppScreen.CommunityScreen.route) {
                popUpTo(0) // clear stack
            }*/
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AuthTextField(
                label = "Name",
                value = name,
                onValueChange = { name = it },
                error = authState.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            AuthTextField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                error = authState.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            PasswordField(
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                error = authState.error,

            )



            Spacer(modifier = Modifier.height(16.dp))

            if (resendEmailState)
                TextButton(
                    onClick = { authViewModel.resendVerificationEmail() },
                    enabled = !authState.isLoading && authState.resendCooldownSeconds == 0
                ) {
                    Text(
                        if (authState.resendCooldownSeconds > 0)
                            "Resend in ${authState.resendCooldownSeconds}s"
                        else
                            "Resend verification email"
                    )
                }

            CustomActionButton(
                text = "Sign Up with Email",
                isLoading = authState.isLoading,
                onClick = {
                    keyboardController?.hide()
                    authViewModel.clearError()
                    authViewModel.signUpWithEmail(
                        email = email,
                        pass = password,
                        name = name,
                        successMessage = "Sign-Up Successful",
                        errorMessage = "Sign-Up Failed: "
                    )

                }
            )
        }


            Spacer(modifier = Modifier.height(8.dp))

            CustomActionButton(
                text = "Sign Up with Google",
                isLoading = false,
                onClick = {
                    authViewModel.clearError()
                    activity?.let {
                        authViewModel.initiateGoogleSignIn(
                            it,
                            errorMessage = "Google sign-up failed",
                            successMessage = "Google sign-up success"
                        )
                    } ?: Toast.makeText(context, "Cannot get Activity", Toast.LENGTH_LONG).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    authViewModel.clearError()
                    navController.popBackStack(AppScreen.LoginScreen.route, inclusive = true, saveState = false)
                    navController.navigate(AppScreen.LoginScreen.route) {
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Already have an account? Login")
            }
        }

}