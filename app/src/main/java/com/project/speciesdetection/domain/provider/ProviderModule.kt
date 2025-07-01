package com.project.speciesdetection.domain.provider

import com.project.speciesdetection.BuildConfig
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.project.speciesdetection.core.services.backend.message.MessageApiService
import com.project.speciesdetection.core.services.backend.species.SpeciesApiService
import com.project.speciesdetection.core.services.backend.user.UserApiService
import com.project.speciesdetection.core.services.content_moderation.SightengineApi
import com.project.speciesdetection.domain.provider.image_classifier.EnetB0ImageClassifier
import com.project.speciesdetection.domain.provider.image_classifier.EnetLite0ImageClassifier
import com.project.speciesdetection.domain.provider.image_classifier.ImageClassifierProvider
import com.project.speciesdetection.domain.provider.language.DeviceLanguageProvider
import com.project.speciesdetection.domain.provider.language.LanguageProvider
import com.project.speciesdetection.domain.provider.network.ConnectivityObserver
import com.project.speciesdetection.domain.provider.network.NetworkConnectivityObserver
import dagger.Binds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Named
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import retrofit2.Retrofit
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageProviderModule {
    @Binds
    @Named("language_provider")
    abstract fun bindDeviceLanguageProvider(
        deviceLanguageProvider: DeviceLanguageProvider
    ): LanguageProvider
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageClassifierProviderModule {
    @Binds
    @Named("enetlite0_classifier_provider")
    abstract fun bindImageClassifierProvider(
        imageClassifierProviderModule: EnetLite0ImageClassifier
    ): ImageClassifierProvider

    @Binds
    @Named("enetb0_classifier_provider")
    abstract fun bindEnetB0ImageClassifierProvider(
        imageClassifierProviderModule: EnetB0ImageClassifier
    ): ImageClassifierProvider
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://species-detection-web-server-git-master-scup0os-projects.vercel.app"

    /*@Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Log request/response body
            })
            // .connectTimeout(30, TimeUnit.SECONDS) // Tùy chỉnh timeout nếu cần
            // .readTimeout(30, TimeUnit.SECONDS)
            // .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }*/

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Tạo một interceptor để thêm User-Agent vào mỗi request
        val userAgentInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                // Đặt User-Agent là Application ID, theo đúng yêu cầu của OSM
                .header("User-Agent", BuildConfig.APPLICATION_ID)
                .build()
            chain.proceed(requestWithUserAgent)
        }

        return OkHttpClient.Builder()
            // Thêm interceptor log để debug (nên đặt trước)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }
            )
            // THÊM INTERCEPTOR USER-AGENT
            .addInterceptor(userAgentInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true // Rất quan trọng nếu API có thể trả về thêm trường
            isLenient = true         // Cho phép một số định dạng JSON không chuẩn (ít dùng)
            // prettyPrint = true    // Chỉ bật khi debug, tắt khi release
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL) // Sử dụng BASE_URL đã cập nhật
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSpeciesApiService(retrofit: Retrofit): SpeciesApiService {
        return retrofit.create(SpeciesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageApiService(retrofit: Retrofit): MessageApiService {
        return retrofit.create(MessageApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }


    @Provides
    @Singleton
    @Named("CloudinaryCloudName")
    fun provideCloudinaryCloudName(): String = BuildConfig.CLOUDINARY_CLOUD_NAME

    @Provides
    @Singleton
    @Named("CloudinaryUploadPreset")
    fun provideCloudinaryUploadPreset(): String = BuildConfig.CLOUDINARY_UPLOAD_PRESET

}

@Module
@InstallIn(SingletonComponent::class)
object ModerationModule {
    @Provides
    @Singleton
    fun provideSightengineApi(okHttpClient: OkHttpClient, json: Json): SightengineApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.sightengine.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SightengineApi::class.java)
    }
}



@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(connectivityManager: ConnectivityManager): ConnectivityObserver {
        return NetworkConnectivityObserver(connectivityManager)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

}



/*
// Qualifier cho CoroutineScope của Camera
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CameraCoroutineScope

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
}*/