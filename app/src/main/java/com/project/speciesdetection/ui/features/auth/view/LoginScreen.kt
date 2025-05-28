package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.core.theme.spacing
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
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect (true) {
        authViewModel.clearError()
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
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
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

            Text(
                "Forgot Password?",
                modifier = Modifier
                    .clickable {
                        authViewModel.clearError()
                        navController.popBackStack(AppScreen.ForgotPasswordScreen.route, inclusive = true, saveState = false)
                        navController.navigate(AppScreen.ForgotPasswordScreen.route) {
                            launchSingleTop = true
                        }
                    }
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.s)
                ,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            CustomActionButton(
                text = "Login with Email",
                isLoading = authState.isLoading,
                onClick = {
                    keyboardController?.hide()
                    authViewModel.clearError()
                    authViewModel.signInWithEmail(
                        email, password,
                        successMessage = "Login successful",
                        errorMessage = "Login failed"
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            CustomActionButton(
                text = "Sign in with Google",
                isLoading = false,
                onClick = {
                    authViewModel.clearError()
                    activity?.let {
                        authViewModel.initiateGoogleSignIn(it, "Google sign-in failed", "Success")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    authViewModel.clearError()
                    navController.popBackStack(AppScreen.SignUpScreen.route, inclusive = true, saveState = false)
                    navController.navigate(AppScreen.SignUpScreen.route) {
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Need an account? Sign Up", textAlign = TextAlign.Center)
            }
        }
    }
}