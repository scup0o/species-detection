package com.project.speciesdetection.data.model.observation.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.GeoPoint
import com.project.speciesdetection.core.services.remote_database.ObservationDatabaseService
import com.project.speciesdetection.core.services.storage.StorageService
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.data.model.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class RemoteObservationRepository @Inject constructor(
    @Named("observation_db") private val databaseService: ObservationDatabaseService,
    private val storageService: StorageService
) : ObservationRepository {

    override suspend fun createObservation(
        user: User,
        speciesId: String,
        content: String,
        imageUris: List<Uri>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Tải tất cả ảnh lên song song
            val imageUrls = imageUris.map { uri ->
                async { storageService.uploadImage(uri) }
            }.awaitAll().mapNotNull { it.getOrNull() }



            val newObservation = Observation(
                uid = user.uid,
                speciesId = speciesId,
                content = content,
                imageURL = imageUrls,
                privacy = privacy,
                location = location,
                dateFound = dateFound,
                point = 0 // Mới tạo
            )

            databaseService.createObservation(newObservation).map { } // Convert Result<String> to Result<Unit>
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateObservation(
        observationId: String,
        speciesId: String,
        content: String,
        images: List<Any>,
        privacy: String,
        location: GeoPoint?,
        dateFound: Timestamp?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Phân loại ảnh cũ và ảnh mới cần upload
            val existingImageUrls = images.filterIsInstance<String>()
            val newImageUris = images.filterIsInstance<Uri>()

            // Tải ảnh mới lên
            val newImageUrls = newImageUris.map { uri ->
                async { storageService.uploadImage(uri) }
            }.awaitAll().mapNotNull { it.getOrNull() }

            val finalImageUrls = existingImageUrls + newImageUrls

            // Tạo đối tượng observation để update
            // Lưu ý: chỉ set các trường cần update, các trường khác sẽ được giữ nguyên nhờ SetOptions.merge()
            val observationToUpdate = Observation(
                id = observationId,
                speciesId = speciesId,
                content = content,
                imageURL = finalImageUrls,
                privacy = privacy,
                location = location,
                dateFound = dateFound
            )

            databaseService.updateObservation(observationToUpdate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}