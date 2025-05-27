package com.project.speciesdetection.ui.features.login.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
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
    val isGoogleSignInInProgress: Boolean = false // Bạn có thể giữ lại hoặc loại bỏ nếu không dùng nữa
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

    fun initiateGoogleSignIn(activityContext: Activity) {
        val currentRequest = googleSignInApiRequest ?: run {
            _authState.update { it.copy(isLoading = false, error = "Google Sign-In request not ready.") }
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar("Google Sign-In request not ready.")) }
            return
        }

        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {

                val result = credentialManager.getCredential(
                    request = currentRequest,
                    context = activityContext,
                )
                // Xử lý credential thành công (như code hiện tại của bạn)
                val credential = result.credential
                var idToken: String? = null

                if (credential is GoogleIdTokenCredential) {
                    idToken = credential.idToken
                } else if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        idToken = googleIdTokenCredential.idToken
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("AuthViewModel", "GoogleIdTokenParsingException", e)
                        handleAuthError(Exception("Failed to parse Google ID token."), "Google Sign-In Failed: ")
                        return@launch
                    }
                } else {
                    Log.e("AuthViewModel", "Unexpected credential type: ${credential::class.java.simpleName}")
                    _authState.update { it.copy(isLoading = false, error = "Unexpected credential type.") }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Unexpected credential type."))
                    return@launch
                }

                if (idToken != null) {
                    repository.signInWithGoogleIdToken(idToken).fold(
                        onSuccess = { firebaseUser ->
                            _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                            _uiEvent.emit(UiEvent.ShowSnackbar("Google Sign-In Successful"))
                        },
                        onFailure = { exception ->
                            handleAuthError(exception, "Google Sign-In (Firebase) Failed: ")
                        }
                    )
                } else {
                    handleAuthError(Exception("Google ID Token was null."), "Google Sign-In Failed: ")
                }

            } catch (e: GetCredentialException) { // Bắt lỗi chung từ Credential Manager
                _authState.update { it.copy(isLoading = false) }
                if (e is GetCredentialCancellationException) {
                    Log.i("AuthViewModel", "Google Sign-In cancelled by user.")
                    _uiEvent.emit(UiEvent.ShowSnackbar("Google Sign-In cancelled."))
                } else {
                    // Các lỗi khác từ GetCredentialException (có thể bao gồm cả NoCredentialException,
                    // nhưng CredentialManager sẽ tự xử lý UI cho trường hợp không có tài khoản).
                    // Thường thì khi đến đây, có thể là lỗi mạng hoặc cấu hình sai.
                    Log.e("AuthViewModel", "GetCredentialException: ${e.message}", e)
                    _uiEvent.emit(UiEvent.ShowSnackbar("Google Sign-In failed. Please try again."))
                }
            } catch (e: Exception) { // Bắt các lỗi khác không mong muốn
                handleAuthError(e, "Google Sign-In (General) Failed: ")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        if (name.isBlank()) {
            _authState.update { it.copy(error = "Name cannot be empty") }
            return
        }
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.signUpWithEmailPassword(email, pass, name).fold(
                onSuccess = { firebaseUser ->
                    _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Sign-Up Successful"))
                },
                onFailure = { exception ->
                    handleAuthError(exception, "Sign-Up Failed: ")
                }
            )
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.signInWithEmailPassword(email, pass).fold(
                onSuccess = { firebaseUser ->
                    _authState.update { it.copy(isLoading = false, currentUser = firebaseUser, error = null) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Sign-In Successful"))
                },
                onFailure = { exception ->
                    handleAuthError(exception, "Sign-In Failed: ")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _authState.update { AuthState() }
            _uiEvent.emit(UiEvent.ShowSnackbar("Signed Out"))
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.i("AuthViewModel", "Credential state cleared on sign out.")
            } catch (e: ClearCredentialException) {
                Log.e("AuthViewModel", "Failed to clear credential state: ${e.message}", e)
            }
        }
    }

    private suspend fun handleAuthError(exception: Throwable, prefix: String) {
        Log.e("AuthViewModel", "$prefix ${exception.message}", exception)
        val errorMessage = when (exception.message) {
            "Email_already_registered_with_password" -> "This email is registered with email/password. Please sign in with your password."
            "Email_already_registered_with_google_or_password" -> "This email is already in use with Google or another password account. Please try logging in or use a different email."
            else -> exception.localizedMessage ?: "An unknown error occurred"
        }
        _authState.update { it.copy(isLoading = false, error = errorMessage) }
        _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage))
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}