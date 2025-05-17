package com.project.speciesdetection.domain.provider.camera // Hoặc package phù hợp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.view.Surface
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.CameraState
import com.project.speciesdetection.ui.features.identification_camera_screen.viewmodel.ImageCaptureResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor // Import đúng
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
// Import suspendCancellableCoroutine nếu chưa có
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs


private const val TAG = "Camera2Helper"

// Bảng ánh xạ giữa Surface.ROTATION_* và độ xoay thực tế
private val ORIENTATIONS = mapOf(
    Surface.ROTATION_0 to 0,
    Surface.ROTATION_90 to 90,
    Surface.ROTATION_180 to 180,
    Surface.ROTATION_270 to 270
)

class CameraHelper(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private var currentCameraId: String? = null
    private var currentLensFacing: Int = CameraCharacteristics.LENS_FACING_BACK
    private var currentFlashEnabled: Boolean = false
    private var currentScreenRotationDegrees: Int = 0 // Lưu trữ độ xoay của màn hình

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
        startBackgroundThreads() // Đảm bảo thread chạy

        this.currentLensFacing = lensFacing
        this.previewSurface = surface
        this.currentScreenRotationDegrees = ORIENTATIONS[screenRotationValue] ?: 0 // Chuyển đổi Surface.ROTATION_* thành độ

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

                startPreviewInternal(session, openedDevice, surface)
                _cameraState.value = CameraState.Previewing(chosenPreviewSize)
                Log.i(TAG, "Camera opened and preview started successfully for $cameraId.")

            } catch (e: Exception) { // Bắt Exception chung để bao quát hơn
                Log.e(TAG, "Error while opening camera: ${e.message}", e)
                _cameraState.value = CameraState.Error("Failed to open camera: ${e.localizedMessage}", e)
                closeCameraInternal()
            }
        }
    }

    private fun findCameraId(lensFacing: Int): String? {
        // ... (giữ nguyên)
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
        val targetAspectRatio = 4f / 3f // hoặc 16f / 9f
        val suitableSizes = choices.filter {
            val ratio = it.width.toFloat() / it.height.toFloat()
            kotlin.math.abs(ratio - targetAspectRatio) < 0.01
        }

        return suitableSizes
            .minByOrNull { kotlin.math.abs(it.width - viewWidth) + kotlin.math.abs(it.height - viewHeight) }
            ?: choices[0]
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCameraDeviceInternal(cameraId: String): CameraDevice =
        suspendCancellableCoroutine { continuation ->
            // ... (giữ nguyên)
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
            } catch (e: Exception) { // Bắt Exception chung
                Log.e(TAG, "Failed to initiate openCamera for $cameraId", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            continuation.invokeOnCancellation {
                Log.d(TAG, "Camera opening coroutine for $cameraId was cancelled.")
            }
        }

    private suspend fun createCaptureSessionInternal(device: CameraDevice, targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { continuation ->
            // ... (giữ nguyên như đã sửa ở lần trước, sử dụng Executor từ Handler)
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
                    val cameraExecutor = Executor { command -> cameraHandler!!.post(command) }
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
            } catch (e: Exception) { // Bắt Exception chung
                Log.e(TAG, "Failed to create capture session", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            continuation.invokeOnCancellation {
                Log.d(TAG, "createCaptureSessionInternal coroutine was cancelled.")
            }
        }

    private fun startPreviewInternal(session: CameraCaptureSession, device: CameraDevice, surface: Surface) {
        // ... (giữ nguyên)
        if (cameraDevice == null || captureSession == null) {
            Log.w(TAG, "Cannot start preview, camera or session is null.")
            _cameraState.value = CameraState.Error("Preview failed: camera/session null")
            return
        }
        try {
            val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                val characteristics = cameraManager.getCameraCharacteristics(device.id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && currentFlashEnabled) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }
            session.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
        } catch (e: Exception) { // Bắt Exception chung
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
                // Không nên set CONTROL_AF_MODE ở đây nếu bạn không trigger AF trước.
                // Nếu muốn AF, bạn cần một chu trình AF riêng. Để đơn giản, tạm bỏ.
                // set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                val characteristics = cameraManager.getCameraCharacteristics(device.id)
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

                // ***** THAY ĐỔI QUAN TRỌNG Ở ĐÂY *****
                val jpegOrientation: Int
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    jpegOrientation = (sensorOrientation + currentScreenRotationDegrees + 360) % 360
                } else { // BACK-facing
                    jpegOrientation = (sensorOrientation - currentScreenRotationDegrees + 360) % 360
                }
                // Một số thiết bị/API có thể cần đảo ngược kết quả cho camera trước
                // if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                //     jpegOrientation = (360 - jpegOrientation) % 360;
                // }

                set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
                Log.d(TAG, "Sensor Orientation: $sensorOrientation, Screen Rotation Degrees: $currentScreenRotationDegrees -> JPEG Orientation: $jpegOrientation")

                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash && currentFlashEnabled) {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                } else {
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON) // Hoặc OFF nếu không muốn AE ảnh hưởng
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(s: CameraCaptureSession, req: CaptureRequest, result: TotalCaptureResult) {
                    Log.d(TAG, "Image capture completed.")
                    val currentPreviewSurface = previewSurface
                    if (cameraDevice != null && currentPreviewSurface != null) {
                        startPreviewInternal(s, cameraDevice!!, currentPreviewSurface)
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
                        startPreviewInternal(s, cameraDevice!!, currentPreviewSurface)
                        _cameraState.value = CameraState.Previewing(_optimalPreviewSize.value ?: Size(0,0))
                    } else {
                        _cameraState.value = CameraState.Error("Failed to restart preview post-capture-failure")
                    }
                }
            }, cameraHandler)
        } catch (e: Exception) { // Bắt Exception chung
            Log.e(TAG, "Failed to initiate capture or during capture callback", e)
            coroutineScope.launch {
                _imageCaptureResult.emit(ImageCaptureResult.Error("Capture failed: ${e.localizedMessage}", e))
            }
            _cameraState.value = CameraState.Error("Capture failed: ${e.localizedMessage}", e)
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        // ... (giữ nguyên)
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

    // Hàm này không còn cần thiết vì ta dùng currentScreenRotationDegrees trực tiếp
    // private fun getJpegRotation(deviceOrientation: Int): Int { ... }

    fun setFlashEnabled(enabled: Boolean) {
        // ... (giữ nguyên)
        currentFlashEnabled = enabled
        if (_cameraState.value is CameraState.Previewing && cameraDevice != null && captureSession != null && previewSurface != null) {
            startPreviewInternal(captureSession!!, cameraDevice!!, previewSurface!!)
        }
    }

    fun updateScreenRotation(newScreenRotationValue: Int) { // Nhận Surface.ROTATION_*
        this.currentScreenRotationDegrees = ORIENTATIONS[newScreenRotationValue] ?: 0
        Log.d(TAG, "Screen rotation updated. New Surface.ROTATION value: $newScreenRotationValue, Degrees: $currentScreenRotationDegrees")
    }

    fun releaseCamera() {
        // ... (giữ nguyên)
        Log.i(TAG, "Release camera requested.")
        closeCameraInternal()
        stopBackgroundThreads()
        _cameraState.value = CameraState.Closed
        _optimalPreviewSize.value = null
        Log.i(TAG, "Camera and threads released.")
    }

    private fun closeCameraInternal() {
        // ... (giữ nguyên)
        Log.d(TAG, "Closing camera internal resources...")
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
    }
}