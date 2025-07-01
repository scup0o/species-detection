package com.project.speciesdetection.data.model.user.repository

import android.net.Uri
import android.util.Log
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.speciesdetection.core.services.authentication.AuthServiceInterface
import com.project.speciesdetection.core.services.authentication.FirebaseAuthService
import com.project.speciesdetection.core.services.storage.StorageService
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RemoteUserRepository @Inject constructor(
    private val authService: AuthServiceInterface,
    private val firestore: FirebaseFirestore,
    private val storageService: StorageService
) : UserRepository{

    override suspend fun updateProfileInfo(
        userId: String,
        newName: String?,
        newPhoto: Uri? // Nhận vào một Uri
    ): Result<String?> {
        return try {
            var finalPhotoUrl: String? = null

            // BƯỚC 1: UPLOAD ẢNH MỚI (NẾU CÓ)
            if (newPhoto != null) {
                Log.d("UpdateProfile", "New photo Uri detected. Uploading to Cloudinary...")
                // Gọi storageService để upload ảnh
                val uploadResult = storageService.uploadImage(newPhoto)

                if (uploadResult.isSuccess) {
                    finalPhotoUrl = uploadResult.getOrNull()
                    Log.d("UpdateProfile", "Upload successful. New URL: $finalPhotoUrl")
                } else {
                    // Nếu upload thất bại, trả về lỗi ngay lập tức
                    Log.e("UpdateProfile", "Photo upload failed.", uploadResult.exceptionOrNull())
                    return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Unknown upload error"))
                }
            }

            // Kiểm tra xem có gì để cập nhật không.
            // Nếu người dùng chỉ đổi ảnh mà không đổi tên, newName sẽ là null.
            if (newName == null && finalPhotoUrl == null) {
                Log.d("UpdateProfile", "No changes to update.")
                return Result.success(finalPhotoUrl)// Không có gì thay đổi, trả về thành công.
            }

            // BƯỚC 2: CẬP NHẬT TRÊN FIREBASE AUTH
            // `finalPhotoUrl` sẽ là URL mới hoặc null nếu không đổi ảnh
            val authUpdateResult = authService.updateAuthProfile(newName, finalPhotoUrl)
            if (authUpdateResult.isFailure) {
                return Result.failure(authUpdateResult.exceptionOrNull()!!) // Trả về lỗi từ Auth nếu thất bại
            }

            // BƯỚC 3: CẬP NHẬT TRÊN FIRESTORE
            val firestoreUpdates = mutableMapOf<String, Any>()
            newName?.let { firestoreUpdates["name"] = it }
            finalPhotoUrl?.let { firestoreUpdates["photoUrl"] = it }

            if (firestoreUpdates.isNotEmpty()) {
                firestore.collection("users").document(userId)
                    .update(firestoreUpdates)
                    .await()
            }

            // BƯỚC 4: (QUAN TRỌNG) GỌI API SERVER HOẶC ĐỂ CLOUD FUNCTION TỰ CHẠY
            // Nếu bạn dùng Cloud Function, nó sẽ tự kích hoạt khi document user được update.
            // Nếu bạn dùng server riêng, bạn cần gọi API ở đây để đồng bộ dữ liệu.
            // Ví dụ: myApiService.propagateUserProfileUpdate(userId, newName, finalPhotoUrl)

            Result.success(finalPhotoUrl)
        } catch (e: Exception) {
            Log.e("UpdateProfileRepo", "Error updating profile in repository", e)
            Result.failure(e)
        }
    }

    /**
     * Cập nhật mật khẩu người dùng. Yêu cầu re-authenticate trước khi gọi.
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        // Hàm này chỉ đơn giản là gọi qua authService
        return authService.updateAuthPassword(newPassword)
    }

    /**
     * Xác thực lại người dùng với mật khẩu hiện tại. Cần thiết cho các hành động nhạy cảm.
     */
    override suspend fun reauthenticateUser(password: String): Result<Unit> {
        // Hàm này chỉ đơn giản là gọi qua authService
        return authService.reauthenticateUser(password)
    }

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