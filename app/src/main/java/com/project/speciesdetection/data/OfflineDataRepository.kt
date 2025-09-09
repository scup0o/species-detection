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

            if (!speciesResponse.success || speciesClassResponse.isEmpty()) {
                val errorMsg = if (!speciesResponse.success) "Failed to fetch species: ${speciesResponse.message}" else "Failed to fetch species classes"
                Log.e(TAG, errorMsg)
                return@coroutineScope DataResult.Error(Exception(errorMsg))
            }

            speciesDao.deleteByLanguage(languageCode)
            speciesClassDao.deleteByLanguage(languageCode)

            val localSpeciesList = speciesResponse.data.map { displayableSpecies ->
                val localThumbnailsImage = downloadAndSaveImageIfNotExists(
                    speciesId = displayableSpecies.id,
                    remoteUrl = displayableSpecies.thumbnailImageURL,
                    imageIndex = "thumbnail"
                )
                val localImagePaths = downloadImagesForSpeciesIfNotExists(displayableSpecies.id, displayableSpecies.imageURL)
                displayableSpecies.toLocal(languageCode).copy(
                    imageURL = localImagePaths,
                    thumbnailImageURL = localThumbnailsImage ?: ""
                )
            }

            val localSpeciesClassList = speciesClassResponse.mapNotNull { sClass ->
                val nameToUse = sClass.name[languageCode] ?: sClass.name["en"]
                nameToUse?.let { localizedName ->
                    LocalSpeciesClass(id = sClass.id, languageCode = languageCode, localizedName = localizedName, scientific = sClass.name["scientific"]?:"")
                }
            }

            Log.i("local_list",localSpeciesClassList.toString())

            if (localSpeciesClassList.isEmpty()) {
                val errorMessage = "No valid species class names found for language '$languageCode' or fallback 'en'."
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


    suspend fun downloadImagesForSpeciesIfNotExists(speciesId: String, remoteUrls: List<String>): List<String> = coroutineScope {
        remoteUrls.mapIndexed { index, url ->
            async {
                downloadAndSaveImageIfNotExists(speciesId, url, index.toString())
            }
        }.awaitAll().filterNotNull()
    }


    suspend fun downloadAndSaveImageIfNotExists(speciesId: String, remoteUrl: String, imageIndex: String): String? {
        if (remoteUrl.isBlank()) return null

        val imageDir = File(context.filesDir, OFFLINE_IMAGES_DIR)
        if (!imageDir.exists()) imageDir.mkdirs()

        val fileName = "${speciesId}_$imageIndex.jpg"
        val outputFile = File(imageDir, fileName)

        if (outputFile.exists() && outputFile.length() > 0) {
            return outputFile.path
        }

        Log.d(TAG, "Image not found locally, starting download: $remoteUrl")
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


    suspend fun removeAllDataForLanguage(languageCode: String) {
        Log.d(TAG, "Starting removal process for language: $languageCode")

        val speciesForLanguageToRemove = speciesDao.getAllByLanguage(languageCode)
        val imagePathsToRemove = speciesForLanguageToRemove
            .flatMap { it.imageURL + it.thumbnailImageURL }
            .filter { it.isNotBlank() }
            .toSet()

        speciesDao.deleteByLanguage(languageCode)
        speciesClassDao.deleteByLanguage(languageCode)

        val allRemainingSpecies = speciesDao.getAllSpecies()
        val allRemainingImagePaths = allRemainingSpecies
            .flatMap { it.imageURL + it.thumbnailImageURL }
            .filter { it.isNotBlank() }
            .toSet()

        var deletedCount = 0
        imagePathsToRemove.forEach { path ->
            if (!allRemainingImagePaths.contains(path)) {
                try {
                    val fileToDelete = File(path)
                    if (fileToDelete.exists() && fileToDelete.delete()) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete old image file: $path", e)
                }
            }
        }
        Log.i(TAG, "Successfully removed data for language '$languageCode'. Deleted $deletedCount orphan image(s).")
    }
}