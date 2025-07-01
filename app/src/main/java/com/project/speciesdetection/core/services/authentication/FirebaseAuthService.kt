package com.project.speciesdetection.core.services.authentication

import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class FirebaseAuthService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthServiceInterface {

    private val webClientId = "1012000261168-h55p7b5hpoe3ub4v7ovoa0s2361j8qon.apps.googleusercontent.com" // Chỉ cần webClientId để tạo GetGoogleIdOption

    override suspend fun updateAuthProfile(newName: String?, newPhotoUrl: String?): Result<Unit> {
        // Lấy người dùng hiện tại, nếu chưa đăng nhập thì không thể thực hiện.
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in."))

        return try {
            // Sử dụng UserProfileChangeRequest.Builder để tạo đối tượng cập nhật
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                // Chỉ thêm vào builder nếu giá trị không phải là null
                newName?.let { displayName = it }
                newPhotoUrl?.let { photoUri = Uri.parse(it) }
            }.build()

            // Gọi hàm updateProfile của FirebaseUser và chờ hoàn tất
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAuthPassword(newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in."))

        return try {
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Bắt lỗi, thường là do chưa xác thực lại.
            Result.failure(e)
        }
    }

    /**
     * Xác thực lại người dùng bằng mật khẩu hiện tại của họ.
     * Đây là bước bắt buộc trước khi thực hiện các hành động nhạy cảm như đổi mật khẩu hoặc xóa tài khoản.
     * @param password Mật khẩu hiện tại của người dùng.
     * @return Result<Unit> cho biết thành công hay thất bại.
     */
    override suspend fun reauthenticateUser(password: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in."))
        // Lấy email của người dùng để tạo credential
        val email = user.email ?: return Result.failure(Exception("Cannot re-authenticate user without an email."))

        return try {
            // Tạo một credential từ email và mật khẩu được cung cấp
            val credential = EmailAuthProvider.getCredential(email, password)

            // Gọi hàm reauthenticate và chờ hoàn tất
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Bắt lỗi, thường là do sai mật khẩu.
            Result.failure(e)
        }
    }

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
                    source = "google.com"
                )
                saveUserProfile(userProfile)
                Result.success(firebaseUser)
            } ?: Result.failure(Exception("Firebase user is null after Google sign-in"))
        } catch (e: FirebaseAuthUserCollisionException) {
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
                    source = "password"
                )

                // Gửi email xác thực
                firebaseUser.sendEmailVerification().await()

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
            /*val placeholder = mapOf("placeholder" to 0)
            firestore.collection("followers").document(user.uid).set(placeholder).await()
            firestore.collection("following").document(user.uid).set(placeholder).await()
            firestore.collection("feeds").document(user.uid).set(placeholder).await()
            */Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        val user = auth.currentUser
        return if (user != null && !user.isEmailVerified) {
            try {
                user.sendEmailVerification().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else if (user == null) {
            Result.failure(Exception("No logged-in user found."))
        } else {
            Result.failure(Exception("Email is already verified."))
        }
    }

}