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

    /**
     * Tải xuống tất cả dữ liệu (loài, lớp) cho một ngôn ngữ cụ thể và lưu vào cơ sở dữ liệu cục bộ.
     * Hàm này sẽ tự động tải các ảnh liên quan và lưu chúng vào bộ nhớ trong của ứng dụng.
     * Nếu ảnh đã tồn tại, nó sẽ không được tải lại.
     */
    suspend fun downloadAllDataForLanguage(languageCode: String): DataResult<Unit> = coroutineScope {
        Log.d(TAG, "Starting download for language: $languageCode")
        try {
            // Lấy dữ liệu loài và lớp từ API song song
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

            // Xóa dữ liệu cũ của ngôn ngữ này trong DB trước khi thêm dữ liệu mới.
            // Việc này đảm bảo dữ liệu luôn được cập nhật và không bị trùng lặp.
            // Lưu ý: Chỉ xóa dữ liệu trong DB, không xóa file ảnh ở bước này.
            speciesDao.deleteByLanguage(languageCode)
            speciesClassDao.deleteByLanguage(languageCode)

            // Xử lý và chuyển đổi dữ liệu loài
            val localSpeciesList = speciesResponse.data.map { displayableSpecies ->
                val localThumbnailsImage = downloadAndSaveImage(
                    speciesId = displayableSpecies.id,
                    remoteUrl = displayableSpecies.thumbnailImageURL,
                    imageIndex = "thumbnail"
                )
                val localImagePaths = downloadImagesForSpecies(displayableSpecies.id, displayableSpecies.imageURL)
                displayableSpecies.toLocal(languageCode).copy(
                    imageURL = localImagePaths,
                    thumbnailImageURL = localThumbnailsImage ?: ""
                )
            }

            // Xử lý và chuyển đổi dữ liệu lớp
            val localSpeciesClassList = speciesClassResponse.mapNotNull { sClass ->
                val nameToUse = sClass.name[languageCode] ?: sClass.name["en"] // Ưu tiên ngôn ngữ yêu cầu, fallback về tiếng Anh
                nameToUse?.let { localizedName ->
                    LocalSpeciesClass(id = (sClass.name["scientific"]?:"").lowercase(), languageCode = languageCode, localizedName = localizedName)
                }
            }

            if (localSpeciesClassList.isEmpty()) {
                val errorMessage = "No valid species class names found for language '$languageCode' or fallback 'en'."
                Log.e(TAG, errorMessage)
                return@coroutineScope DataResult.Error(Exception(errorMessage))
            }

            Log.d(TAG, "Prepared to insert ${localSpeciesList.size} species and ${localSpeciesClassList.size} classes.")

            // Chèn dữ liệu mới vào cơ sở dữ liệu
            speciesDao.insertAll(localSpeciesList)
            speciesClassDao.insertAll(localSpeciesClassList)

            Log.i(TAG, "Successfully downloaded and saved data for language: $languageCode")
            DataResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "An exception occurred during data download for language '$languageCode'", e)
            DataResult.Error(e)
        }
    }

    /**
     * Tải một danh sách các ảnh cho một loài cụ thể.
     */
    private suspend fun downloadImagesForSpecies(speciesId: String, remoteUrls: List<String>): List<String> = coroutineScope {
        remoteUrls.mapIndexed { index, url ->
            async {
                downloadAndSaveImage(speciesId, url, index.toString())
            }
        }.awaitAll().filterNotNull()
    }

    /**
     * Tải và lưu một ảnh duy nhất.
     * KIỂM TRA NẾU ẢNH ĐÃ TỒN TẠI, SẼ BỎ QUA VIỆC TẢI LẠI.
     */
    private suspend fun downloadAndSaveImage(speciesId: String, remoteUrl: String, imageIndex: String): String? {
        if (remoteUrl.isBlank()) return null

        val imageDir = File(context.filesDir, OFFLINE_IMAGES_DIR)
        if (!imageDir.exists()) imageDir.mkdirs()

        val fileName = "${speciesId}_$imageIndex.jpg"
        val outputFile = File(imageDir, fileName)

        // 1. Kiểm tra xem ảnh đã tồn tại và có nội dung hay chưa
        if (outputFile.exists() && outputFile.length() > 0) {
            Log.d(TAG, "Image already exists, skipping download: ${outputFile.path}")
            return outputFile.path
        }

        Log.d(TAG, "Image not found locally, starting download: $remoteUrl")

        // 2. Tải ảnh bằng Glide nếu chưa có
        val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (continuation.isActive) continuation.resume(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            }
            Glide.with(context).asBitmap().load(remoteUrl).into(target)
            continuation.invokeOnCancellation { Glide.with(context).clear(target) }
        }

        // 3. Lưu bitmap vào file
        return if (bitmap != null) {
            try {
                FileOutputStream(outputFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                outputFile.path
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save image to file: ${outputFile.path}", e)
                null
            }
        } else {
            Log.w(TAG, "Failed to download bitmap from url: $remoteUrl")
            null
        }
    }

    /**
     * Xóa tất cả dữ liệu liên quan đến một ngôn ngữ.
     * THỰC HIỆN XÓA ẢNH THÔNG MINH: CHỈ XÓA FILE ẢNH NẾU KHÔNG CÒN NGÔN NGỮ NÀO KHÁC SỬ DỤNG NÓ.
     */
    suspend fun removeAllDataForLanguage(languageCode: String) {
        Log.d(TAG, "Starting removal process for language: $languageCode")

        // 1. Lấy danh sách ảnh của ngôn ngữ sắp bị xóa
        val speciesForLanguageToRemove = speciesDao.getAllByLanguage(languageCode)
        val imagePathsToRemove = speciesForLanguageToRemove
            .flatMap { it.imageURL + it.thumbnailImageURL }
            .filter { it.isNotBlank() }
            .toSet()

        // 2. Xóa dữ liệu text (loài và lớp) của ngôn ngữ đó khỏi DB
        speciesDao.deleteByLanguage(languageCode)
        speciesClassDao.deleteByLanguage(languageCode)
        Log.d(TAG, "Deleted species and class entries for language '$languageCode' from database.")

        // 3. Lấy danh sách tất cả các ảnh còn lại trong DB (của các ngôn ngữ khác)
        val allRemainingSpecies = speciesDao.getAllSpecies()
        val allRemainingImagePaths = allRemainingSpecies
            .flatMap { it.imageURL + it.thumbnailImageURL }
            .filter { it.isNotBlank() }
            .toSet()

        Log.d(TAG, "Found ${imagePathsToRemove.size} images to potentially remove.")
        Log.d(TAG, "Found ${allRemainingImagePaths.size} images that are still in use by other languages.")

        // 4. Lặp qua danh sách ảnh cần xóa và kiểm tra xem có nên xóa file vật lý không
        var deletedCount = 0
        imagePathsToRemove.forEach { path ->
            // Chỉ xóa ảnh nếu nó không còn được sử dụng bởi bất kỳ ngôn ngữ nào khác
            if (!allRemainingImagePaths.contains(path)) {
                if (path.startsWith(context.filesDir.path)) {
                    try {
                        val fileToDelete = File(path)
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                deletedCount++
                            } else {
                                Log.w(TAG, "Failed to delete file: $path")
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException: Failed to delete old image file: $path", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete old image file: $path", e)
                    }
                }
            } else {
                Log.d(TAG, "Skipping deletion, image is still in use: $path")
            }
        }
        Log.i(TAG, "Successfully removed data for language '$languageCode'. Deleted $deletedCount orphan image(s).")
    }
}