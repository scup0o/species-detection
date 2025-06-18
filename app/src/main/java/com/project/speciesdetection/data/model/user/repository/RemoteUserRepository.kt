package com.project.speciesdetection.data.model.user.repository

import android.util.Log
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
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

    override fun addCurrentUserFcmToken(userId: String, token: String) {
        firestore.collection("users").document(userId)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .addOnSuccessListener { Log.d("FCM", "FCM Token added/updated successfully.") }
            .addOnFailureListener { e -> Log.w("FCM", "Error adding/updating FCM Token", e) }
    }

    override fun removeCurrentUserFcmToken(userId: String, token: String, onComplete: () -> Unit) {
        firestore.collection("users").document(userId)
            .update("fcmTokens", FieldValue.arrayRemove(token))
            .addOnCompleteListener {
                Log.d("FCM", "Attempted to remove FCM Token.")
                onComplete() // Luôn gọi callback để tiếp tục quá trình logout
            }
    }


}