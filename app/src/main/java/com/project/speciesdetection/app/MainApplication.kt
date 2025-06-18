package com.project.speciesdetection.app

import android.app.Application
import com.project.speciesdetection.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class MainApplication(): Application(){
    override fun onCreate() {
        super.onCreate()

        // Cấu hình User Agent cho osmdroid (nếu bạn vẫn dùng nó ở đâu đó)
         Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        // --- THÊM ĐOẠN CODE NÀY ---
        /* Khởi tạo Places SDK với API Key
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
        // --- KẾT THÚC PHẦN THÊM ---*/
    }
}