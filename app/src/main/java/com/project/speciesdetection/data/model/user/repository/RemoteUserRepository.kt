package com.project.speciesdetection.data.model.user.repository

import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speciesdetection.core.services.authentication.AuthServiceInterface
import com.project.speciesdetection.core.services.authentication.FirebaseAuthService
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RemoteUserRepository @Inject constructor(
    private val authService: AuthServiceInterface,
    private val firestore: FirebaseFirestore
) : UserRepository{
    override fun createGoogleSignInRequest(): GetCredentialRequest {
        return authService.createGoogleSignInRequest()
    }
    override suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        return authService.signInWithGoogleIdToken(idToken)
    }

    override suspend fun signUpWithEmailPassword(email: String, pass: String, name: String): Result<FirebaseUser> {
        return authService.signUpWithEmailPassword(email, pass, name)
    }

    override suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser> {
        return authService.signInWithEmailPassword(email, pass)
    }

    override fun getCurrentUser(): FirebaseUser? = authService.getCurrentUser()
    override suspend fun getUserInformation(uid: String): User? {
        return try {
            val documentSnapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            documentSnapshot.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun signOut() = authService.signOut()
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authService.sendPasswordResetEmail(email)
    }
    override suspend fun resendVerificationEmail(): Result<Unit> {
        return authService.resendVerificationEmail()
    }


}