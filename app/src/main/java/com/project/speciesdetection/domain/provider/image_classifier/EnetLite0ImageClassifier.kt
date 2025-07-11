package com.project.speciesdetection.domain.provider.image_classifier

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import org.tensorflow.lite.gpu.CompatibilityList

@Singleton
class EnetLite0ImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageClassifierProvider {

    private val modelPath = ""//"organism_classifier_quant.tflite"
    private val labelPath = "labels.txt"
    private val imageInputWidth = 224
    private val imageInputHeight = 224
    private val imageInputChannels = 3
    private val numThreads = 4
    private val useNNApi: Boolean = true //false khi dùng EB0
    private val useGPU: Boolean = false

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputDataType: DataType? = null
    private var quantParams: Pair<Float, Int>? = null

    // Delegates cho hardware acceleration
    private var nnApiDelegate: NnApiDelegate? = null
    private var gpuDelegate: GpuDelegate? = null

    companion object {
        private const val TAG = "TFLiteImageClassifier"
        private const val MAX_RESULTS = 5
    }

    override suspend fun setup(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelBuffer = loadModelFile(context, modelPath)
            interpreter = Interpreter(modelBuffer, Interpreter.Options().apply {
                setNumThreads(numThreads)
                var delegateAdded = false

                // Ưu tiên NNAPI (Neural Networks API)
                if (useNNApi) {
                    try {
                        nnApiDelegate = NnApiDelegate()
                        addDelegate(nnApiDelegate)
                        Log.i(TAG, "NNAPI delegate added successfully.")
                        delegateAdded = true
                    } catch (e: Exception) {
                        Log.w(TAG, "NNAPI delegate failed to initialize: ${e.message}")
                        nnApiDelegate?.close() // Đảm bảo giải phóng nếu khởi tạo lỗi
                        nnApiDelegate = null
                    }
                }

                // Nếu không dùng NNAPI hoặc NNAPI thất bại, thử GPU
                if (!delegateAdded && useGPU) {
                    val compatList = CompatibilityList() // Vẫn cần để kiểm tra isDelegateSupportedOnThisDevice
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        try {
                            gpuDelegate = GpuDelegate() // Khởi tạo không có options
                            addDelegate(gpuDelegate)
                            Log.i(TAG, "GPU delegate added (using default options).")
                            delegateAdded = true
                        } catch (e: Exception) {
                            Log.w(TAG, "GPU delegate (default options) failed to initialize: ${e.message}")
                            gpuDelegate?.close()
                            gpuDelegate = null
                        }
                    } else {
                        Log.w(TAG, "GPU delegate is not supported on this device.")
                    }
                }

                if (!delegateAdded) {
                    Log.i(TAG, "No hardware delegate added. Using CPU with $numThreads threads.")
                }
                //setUseNNAPI(true)
            })

            labels = FileUtil.loadLabels(context, labelPath)
            val inputTensor = interpreter!!.getInputTensor(0)
            inputDataType = inputTensor.dataType()
            val quant = inputTensor.quantizationParams()
            if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                quantParams = Pair(quant.scale, quant.zeroPoint)
            }

            Log.i(TAG, "Model and labels loaded. InputType: $inputDataType Quant: $quantParams")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite interpreter: ${e.message}", e)
            false
        }
    }

    override suspend fun classify(bitmap: Bitmap): List<Recognition> = withContext(Dispatchers.Default) {
        if (interpreter == null || inputDataType == null) {
            Log.e(TAG, "Interpreter is not initialized.")
            return@withContext emptyList()
        }

        val inputBuffer = when (inputDataType) {
            DataType.FLOAT32 -> convertBitmapUsingNormalizeOp(bitmap)
            DataType.UINT8, DataType.INT8 -> convertBitmapToQuantizedBuffer(bitmap)
            else -> throw IllegalArgumentException("Unsupported input type: $inputDataType")
        }

        val output = Array(1) { FloatArray(labels.size) }

        Log.i(TAG, output.toString())

        val startTime = SystemClock.uptimeMillis()
        interpreter?.run(inputBuffer, output)
        val endTime = SystemClock.uptimeMillis()
        Log.i(TAG, "Inference time: ${endTime - startTime} ms")

        val pq = PriorityQueue(MAX_RESULTS, compareByDescending<Recognition> { it.confidence })
        output[0].forEachIndexed { index, confidence ->
            if (index < labels.size) {
                pq.add(Recognition(labels[index], labels[index], confidence))
            }
        }

        return@withContext List(min(pq.size, MAX_RESULTS)) { pq.poll() }
    }

    private fun convertBitmapUsingNormalizeOp(bitmap: Bitmap): ByteBuffer {

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(imageInputHeight, imageInputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) //127.5f
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = imageProcessor.process(tensorImage)
        return processedImage.buffer
        /*
        val resized = Bitmap.createScaledBitmap(bitmap, imageInputWidth, imageInputHeight, true)
        val inputBuffer = ByteBuffer.allocateDirect(4 * imageInputWidth * imageInputHeight * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until imageInputHeight) {
            for (x in 0 until imageInputWidth) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF) / 127.5f - 1.0f
                val g = ((pixel shr 8) and 0xFF) / 127.5f - 1.0f
                val b = (pixel and 0xFF) / 127.5f - 1.0f

                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
        }

        inputBuffer.rewind()
        return inputBuffer*/
    }

    private fun convertBitmapToQuantizedBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(imageInputWidth * imageInputHeight * imageInputChannels)
        buffer.order(ByteOrder.nativeOrder())

        val resized = Bitmap.createScaledBitmap(bitmap, imageInputWidth, imageInputHeight, true)
        val pixels = IntArray(imageInputWidth * imageInputHeight)
        resized.getPixels(pixels, 0, imageInputWidth, 0, 0, imageInputWidth, imageInputHeight)

        val (scale, zeroPoint) = quantParams ?: Pair(1f, 0)

        for (y in 0 until imageInputHeight) {
            for (x in 0 until imageInputWidth) {
                val index = y * imageInputWidth + x
                val pixel = pixels[index]

                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                buffer.put(((r / scale) + zeroPoint).toInt().toByte())
                buffer.put(((g / scale) + zeroPoint).toInt().toByte())
                buffer.put(((b / scale) + zeroPoint).toInt().toByte())
            }
        }

        return buffer
    }

    private fun loadModelFile(context: Context, path: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(path)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }
}
