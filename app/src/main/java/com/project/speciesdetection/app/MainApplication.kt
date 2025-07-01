package com.project.speciesdetection.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import com.project.speciesdetection.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import javax.inject.Inject
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.project.speciesdetection.domain.worker.DataUpdateWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MainApplication(): Application(){
    /*@Inject
    lateinit var workerFactory: HiltWorkerFactory*/

    override fun onCreate() {
        super.onCreate()
        scheduleDailyDataUpdate()

        // Cấu hình User Agent cho osmdroid (nếu bạn vẫn dùng nó ở đâu đó)
         Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID


    }

    private fun scheduleDailyDataUpdate() {
        // Điều kiện để Worker có thể chạy: thiết bị phải có kết nối mạng.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Tính toán độ trễ ban đầu để tác vụ chạy lần đầu vào 3h sáng hôm sau.
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Nếu 3h sáng hôm nay đã qua, đặt lịch cho ngày mai.
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        // Tạo yêu cầu công việc lặp lại mỗi 24 giờ.
        val dailyUpdateRequest =
            PeriodicWorkRequestBuilder<DataUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(DataUpdateWorker.WORK_NAME) // Gắn tag để dễ dàng theo dõi/hủy
                .build()

        // Gửi yêu cầu đến WorkManager.
        // Sử dụng enqueueUniquePeriodicWork để đảm bảo chỉ có một công việc
        // với tên này được xếp hàng tại một thời điểm.
        // KEEP: Nếu đã có công việc đang chờ, giữ lại nó.
        // REPLACE: Nếu đã có, hủy nó và thay thế bằng công việc mới.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DataUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyUpdateRequest
        )
    }
}