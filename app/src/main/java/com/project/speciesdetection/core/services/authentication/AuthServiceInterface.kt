package com.project.speciesdetection.core.services.authentication

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.project.speciesdetection.data.model.user.User

interface AuthServiceInterface {
    fun createGoogleSignInRequest(): GetCredentialRequest // Service chỉ tạo request
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> // Service nhận idToken
    suspend fun signUpWithEmailPassword(email: String, pass: String, name: String): Result<FirebaseUser>
    suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser>
    suspend fun saveUserProfile(user: User): Result<Unit>
    fun getCurrentUser(): FirebaseUser?
    suspend fun signOut()
}