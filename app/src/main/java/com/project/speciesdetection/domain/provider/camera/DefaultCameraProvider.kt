package com.project.speciesdetection.domain.provider.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton // ProcessCameraProvider is a singleton, so this source can be too.
class DefaultCameraProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraProvider {

    override suspend fun get(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                if (continuation.isActive) {
                    continuation.resume(cameraProviderFuture.get())
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }, mainExecutor)

        continuation.invokeOnCancellation {
            // Optional: Cancel the future if the coroutine is cancelled,
            // though addListener doesn't directly support removal for a specific listener.
            // For ProcessCameraProvider.getInstance, it's generally safe as it completes quickly.
        }
    }
}