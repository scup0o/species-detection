package com.project.speciesdetection.data.model.user.repository

import android.net.Uri
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.project.speciesdetection.data.model.user.User

interface UserRepository {
    suspend fun updateProfileInfo(
        userId: String,
        newName: String?,
        newPhoto: Uri? // <-- SỬA TỪ String? THÀNH Uri?
    ): Result<String?>

    // HÀM MỚI 2: Cập nhật mật khẩu
    suspend fun updatePassword(newPassword: String): Result<Unit>

    // HÀM MỚI 3: Xác thực lại người dùng
    suspend fun reauthenticateUser(password: String): Result<Unit>
    fun createGoogleSignInRequest(): GetCredentialRequest
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser>
    suspend fun signUpWithEmailPassword(email: String, pass: String, name: String): Result<FirebaseUser>
    suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser>
    fun getCurrentUser(): FirebaseUser?

    suspend fun getUserInformation(uid : String): User?

    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
    fun addCurrentUserFcmToken(userId: String, token: String)
    fun removeCurrentUserFcmToken(userId: String, token: String, onComplete: () -> Unit)
}