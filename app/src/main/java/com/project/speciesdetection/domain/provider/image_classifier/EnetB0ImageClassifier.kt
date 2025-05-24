package com.project.speciesdetection.domain.provider.image_classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class EnetB0ImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageClassifierProvider {

    private val modelPath = "efficientnetb0_classifier_quant.tflite"
    private val labelPath = "labels.txt"
    private val imageInputWidth = 224
    private val imageInputHeight = 224
    private val numThreads = 4

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputDataType: DataType = DataType.FLOAT32

    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    companion object {
        private const val TAG = "TFLiteImageClassifier"
        private const val MAX_RESULTS = 5
    }

    override suspend fun setup(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelBuffer = loadModelFile(context, modelPath)
            val options = Interpreter.Options().apply { setNumThreads(numThreads) }
            interpreter = Interpreter(modelBuffer, options)
            labels = FileUtil.loadLabels(context, labelPath)

            val inputTensor = interpreter!!.getInputTensor(0)
            inputDataType = inputTensor.dataType()
            Log.i(TAG, "Model input type: $inputDataType, quant params: ${inputTensor.quantizationParams()}")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite interpreter: ${e.message}", e)
            false
        }
    }

    override suspend fun classify(bitmap: Bitmap): List<Recognition> = withContext(Dispatchers.Default) {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter is not initialized.")
            return@withContext emptyList()
        }

        Log.i(TAG, inputDataType.toString())



        val inputBuffer = when (inputDataType) {
            DataType.FLOAT32 -> preprocessFloatInput(bitmap)
            DataType.UINT8 -> preprocessQuantizedInput(bitmap)
            else -> {
                Log.e(TAG, "Unsupported input type: $inputDataType")
                return@withContext emptyList()
            }
        }

        val output = when (interpreter!!.getOutputTensor(0).dataType()) {
            DataType.FLOAT32 -> Array(1) { FloatArray(labels.size) }
            DataType.UINT8 -> Array(1) { ByteArray(labels.size) }
            else -> {
                Log.e(TAG, "Unsupported output type")
                return@withContext emptyList()
            }
        }

        val startTime = SystemClock.uptimeMillis()
        interpreter?.run(inputBuffer, output)
        val endTime = SystemClock.uptimeMillis()
        Log.i(TAG, "Inference time: ${endTime - startTime} ms")

        val probabilities: List<Float> = when (output) {
            is Array<*> -> {
                val out = output[0]
                when (out) {
                    is FloatArray -> out.toList()
                    is ByteArray -> {
                        val quant = interpreter!!.getOutputTensor(0).quantizationParams()
                        out.map { (it.toInt() - quant.zeroPoint) * quant.scale }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }

        val pq = PriorityQueue(MAX_RESULTS, compareByDescending<Recognition> { it.confidence })
        probabilities.forEachIndexed { index, prob ->
            if (index < labels.size) pq.add(Recognition(labels[index], labels[index], prob))
        }

        return@withContext List(min(pq.size, MAX_RESULTS)) { pq.poll() }
    }

    fun preprocessFloatInput(bitmap: Bitmap): ByteBuffer {
        val inputImageWidth = 224
        val inputImageHeight = 224
        val inputChannels = 3

        val imageSize = inputImageWidth * inputImageHeight * inputChannels
        val inputBuffer = ByteBuffer.allocateDirect(4 * imageSize)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Resize ảnh về 224x224, giữ định dạng ARGB_8888
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap.copy(Bitmap.Config.ARGB_8888, true),
            inputImageWidth, inputImageHeight, true
        )

        // Duyệt từng pixel và scale về [0, 1]
        for (y in 0 until inputImageHeight) {
            for (x in 0 until inputImageWidth) {
                val pixel = scaledBitmap.getPixel(x, y)

                val r = Color.red(pixel).toFloat()
                val g = Color.green(pixel).toFloat()
                val b = Color.blue(pixel).toFloat()

                // TensorFlow lưu theo RGB thứ tự (channels-last)
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun preprocessQuantizedInput(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, imageInputWidth, imageInputHeight, true)
        val buffer = ByteBuffer.allocateDirect(imageInputWidth * imageInputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())

        val quant = interpreter!!.getInputTensor(0).quantizationParams()
        val scale = quant.scale
        val zeroPoint = quant.zeroPoint

        for (y in 0 until imageInputHeight) {
            for (x in 0 until imageInputWidth) {
                val pixel = resized.getPixel(x, y)

                val r = (((pixel shr 16) and 0xFF) / scale + zeroPoint).toInt().coerceIn(0, 255)
                val g = (((pixel shr 8) and 0xFF) / scale + zeroPoint).toInt().coerceIn(0, 255)
                val b = ((pixel and 0xFF) / scale + zeroPoint).toInt().coerceIn(0, 255)

                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            }
        }

        buffer.rewind()
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
