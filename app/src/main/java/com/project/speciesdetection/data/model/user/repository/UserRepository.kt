package com.project.speciesdetection.data.model.user.repository

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.project.speciesdetection.data.model.user.User

interface UserRepository {
    fun createGoogleSignInRequest(): GetCredentialRequest
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser>
    suspend fun signUpWithEmailPassword(email: String, pass: String, name: String): Result<FirebaseUser>
    suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser>
    fun getCurrentUser(): FirebaseUser?
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
}