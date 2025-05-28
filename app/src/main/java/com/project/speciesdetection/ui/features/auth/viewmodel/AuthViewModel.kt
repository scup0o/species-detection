package com.project.speciesdetection.ui.features.auth.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseUser
import com.project.speciesdetection.data.model.user.repository.RemoteUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val currentUser: FirebaseUser? = null,
    val error: String? = null,
    val isGoogleSignInInProgress: Boolean = false,
    val resendCooldownSeconds: Int = 0
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: RemoteUserRepository,
    private val credentialManager: CredentialManager
) : AndroidViewModel(application) {

    private val _authState = MutableStateFlow(AuthState())
    val authState = _authState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _resendEmailState = MutableStateFlow(false)
    val resendEmailState = _resendEmailState.asStateFlow()

    private var googleSignInApiRequest: GetCredentialRequest? = null

    init {
        checkCurrentUser()
        prepareGoogleSignInRequestObject()
    }

    private fun checkCurrentUser() {
        _authState.update { it.copy(currentUser = repository.getCurrentUser()) }
    }

    private fun prepareGoogleSignInRequestObject() {
        googleSignInApiRequest = repository.createGoogleSignInRequest()
    }

    fun initiateGoogleSignIn(
        activityContext: Activity,
        errorMessage: String,
        successMessage : String) {
        val currentRequest = googleSignInApiRequest ?: run {
            _authState.update { it.copy(isLoading = false, error = "Google Sign-In request not ready.") }
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage)) }
            return
        }

        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {

                val result = credentialManager.getCredential(
                    request = currentRequest,
                    context = activityContext,
                )
                val credential = result.credential
                var idToken: String?

                if (credential is GoogleIdTokenCredential) {
                    idToken = credential.idToken
                } else if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        idToken = googleIdTokenCredential.idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        //Log.e("AuthViewModel", "GoogleIdTokenParsingException", e)
                        handleAuthError(Exception(errorMessage), "Google Sign-In Failed: ")
                        return@launch
                    }
                } else {
                    //Log.e("AuthViewModel", "Unexpected credential type: ${credential::class.java.simpleName}")
                    _authState.update { it.copy(isLoading = false, error = "Unexpected credential type.") }
                    _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage))
                    return@launch
                }

                if (idToken != null) {
                    repository.signInWithGoogleIdToken(idToken).fold(
                        onSuccess = { firebaseUser ->
                            _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                            _uiEvent.emit(UiEvent.ShowSnackbar(successMessage))
                        },
                        onFailure = { exception ->
                            handleAuthError(exception, errorMessage)
                        }
                    )
                } else {
                    handleAuthError(Exception(errorMessage), "Google Sign-In Failed: ")
                }

            } catch (e: GetCredentialException) {
                _authState.update { it.copy(isLoading = false) }
                if (e is GetCredentialCancellationException) {
                    //Log.i("AuthViewModel", "Google Sign-In cancelled by user.")
                    _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage))
                } else {
                    //Log.e("AuthViewModel", "GetCredentialException: ${e.message}", e)
                    _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage))
                }
            } catch (e: Exception) {
                handleAuthError(e, errorMessage)
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        name: String,
        errorMessage: String,
        successMessage : String) {
        if (name.isBlank()) {
            _authState.update { it.copy(error = "Name cannot be empty") }
            return
        }
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.signUpWithEmailPassword(email, pass, name).fold(
                onSuccess = { firebaseUser ->
                    startCooldown()
                    _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                    _resendEmailState.value = true
                    _uiEvent.emit(
                        UiEvent.ShowSnackbar("Sign up successful! A verification email has been sent.")
                    )
                },
                onFailure = { exception ->
                    handleAuthError(exception, errorMessage)
                }
            )
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        errorMessage: String,
        successMessage : String) {
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.signInWithEmailPassword(email, pass).fold(
                onSuccess = { firebaseUser ->
                    if (!firebaseUser.isEmailVerified) {
                        _authState.update { it.copy(isLoading = false, error = "Email not verified.") }
                        _uiEvent.emit(UiEvent.ShowSnackbar("Please verify your email before signing in."))
                        return@launch
                    }

                    _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                    _uiEvent.emit(UiEvent.ShowSnackbar(successMessage))
                },
                onFailure = { exception ->
                    handleAuthError(exception, errorMessage)
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            repository.signOut()
            _authState.update { AuthState() }
            //_uiEvent.emit(UiEvent.ShowSnackbar("Signed Out"))
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                //Log.i("AuthViewModel", "Credential state cleared on sign out.")
            } catch (e: ClearCredentialException) {
                //Log.e("AuthViewModel", "Failed to clear credential state: ${e.message}", e)
            }
        }
    }

    fun resetPassword(email: String) {
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (email.isBlank()) {
                _authState.update { it.copy(isLoading = false, error = "empty email") }
                _uiEvent.emit(UiEvent.ShowSnackbar("Please enter your email."))
                return@launch
            }

            val result = repository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = {
                    _authState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Password reset email sent. Please check your inbox."))
                },
                onFailure = { error ->
                    handleAuthError(error,"Error")
                    _uiEvent.emit(UiEvent.ShowSnackbar(error.message ?: "Failed to send reset email."))
                }
            )
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            if (authState.value.resendCooldownSeconds > 0) return@launch

            _authState.update { it.copy(isLoading = true) }

            val result = repository.resendVerificationEmail()
            result.fold(
                onSuccess = {
                    _authState.update { it.copy(isLoading = false) }
                    _resendEmailState.value = true
                    _uiEvent.emit(UiEvent.ShowSnackbar("Verification email has been resent. Please check your inbox."))

                    startCooldown()
                },
                onFailure = { e ->
                    _authState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(UiEvent.ShowSnackbar(e.message ?: "Failed to resend verification email."))
                }
            )
        }
    }

    private fun startCooldown() {
        val cooldown = 60 // thời gian chờ (giây)
        viewModelScope.launch {
            for (i in cooldown downTo 0) {
                _authState.update { it.copy(resendCooldownSeconds = i) }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    private suspend fun handleAuthError(exception: Throwable, prefix: String) {
        Log.e("AuthViewModel", "$prefix ${exception.message}", exception)
        /*val errorMessage = when (exception.message) {
            "Email_already_registered_with_password" -> "This email is registered with email/password. Please sign in with your password."
            "Email_already_registered_with_google_or_password" -> "This email is already in use with Google or another password account. Please try logging in or use a different email."
            else -> exception.localizedMessage ?: "An unknown error occurred"
        }*/
        _authState.update { it.copy(isLoading = false, error = exception.message) }
        _uiEvent.emit(UiEvent.ShowSnackbar(exception.message!!))
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
        _resendEmailState.value = false
    }
}