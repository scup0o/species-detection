package com.project.speciesdetection.domain.provider.camera // Hoặc package phù hợp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.project.speciesdetection.domain.model.camera.CameraState
import com.project.speciesdetection.domain.model.camera.ImageCaptureResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.graphics.Rect

// Import suspendCancellableCoroutine nếu chưa có


private const val TAG = "Camera2Helper"

// Bảng ánh xạ giữa Surface.ROTATION_* và độ xoay thực tế
private val ORIENTATIONS = mapOf(
    Surface.ROTATION_0 to 0,
    Surface.ROTATION_90 to 90,
    Surface.ROTATION_180 to 180,
    Surface.ROTATION_270 to 270
)

class Camera2Source @Inject constructor(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null // Store the builder

    private var currentCameraId: String? = null
    private var currentLensFacing: Int = CameraCharacteristics.LENS_FACING_BACK
    private var currentFlashEnabled: Boolean = false
    private var currentScreenRotationDegrees: Int = 0

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var imageReaderThread: HandlerThread? = null
    private var imageReaderHandler: Handler? = null

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState = _cameraState.asStateFlow()

    private val _imageCaptureResult = MutableSharedFlow<ImageCaptureResult>()
    val imageCaptureResult = _imageCaptureResult.asSharedFlow()

    private val _optimalPreviewSize = MutableStateFlow<Size?>(null)
    val optimalPreviewSize = _optimalPreviewSize.asStateFlow()

    // Zoom related properties
    private var activeArraySize: Rect? = null
    private var maxDigitalZoom: Float = 1.0f
    private var currentZoomRatio: Float = 1.0f

    init {
        startBackgroundThreads()
    }

    private fun startBackgroundThreads() {
        if (cameraThread == null || !cameraThread!!.isAlive) {
            cameraThread = HandlerThread("CameraHelperThread").apply { start() }
            cameraHandler = Handler(cameraThread!!.looper)
        }
        if (imageReaderThread == null || !imageReaderThread!!.isAlive) {
            imageReaderThread = HandlerThread("ImageReaderThread").apply { start() }
            imageReaderHandler = Handler(imageReaderThread!!.looper)
        }
        Log.d(TAG, "Background threads started/ensured.")
    }

    private fun stopBackgroundThreads() {
        try {
            cameraThread?.quitSafely()
            cameraThread?.join(500)
            cameraThread = null
            cameraHandler = null

            imageReaderThread?.quitSafely()
            imageReaderThread?.join(500)
            imageReaderThread = null
            imageReaderHandler = null
            Log.d(TAG, "Background threads stopped")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background threads", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(lensFacing: Int, surface: Surface, screenRotationValue: Int, viewWidth: Int, viewHeight: Int) {
        Log.d(TAG, "Attempting to open camera. Lens: $lensFacing, ScreenRotationValue: $screenRotationValue, View: $viewWidth x $viewHeight")
        if (_cameraState.value !is CameraState.Idle && _cameraState.value !is CameraState.Closed && _cameraState.value !is CameraState.Error) {
            Log.w(TAG, "Camera is not in a state to be opened: ${_cameraState.value}")
            return
        }
        startBackgroundThreads()

        this.currentLensFacing = lensFacing
        this.previewSurface = surface
        this.currentScreenRotationDegrees = ORIENTATIONS[screenRotationValue] ?: 0
        this.currentZoomRatio = 1.0f // Reset zoom on camera open/flip

        coroutineScope.launch(Dispatchers.Default) {
            _cameraState.value = CameraState.Opening
            try {
                val cameraId = findCameraId(lensFacing)
                if (cameraId == null) {
                    _cameraState.value = CameraState.Error("No camera found for lens facing: $lensFacing")
                    return@launch
                }
                currentCameraId = cameraId

                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: throw IllegalStateException("Cannot get SCALER_STREAM_CONFIGURATION_MAP for camera $cameraId")

                // Store zoom capabilities
                activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                maxDigitalZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
                // Some devices report 0.0 for max zoom, default to a reasonable max like 8x if so
                if (maxDigitalZoom <= 1.0f) maxDigitalZoom = 8.0f


                val chosenPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), viewWidth, viewHeight)
                _optimalPreviewSize.value = chosenPreviewSize

                imageReader?.close()
                imageReader = ImageReader.newInstance(
                    chosenPreviewSize.width, chosenPreviewSize.height, ImageFormat.JPEG, 2
                ).apply {
                    setOnImageAvailableListener(onImageAvailableListener, imageReaderHandler)
                }

                val openedDevice = openCameraDeviceInternal(cameraId)
                cameraDevice = openedDevice

                val sessionTargets = listOf(surface, imageReader!!.surface)
                val session = createCaptureSessionInternal(openedDevice, sessionTargets)
                captureSession = session

                startPreviewInternal(session, openedDevice, surface) // This will init previewRequestBuilder
                _cameraState.value = CameraState.Previewing(chosenPreviewSize)
                Log.i(TAG, "Camera opened and preview started successfully for $cameraId. Max Zoom: $maxDigitalZoom")

            } catch (e: Exception) {
                Log.e(TAG, "Error while opening camera: ${e.message}", e)
                _cameraState.value = CameraState.Error("Failed to open camera: ${e.localizedMessage}", e)
                closeCameraInternal()
            }
        }
    }

    private fun findCameraId(lensFacing: Int): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Could not access camera list: ${e.message}", e)
            _cameraState.value = CameraState.Error("Failed to access camera list", e)
            null
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, viewWidth: Int, viewHeight: Int): Size {
        // Implement your logic, or use a simple one:
        // This is a very basic implementation. You might want a more sophisticated one.
        val bigEnough = choices.filter { it.width >= viewWidth && it.height >= viewHeight }
        return if (bigEnough.isNotEmpty()) {
            bigEnough.minByOrNull { it.width * it.height } ?: choices[0]
        } else {
            choices.maxByOrNull { it.width * it.height } ?: choices[0]
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCameraDeviceInternal(cameraId: String): CameraDevice =
        suspendCancellableCoroutine { continuation ->
            val callback = object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    Log.d(TAG, "Camera $cameraId opened successfully.")
                    if (continuation.isActive) continuation.resume(device)
                }
                override fun onDisconnected(device: CameraDevice) {
                    Log.w(TAG, "Camera $cameraId disconnected.")
                    device.close()
                    if (continuation.isActive) {
                        _cameraState.value = CameraState.Error("Camera $cameraId disconnected unexpectedly")
                        //continuation.resumeWithException(CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED, "Camera $cameraId disconnected"))
                    }
                }
                override fun onError(device: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera $cameraId error: $error")
                    val errorMessage = "Camera error ($cameraId): $error"
                    device.close()
                    if (continuation.isActive) {
                        _cameraState.value = CameraState.Error(errorMessage, CameraAccessException(error))
                        continuation.resumeWithException(CameraAccessException(error, errorMessage))
                    }
                }
            }
            try {
                cameraManager.openCamera(cameraId, callback, cameraHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate openCamera for $cameraId", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            continuation.invokeOnCancellation {
                Log.d(TAG, "Camera opening coroutine for $cameraId was cancelled.")
                // Potentially close the device if it was opened but coroutine cancelled before resume
            }
        }

    private suspend fun createCaptureSessionInternal(device: CameraDevice, targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { continuation ->
            val sessionStateCallback = object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG, "Capture session configured successfully.")
                    if (continuation.isActive) continuation.resume(session)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val e = RuntimeException("Camera capture session configuration failed")
                    Log.e(TAG, e.message ?: "Unknown session config error")
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val outputConfigs = targets.map { OutputConfiguration(it) }
                    val cameraExecutor = Executor { cameraHandler?.post(it) } // Make sure cameraHandler is not null
                    val sessionConfig = SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputConfigs,
                        cameraExecutor,
                        sessionStateCallback
                    )
                    device.createCaptureSession(sessionConfig)
                } else {
                    @Suppress("DEPRECATION")
                    device.createCaptureSession(targets, sessionStateCallback, cameraHandler)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create capture session", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            continuation.invokeOnCancellation {
                Log.d(TAG, "createCaptureSessionInternal coroutine was cancelled.")
            }
        }

    private fun startPreviewInternal(session: CameraCaptureSession, device: CameraDevice, surface: Surface) {
        if (cameraDevice == null || captureSession == null) {
            Log.w(TAG, "Cannot start preview, camera or session is null.")
            _cameraState.value = CameraState.Error("Preview failed: camera/session null")
            return
        }
        try {
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                // Apply current zoom
                applyZoomToRequestBuilder(this)

                val characteristics = cameraManager.getCameraCharacteristics(device.id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && currentFlashEnabled) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }
            session.setRepeatingRequest(previewRequestBuilder!!.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview session", e)
            _cameraState.value = CameraState.Error("Preview failed: ${e.localizedMessage}", e)
        }
    }

    fun captureImage() {
        if (_cameraState.value !is CameraState.Previewing) {
            Log.w(TAG, "Cannot capture image, not in previewing state. State: ${_cameraState.value}")
            coroutineScope.launch {
                _imageCaptureResult.emit(ImageCaptureResult.Error("Not ready to capture. Current state: ${_cameraState.value}"))
            }
            return
        }
        if (cameraDevice == null || captureSession == null || imageReader == null) {
            Log.e(TAG, "Cannot capture: cameraDevice, captureSession or imageReader is null")
            coroutineScope.launch {
                _imageCaptureResult.emit(ImageCaptureResult.Error("Internal error: camera resources not ready."))
            }
            return
        }
        _cameraState.value = CameraState.Capturing
        Log.d(TAG, "Attempting to capture image. Flash enabled: $currentFlashEnabled. Screen Rotation Degrees: $currentScreenRotationDegrees")
        try {
            val device = cameraDevice!!
            val session = captureSession!!
            val readerSurface = imageReader!!.surface

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(readerSurface)
                // Apply current zoom to capture request
                applyZoomToRequestBuilder(this)

                val characteristics = cameraManager.getCameraCharacteristics(device.id)
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                val jpegOrientation = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> (sensorOrientation + currentScreenRotationDegrees) % 360
                    else -> (sensorOrientation - currentScreenRotationDegrees + 360) % 360
                }
                set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
                Log.d(TAG, "Sensor Orientation: $sensorOrientation, Screen Rotation Degrees: $currentScreenRotationDegrees -> JPEG Orientation: $jpegOrientation")

                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && currentFlashEnabled) {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                } else {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(s: CameraCaptureSession, req: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(TAG, "Image capture completed.")
                    val currentPreviewSurface = previewSurface
                    if (cameraDevice != null && currentPreviewSurface != null) {
                        startPreviewInternal(s, cameraDevice!!, currentPreviewSurface) // Restart preview
                        _cameraState.value = CameraState.Previewing(_optimalPreviewSize.value ?: Size(0,0) )
                    } else {
                        _cameraState.value = CameraState.Error("Failed to restart preview post-capture")
                    }
                }
                override fun onCaptureFailed(s: CameraCaptureSession, req: CaptureRequest, failure: CaptureFailure) {
                    Log.e(TAG, "Image capture failed. Reason: ${failure.reason}")
                    coroutineScope.launch {
                        _imageCaptureResult.emit(ImageCaptureResult.Error("Capture failed: ${failure.reason}"))
                    }
                    val currentPreviewSurface = previewSurface
                    if (cameraDevice != null && currentPreviewSurface != null) {
                        startPreviewInternal(s, cameraDevice!!, currentPreviewSurface) // Restart preview
                        _cameraState.value = CameraState.Previewing(_optimalPreviewSize.value ?: Size(0,0))
                    } else {
                        _cameraState.value = CameraState.Error("Failed to restart preview post-capture-failure")
                    }
                } }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate capture or during capture callback", e)
            coroutineScope.launch {
                _imageCaptureResult.emit(ImageCaptureResult.Error("Capture failed: ${e.localizedMessage}", e))
            }
            _cameraState.value = CameraState.Error("Capture failed: ${e.localizedMessage}", e)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image? = try {
            reader.acquireLatestImage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire image from reader", e)
            null
        }
        image?.let { img ->
            coroutineScope.launch(Dispatchers.IO) {
                val buffer = img.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                img.close()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val photoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "JPEG_${timeStamp}.jpg")
                try {
                    FileOutputStream(photoFile).use { output -> output.write(bytes) }
                    val uri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Image saved successfully: $uri")
                    _imageCaptureResult.emit(ImageCaptureResult.Success(uri))
                } catch (e: IOException) {
                    Log.e(TAG, "Error saving image", e)
                    _imageCaptureResult.emit(ImageCaptureResult.Error("Failed to save image", e))
                }
            }
        }
    }

    fun setFlashEnabled(enabled: Boolean) {
        currentFlashEnabled = enabled
        if (_cameraState.value is CameraState.Previewing && cameraDevice != null && captureSession != null && previewSurface != null) {
            // Update repeating request with new flash state
            previewRequestBuilder?.let { builder ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraDevice!!.id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && currentFlashEnabled) {
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
                try {
                    captureSession?.setRepeatingRequest(builder.build(), null, cameraHandler)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update flash state for preview", e)
                }
            }
        }
    }

    fun updateScreenRotation(newScreenRotationValue: Int) {
        this.currentScreenRotationDegrees = ORIENTATIONS[newScreenRotationValue] ?: 0
        Log.d(TAG, "Screen rotation updated. New Surface.ROTATION value: $newScreenRotationValue, Degrees: $currentScreenRotationDegrees")
    }

    // --- ZOOM FUNCTIONALITY ---
    private fun applyZoomToRequestBuilder(builder: CaptureRequest.Builder) {
        val sensorRect = activeArraySize ?: return // No zoom if we don't have the active array size

        val zoom = currentZoomRatio.coerceIn(1.0f, maxDigitalZoom)

        val cropWidth = (sensorRect.width() / zoom).toInt()
        val cropHeight = (sensorRect.height() / zoom).toInt()

        val cropLeft = (sensorRect.width() - cropWidth) / 2
        val cropTop = (sensorRect.height() - cropHeight) / 2
        val cropRight = cropLeft + cropWidth
        val cropBottom = cropTop + cropHeight

        val cropRect = Rect(cropLeft, cropTop, cropRight, cropBottom)
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
    }

    fun setZoom(zoomRatio: Float) {
        if (cameraDevice == null || captureSession == null || previewRequestBuilder == null || activeArraySize == null) {
            Log.w(TAG, "Cannot set zoom, camera not ready or activeArraySize not available.")
            return
        }
        if (_cameraState.value !is CameraState.Previewing) {
            Log.w(TAG, "Cannot set zoom, not in previewing state.")
            return
        }

        val newZoom = zoomRatio.coerceIn(1.0f, maxDigitalZoom)
        if (newZoom == currentZoomRatio) return // No change

        currentZoomRatio = newZoom
        Log.d(TAG, "Setting zoom to: $currentZoomRatio (clamped from $zoomRatio, max: $maxDigitalZoom)")

        applyZoomToRequestBuilder(previewRequestBuilder!!) // Update the stored builder
        try {
            captureSession?.setRepeatingRequest(previewRequestBuilder!!.build(), null, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set repeating request for zoom", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state while setting repeating request for zoom (session might be closed)", e)
        }
    }
    // --- END ZOOM FUNCTIONALITY ---


    fun releaseCamera() {
        Log.i(TAG, "Release camera requested.")
        closeCameraInternal()
        stopBackgroundThreads()
        _cameraState.value = CameraState.Closed
        _optimalPreviewSize.value = null
        // Reset zoom related properties
        activeArraySize = null
        maxDigitalZoom = 1.0f
        currentZoomRatio = 1.0f
        previewRequestBuilder = null
        Log.i(TAG, "Camera and threads released.")
    }

    private fun closeCameraInternal() {
        Log.d(TAG, "Closing camera internal resources...")
        try {
            captureSession?.stopRepeating() // Important to stop repeating requests
            captureSession?.abortCaptures()
        } catch (e: Exception) {Log.e(TAG, "Error stopping session", e)}
        try {
            captureSession?.close()
            captureSession = null
        } catch (e: Exception) { Log.e(TAG, "Error closing capture session", e) }
        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) { Log.e(TAG, "Error closing camera device", e) }
        try {
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) { Log.e(TAG, "Error closing image reader", e) }
        previewRequestBuilder = null // Clear the builder
    }
}