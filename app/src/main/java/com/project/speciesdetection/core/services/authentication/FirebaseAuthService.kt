// data/service/FirestoreAuthService.kt
package com.project.speciesdetection.core.services.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class FirebaseAuthService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthServiceInterface {

    private val webClientId = "1012000261168-h55p7b5hpoe3ub4v7ovoa0s2361j8qon.apps.googleusercontent.com" // Chỉ cần webClientId để tạo GetGoogleIdOption


    override fun createGoogleSignInRequest(): GetCredentialRequest {
        /*val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Cho phép chọn từ tất cả tài khoản Google
            .setServerClientId(webClientId)
            // .setAutoSelectEnabled(true) // Tùy chọn: cố gắng tự động đăng nhập nếu có thể
            .build()*/

        val googleIdOption : GetSignInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(webClientId)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            authResult.user?.let { firebaseUser ->
                val userProfile = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Google User",
                    email = firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    source = "google"
                )
                saveUserProfile(userProfile)
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("Firebase user is null after Google sign-in"))
        } catch (e: FirebaseAuthUserCollisionException) {
            // Xảy ra khi "One account per email address" được bật trong Firebase Console
            // và email này đã được sử dụng bởi một nhà cung cấp khác (ví dụ: Email/Password)
            Result.failure(Exception("Email_already_registered_with_password"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun signUpWithEmailPassword(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            authResult.user?.let { firebaseUser ->
                val userProfile = User(
                    uid = firebaseUser.uid,
                    name = name,
                    email = firebaseUser.email,
                    photoUrl = null,
                    source = "email_password"
                )
                saveUserProfile(userProfile).fold(
                    onSuccess = { Result.success(firebaseUser) },
                    onFailure = { ex -> Result.failure(Exception("User created, but failed to save profile: ${ex.message}", ex)) }
                )
            } ?: Result.failure(Exception("Firebase user is null after email/password sign-up"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email_already_registered_with_google_or_password"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            authResult.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Firebase user is null after email/password sign-in"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signOut() {
        auth.signOut()
        // Theo tài liệu Credential Manager, việc signOut khỏi FirebaseAuth là đủ.
        // Hệ thống sẽ quản lý trạng thái đăng nhập Google.
    }
}