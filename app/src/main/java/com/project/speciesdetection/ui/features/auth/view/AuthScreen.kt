package com.project.speciesdetection.ui.features.auth.view

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.project.speciesdetection.core.navigation.BottomNavigationBar
import com.project.speciesdetection.ui.composable.common.ErrorText
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.auth.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavHostController,
    viewModel: AuthViewModel
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = LocalActivity.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isForgotPassword) "Password Recovery"
                        else{
                            if (isLoginMode) "Login"
                            else "Sign Up"
                        }
                         )},
                navigationIcon = {
                    if (isForgotPassword)
                        IconButton(
                            onClick = {isForgotPassword = false}
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack, null
                            )
                        }
                })
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (!isLoginMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = authState.error == "Name cannot be empty"
                )
                if (authState.error == "Name cannot be empty")
                    ErrorText("Name cannot be empty")
                Spacer(modifier = Modifier.height(8.dp))

            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError =
                    if (authState.error!=null){
                        if (authState.error!!.contains("empty")
                            && email=="") true
                        else {
                            authState.error!!.contains("formatted")
                                    || authState.error!!.contains("incorrect")

                        }
                    }
                    else false
            )
            if (authState.error!=null){
                if (authState.error!!.contains("empty") && email=="")
                    ErrorText("Email cannot be empty")
                if ((authState.error!!.contains("formatted")))
                    ErrorText("Email is badly formatted")
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (!isForgotPassword)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Default.PlayArrow
                        else Icons.Default.Build

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError =
                        if (authState.error!=null){
                            if (authState.error!!.contains("empty") &&
                                password==""
                            ) true
                            else {
                                authState.error!!.contains("incorrect")
                            }
                        }
                        else false
                )

            if (authState.error!=null){
                if (authState.error!!.contains("empty") &&
                    password=="" && !isForgotPassword
                )
                    ErrorText("Password can not be empty")
                if (authState.error!!.contains("incorrect"))
                    ErrorText("Invalid Email or Password ")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (authState.error == "Email_already_registered_with_password"
                || authState.error == "Email_already_registered_with_google_or_password"){
                ErrorText(
                    "This email is already in use with another account. Please try logging in or use a different email."
                )
            }
            if (!isForgotPassword && isLoginMode) {
                TextButton(
                    onClick = {
                        isForgotPassword = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?")
                }
            }

            if (authState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                }

            } else {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.clearError()
                        if (isForgotPassword){
                            viewModel.resetPassword(email)
                        }
                        else{
                            if (isLoginMode) {
                                viewModel.signInWithEmail(
                                    email,
                                    password,
                                    successMessage = "Sign-In Successful",
                                    errorMessage = "Sign-In Failed: "
                                )
                            } else {
                                viewModel.signUpWithEmail(
                                    email,
                                    password,
                                    name,
                                    successMessage = "Sign-Up Successful",
                                    errorMessage = "Sign-Up Failed: "
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isForgotPassword) "Send Email"
                        else{
                            if (isLoginMode)
                                "Login with Email"
                            else "Sign Up with Email"
                        }
                        )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.clearError()
                        if (activity != null) {
                            viewModel.initiateGoogleSignIn(
                                activity,
                                errorMessage = "Google Sign-In failed. Please try again.",
                                successMessage = "Google Sign-In success")
                        } else {
                            Toast.makeText(context, "Cannot get Activity context for Google Sign-In.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In with Google")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    isForgotPassword = false
                    isLoginMode = !isLoginMode
                    viewModel.clearError()}) {
                Text(
                    if (isLoginMode) "Need an account? Sign Up"
                    else "Have an account? Login",
                    textAlign = TextAlign.Center)
            }

        }
    }
}