package com.project.speciesdetection.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.project.speciesdetection.core.services.backend.species.SpeciesApiService
import com.project.speciesdetection.core.services.remote_database.DataResult
import com.project.speciesdetection.data.local.species.LocalSpecies
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.local.species.toLocal
import com.project.speciesdetection.data.local.species_class.LocalSpeciesClass
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class OfflineDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speciesApiService: SpeciesApiService,
    @Named("remote_species_class_repo") private val speciesClassRepository: SpeciesClassRepository,
    private val speciesDao: SpeciesDao,
    private val speciesClassDao: SpeciesClassDao
) {
    companion object {
        private const val TAG = "OfflineDataRepository"
        private const val OFFLINE_IMAGES_DIR = "offline_species_images"
    }

    suspend fun downloadAllDataForLanguage(languageCode: String): DataResult<Unit> = coroutineScope {
        Log.d(TAG, "Starting download for language: $languageCode")
        try {
            val speciesDeferred = async { speciesApiService.getAllSpeciesForLanguage(languageCode) }
            val speciesClassDeferred = async { speciesClassRepository.getAll() }

            val speciesResponse = speciesDeferred.await()
            val speciesClassResponse = speciesClassDeferred.await()

            Log.d(TAG, "API calls finished. Species success: ${speciesResponse.success}, Classes fetched: ${speciesClassResponse.size}")

            if (!speciesResponse.success || speciesClassResponse.isEmpty()) {
                val errorMsg = if (!speciesResponse.success) "Failed to fetch species: ${speciesResponse.message}" else "Failed to fetch species classes"
                Log.e(TAG, errorMsg)
                return@coroutineScope DataResult.Error(Exception(errorMsg))
            }

            cleanupOldData(languageCode)

            val localSpeciesList = speciesResponse.data.map { displayableSpecies ->
                val localThumbnailsImage = downloadAndSaveImage(
                    speciesId = displayableSpecies.id,
                    remoteUrl = displayableSpecies.thumbnailImageURL,
                    imageIndex = "thumbnail" // Sử dụng index đặc biệt cho thumbnail
                )
                val localImagePaths = downloadImagesForSpecies(displayableSpecies.id, displayableSpecies.imageURL)
                displayableSpecies.toLocal(languageCode).copy(imageURL = localImagePaths, thumbnailImageURL = localThumbnailsImage ?: "")
            }

            val localSpeciesClassList = speciesClassResponse.mapNotNull { sClass ->
                val nameToUse = sClass.name[languageCode] ?: sClass.name["en"]
                nameToUse?.let { localizedName ->
                    LocalSpeciesClass(id = sClass.id, languageCode = languageCode, localizedName = localizedName)
                }

            }
            Log.i("localspecies", localSpeciesList.toString())
            Log.i("localspeciesclass", localSpeciesClassList.toString())

            Log.d(TAG, "Prepared to insert ${localSpeciesList.size} species and ${localSpeciesClassList.size} classes.")

            if (localSpeciesClassList.isEmpty()) {
                val errorMessage = "No valid species class names found for language '$languageCode' or fallback 'en'."
                Log.e(TAG, errorMessage)
                return@coroutineScope DataResult.Error(Exception(errorMessage))
            }

            speciesDao.insertAll(localSpeciesList)
            speciesClassDao.insertAll(localSpeciesClassList)

            Log.i(TAG, "Successfully downloaded and saved data for language: $languageCode")
            DataResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred during data download for language '$languageCode'", e)
            DataResult.Error(e)
        }
    }

    // --- HÀM NÀY ĐÃ ĐƯỢC SỬA LẠI ---
    private suspend fun downloadImagesForSpecies(speciesId: String, remoteUrls: List<String>): List<String> = coroutineScope {
        val downloadJobs = remoteUrls.mapIndexed { index, url ->
            // Mỗi ảnh sẽ được download song song trong một coroutine riêng
            async {
                downloadAndSaveImage(speciesId, url, index.toString())
            }
        }
        // Chờ tất cả các tác vụ tải ảnh hoàn thành và lọc ra những đường dẫn không null
        downloadJobs.awaitAll().filterNotNull()
    }

    // --- TÁCH RA HÀM RIÊNG ĐỂ TẢI VÀ LƯU 1 ẢNH ---
    private suspend fun downloadAndSaveImage(speciesId: String, remoteUrl: String, imageIndex: String): String? {
        if (remoteUrl.isBlank()) return null

        // Lấy Bitmap từ URL bằng coroutine
        val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (continuation.isActive) {
                        continuation.resume(resource)
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Không cần làm gì ở đây
                }
            }

            // Bắt đầu tải ảnh
            Glide.with(context).asBitmap().load(remoteUrl).into(target)

            // Nếu coroutine bị hủy, hủy luôn tác vụ của Glide
            continuation.invokeOnCancellation {
                Glide.with(context).clear(target)
            }
        }

        // Nếu lấy bitmap thành công, tiến hành lưu file
        // Phần code này đang chạy trên `Dispatchers.IO` nên rất an toàn
        return if (bitmap != null) {
            val imageDir = File(context.filesDir, OFFLINE_IMAGES_DIR)
            if (!imageDir.exists()) imageDir.mkdirs()

            val fileName = "${speciesId}_$imageIndex.jpg"
            val outputFile = File(imageDir, fileName)

            try {
                FileOutputStream(outputFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                outputFile.path // Trả về đường dẫn file đã lưu
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save image to file: ${outputFile.path}", e)
                null
            }
        } else {
            Log.w(TAG, "Failed to download bitmap from url: $remoteUrl")
            null
        }
    }

    private suspend fun cleanupOldData(languageCode: String) {
        val oldLocalSpecies = speciesDao.getAllByLanguage(languageCode)
        // Lấy tất cả các đường dẫn ảnh, bao gồm cả ảnh thumbnail và ảnh trong list
        val oldImagePaths = oldLocalSpecies.flatMap { it.imageURL + it.thumbnailImageURL }.filter { it.isNotBlank() }

        speciesDao.deleteByLanguage(languageCode)
        speciesClassDao.deleteByLanguage(languageCode)

        oldImagePaths.forEach { path ->
            if (path.startsWith(context.filesDir.path)) {
                try {
                    File(path).takeIf { it.exists() }?.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete old image file: $path", e)
                }
            }
        }
    }

    suspend fun removeAllDataForLanguage(languageCode: String) {
        Log.d(TAG, "Removing all data for language: $languageCode")
        cleanupOldData(languageCode)
        Log.i(TAG, "Successfully removed data for language: $languageCode")
    }
}