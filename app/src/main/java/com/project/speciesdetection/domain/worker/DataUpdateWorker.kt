package com.project.speciesdetection.domain.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.speciesdetection.data.OfflineDataRepository
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import com.project.speciesdetection.data.model.species_class.repository.SpeciesClassRepository // Import repository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import javax.inject.Named

/**
 * Một CoroutineWorker được quản lý bởi Hilt để thực hiện tác vụ nền.
 * Nhiệm vụ: Kiểm tra các lớp loài mới và tự động cập nhật dữ liệu offline
 * cho tất cả các ngôn ngữ đã được người dùng tải về.
 */
@HiltWorker
class DataUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Inject các repository và DAO cần thiết
    private val offlineDataRepository: OfflineDataRepository,
    private val speciesClassDao: SpeciesClassDao,
    // Inject repository để lấy danh sách class từ remote server
    @Named("remote_species_class_repo") private val speciesClassRepository: SpeciesClassRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "DailyDataUpdateWorker"
        private const val TAG = "DataUpdateWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker starting. Checking for new data and attempting to update...")

        try {
            // **BƯỚC MỚI: Kiểm tra xem có lớp (class) mới nào trên server không.**
            val remoteClasses = speciesClassRepository.getAll()
            // Lấy ID khoa học (scientific) làm định danh duy nhất cho mỗi lớp
            val remoteClassIds = remoteClasses.mapNotNull { it.name["scientific"]?.lowercase() }.toSet()
            val localClassIds = speciesClassDao.getDistinctClassIds().first().toSet()

            if (remoteClassIds.isEmpty()) {
                Log.w(TAG, "Remote class list is empty. Cannot proceed. Will retry later.")
                return Result.retry()
            }

            // So sánh hai danh sách
            if (remoteClassIds != localClassIds) {
                val newClasses = remoteClassIds - localClassIds
                val removedClasses = localClassIds - remoteClassIds
                if (newClasses.isNotEmpty()) {
                    Log.i(TAG, "New species classes detected on the server: $newClasses")
                }
                if (removedClasses.isNotEmpty()){
                    Log.i(TAG, "Removed species classes detected on the server: $removedClasses")
                }
                Log.i(TAG, "Class list has changed. Proceeding with full data sync for all languages.")
            } else {
                Log.i(TAG, "No new species classes detected. Proceeding with regular data refresh.")
            }

            // 1. Lấy danh sách các mã ngôn ngữ đã được người dùng tải về từ DB.
            val downloadedLanguages = speciesClassDao.getDownloadedLanguageCodes().first()

            if (downloadedLanguages.isEmpty()) {
                Log.i(TAG, "No languages have been downloaded. Worker finished successfully.")
                return Result.success()
            }

            Log.d(TAG, "Found downloaded languages to update: $downloadedLanguages")

            // 2. Lặp qua từng ngôn ngữ và gọi hàm cập nhật từ repository.
            var allUpdatesSuccessful = true
            for (langCode in downloadedLanguages) {
                Log.d(TAG, "Updating data for language: '$langCode'...")
                val result = offlineDataRepository.downloadAllDataForLanguage(langCode)

                if (result is com.project.speciesdetection.core.services.remote_database.DataResult.Error) {
                    allUpdatesSuccessful = false
                    Log.e(TAG, "Failed to update data for language '$langCode'. Error: ${result.exception.message}")
                } else {
                    Log.i(TAG, "Successfully updated data for language: '$langCode'.")
                }
            }

            // 3. Quyết định kết quả cuối cùng của Worker.
            return if (allUpdatesSuccessful) {
                Log.i(TAG, "All languages updated successfully. Worker finished.")
                Result.success()
            } else {
                Log.w(TAG, "One or more languages failed to update. Worker will retry later.")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred in DataUpdateWorker. Retrying...", e)
            return Result.retry()
        }
    }
}