package com.project.speciesdetection.domain.provider

import android.content.Context
import com.project.speciesdetection.domain.provider.camera.Camera2Source
import com.project.speciesdetection.domain.provider.camera.CameraProvider
import com.project.speciesdetection.domain.provider.camera.DefaultCameraProvider
import com.project.speciesdetection.domain.provider.language.DeviceLanguageProvider
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Qualifier

// Qualifier cho CoroutineScope của Camera
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CameraCoroutineScope

@Module
@InstallIn(SingletonComponent::class) // Module này có thể chứa các binding cho các scope khác nhau
abstract class LanguageProviderModule { // Tách module language riêng cho rõ ràng
    @Binds
    @Named("language_provider") // Giả sử language provider là singleton
    abstract fun bindDeviceLanguageProvider(
        deviceLanguageProvider: DeviceLanguageProvider
    ): LanguageProvider
}

@Module
@InstallIn(ViewModelComponent::class) // Cài đặt vào ViewModelComponent vì camera thường gắn với ViewModel
object CameraProviderModule {

    @Provides
    @ViewModelScoped // Scope này sẽ sống cùng ViewModel
    @CameraCoroutineScope // Sử dụng qualifier
    fun provideCameraCoroutineScope(): CoroutineScope {
        // Tạo một scope mới, nó sẽ tự động bị cancel khi ViewModelComponent bị hủy
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @ViewModelScoped // Camera2Source cũng nên có cùng scope với CameraProvider nó phục vụ
    fun provideCamera2Source(
        @ApplicationContext context: Context,
        @CameraCoroutineScope coroutineScope: CoroutineScope // Inject scope đã định nghĩa
    ): Camera2Source {
        return Camera2Source(context, coroutineScope)
    }

    @Provides
    @ViewModelScoped // DefaultCameraProvider sẽ có cùng scope
    @Named("camera_provider") // Đảm bảo khớp với @Named trong ViewModel
    fun provideCameraProvider(
        camera2Source: Camera2Source // Inject Camera2Source đã được Hilt cung cấp
    ): CameraProvider {
        return DefaultCameraProvider(camera2Source)
    }
}