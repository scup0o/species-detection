package com.project.speciesdetection.domain.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.speciesdetection.data.OfflineDataRepository
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Một CoroutineWorker được quản lý bởi Hilt để thực hiện tác vụ nền.
 * Nhiệm vụ: Tự động cập nhật dữ liệu offline cho tất cả các ngôn ngữ
 * đã được người dùng tải về.
 */
@HiltWorker
class DataUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Inject các repository và DAO cần thiết
    private val offlineDataRepository: OfflineDataRepository,
    private val speciesClassDao: SpeciesClassDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "DailyDataUpdateWorker"
        private const val TAG = "DataUpdateWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Worker starting. Attempting to update offline data...")

        try {
            // 1. Lấy danh sách các mã ngôn ngữ đã được người dùng tải về từ DB.
            // Dùng .first() để lấy giá trị hiện tại từ Flow.
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

                // Nếu bất kỳ lần tải nào thất bại, ghi nhận lại nhưng vẫn tiếp tục với các ngôn ngữ khác.
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
                // Trả về retry() để WorkManager lên lịch chạy lại tác vụ này sau một khoảng thời gian.
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred in DataUpdateWorker. Retrying...", e)
            // Nếu có lỗi không mong muốn (ví dụ: mất kết nối đột ngột), thử lại.
            return Result.retry()
        }
    }
}