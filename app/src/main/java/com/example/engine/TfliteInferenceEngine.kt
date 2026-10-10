package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.model.BenchmarkStats
import com.example.model.ClassificationResult
import com.example.model.InferenceOutput
import com.example.model.ModelItem
import com.example.model.TensorInfo
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TfliteInferenceEngine(private val context: Context) : AutoCloseable {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null


    private var currentModelItem: ModelItem? = null
    private var currentDelegate: HardwareDelegate = HardwareDelegate.CPU
    private var currentThreadCount: Int = 4
    private var activeDelegate: HardwareDelegate = HardwareDelegate.CPU
    private var fallbackWarning: String? = null

    private var labels: List<String> = emptyList()

    val isLoaded: Boolean
        get() = interpreter != null

    val activeHardwareDelegate: HardwareDelegate
        get() = activeDelegate

    val delegateWarning: String?
        get() = fallbackWarning

    fun loadModel(
        modelItem: ModelItem,
        delegate: HardwareDelegate = HardwareDelegate.CPU,
        threadCount: Int = 4
    ): Result<String> {
        return try {
            close()
            currentModelItem = modelItem
            currentDelegate = delegate
            currentThreadCount = threadCount
            fallbackWarning = null

            val modelBuffer = loadModelBuffer(modelItem)
            val options = Interpreter.Options()
            options.setNumThreads(threadCount)
            options.setUseXNNPACK(true)

            var delegateApplied = HardwareDelegate.CPU

            when (delegate) {
                HardwareDelegate.GPU -> {
                    try {
                        val gpu = GpuDelegate()
                        options.addDelegate(gpu)
                        gpuDelegate = gpu
                        delegateApplied = HardwareDelegate.GPU
                    } catch (e: Exception) {
                        Log.w("TfliteInferenceEngine", "GPU Delegate unavailable, falling back to CPU", e)
                        fallbackWarning = "GPU accelerator unavailable on current device/driver. Running on CPU (XNNPACK)."
                        delegateApplied = HardwareDelegate.CPU
                    }
                }
                HardwareDelegate.NNAPI -> {
                    try {
                        options.setUseNNAPI(true)
                        delegateApplied = HardwareDelegate.NNAPI
                    } catch (e: Exception) {
                        Log.w("TfliteInferenceEngine", "NNAPI Delegate unavailable, falling back to CPU", e)
                        fallbackWarning = "NNAPI accelerator unavailable. Running on CPU (XNNPACK)."
                        delegateApplied = HardwareDelegate.CPU
                    }
                }

                HardwareDelegate.CPU -> {
                    delegateApplied = HardwareDelegate.CPU
                }
            }

            try {
                interpreter = Interpreter(modelBuffer, options)
                activeDelegate = delegateApplied
            } catch (initErr: Exception) {
                // If delegate crashed inside interpreter creation, retry with CPU fallback
                if (delegateApplied != HardwareDelegate.CPU) {
                    Log.w("TfliteInferenceEngine", "Delegate execution initialization failed, retrying with CPU", initErr)
                    closeDelegates()
                    val cpuOptions = Interpreter.Options()
                    cpuOptions.setNumThreads(threadCount)
                    cpuOptions.setUseXNNPACK(true)
                    interpreter = Interpreter(modelBuffer, cpuOptions)
                    activeDelegate = HardwareDelegate.CPU
                    fallbackWarning = "Hardware delegate initialization failed (${initErr.localizedMessage}). Cleanly fell back to CPU."
                } else {
                    throw initErr
                }
            }

            // Load labels if available
            loadLabelsForModel(modelItem)

            Result.success("Model '${modelItem.name}' loaded successfully on ${activeDelegate.displayName}.")
        } catch (e: Exception) {
            Log.e("TfliteInferenceEngine", "Failed to load model", e)
            Result.failure(e)
        }
    }

    private fun loadModelBuffer(modelItem: ModelItem): ByteBuffer {
        return if (modelItem.isAsset) {
            val assetPath = "models/${modelItem.filename}"
            val fileDescriptor = context.assets.openFd(assetPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } else {
            val file = File(context.filesDir, modelItem.filename)
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }
    }

    private fun loadLabelsForModel(modelItem: ModelItem) {
        val labelFile = modelItem.labelsFilename ?: return
        labels = try {
            context.assets.open("models/$labelFile").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            Log.w("TfliteInferenceEngine", "Failed to load labels: $labelFile", e)
            emptyList()
        }
    }

    fun getInputTensors(): List<TensorInfo> {
        val inter = interpreter ?: return emptyList()
        return (0 until inter.inputTensorCount).map { i ->
            val tensor = inter.getInputTensor(i)
            TensorInfo(
                index = i,
                name = tensor.name(),
                shape = tensor.shape().toList(),
                dataType = tensor.dataType().name,
                byteSize = tensor.numBytes(),
                quantScale = tensor.quantizationParams().scale,
                quantZeroPoint = tensor.quantizationParams().zeroPoint
            )
        }
    }

    fun getOutputTensors(): List<TensorInfo> {
        val inter = interpreter ?: return emptyList()
        return (0 until inter.outputTensorCount).map { i ->
            val tensor = inter.getOutputTensor(i)
            TensorInfo(
                index = i,
                name = tensor.name(),
                shape = tensor.shape().toList(),
                dataType = tensor.dataType().name,
                byteSize = tensor.numBytes(),
                quantScale = tensor.quantizationParams().scale,
                quantZeroPoint = tensor.quantizationParams().zeroPoint
            )
        }
    }

    /**
     * Executes real image classification on a Bitmap
     */
    fun classifyImage(bitmap: Bitmap, topK: Int = 5): InferenceOutput {
        val inter = interpreter ?: return InferenceOutput(
            latencyMs = 0f,
            delegateUsed = activeDelegate,
            statusMessage = "Interpreter not initialized"
        )

        val inputTensor = inter.getInputTensor(0)
        val shape = inputTensor.shape() // [1, height, width, channels]
        val targetHeight = if (shape.size >= 3) shape[1] else 224
        val targetWidth = if (shape.size >= 3) shape[2] else 224
        val channels = if (shape.size >= 4) shape[3] else 3
        val dataType = inputTensor.dataType()

        // Resize bitmap to target dimensions
        val resized = if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        val inputBuffer = ByteBuffer.allocateDirect(
            1 * targetWidth * targetHeight * channels * if (dataType == DataType.FLOAT32) 4 else 1
        ).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val intValues = IntArray(targetWidth * targetHeight)
        resized.getPixels(intValues, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        if (dataType == DataType.FLOAT32) {
            for (pixelValue in intValues) {
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)
                // Normalize to [-1, 1] or [0, 1]
                inputBuffer.putFloat((r - 127.5f) / 127.5f)
                inputBuffer.putFloat((g - 127.5f) / 127.5f)
                inputBuffer.putFloat((b - 127.5f) / 127.5f)
            }
        } else {
            // UINT8 or INT8
            for (pixelValue in intValues) {
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)
                inputBuffer.put(r.toByte())
                inputBuffer.put(g.toByte())
                inputBuffer.put(b.toByte())
            }
        }

        val outputTensor = inter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val numClasses = outputShape.lastOrNull() ?: 1001

        val startTime = System.nanoTime()

        val topPredictions = mutableListOf<ClassificationResult>()
        var previewList = listOf<Float>()

        if (outputTensor.dataType() == DataType.FLOAT32) {
            val outputArray = Array(1) { FloatArray(numClasses) }
            inter.run(inputBuffer, outputArray)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000f

            val scores = outputArray[0]
            val sortedIndices = scores.indices.sortedByDescending { scores[it] }.take(topK)
            for (idx in sortedIndices) {
                val label = labels.getOrElse(idx) { "Class #$idx" }
                topPredictions.add(ClassificationResult(label, scores[idx], idx))
            }
            previewList = sortedIndices.map { scores[it] }

            return InferenceOutput(
                latencyMs = elapsedMs,
                delegateUsed = activeDelegate,
                topPredictions = topPredictions,
                rawOutputShape = outputShape.toList(),
                rawOutputPreview = previewList,
                statusMessage = fallbackWarning ?: "Executed successfully on ${activeDelegate.badge}",
                isFallback = fallbackWarning != null
            )
        } else {
            // Quantized output (e.g. UINT8 0..255 or INT8 -128..127)
            val outputArray = Array(1) { ByteArray(numClasses) }
            inter.run(inputBuffer, outputArray)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000f

            val byteScores = outputArray[0]
            val quantScale = outputTensor.quantizationParams().scale.takeIf { it > 0f } ?: (1f / 255f)
            val zeroPoint = outputTensor.quantizationParams().zeroPoint

            val floatScores = FloatArray(numClasses) { i ->
                val rawVal = byteScores[i].toInt() and 0xFF
                (rawVal - zeroPoint) * quantScale
            }

            val sortedIndices = floatScores.indices.sortedByDescending { floatScores[it] }.take(topK)
            for (idx in sortedIndices) {
                val label = labels.getOrElse(idx) { "Class #$idx" }
                topPredictions.add(ClassificationResult(label, floatScores[idx], idx))
            }
            previewList = sortedIndices.map { floatScores[it] }

            return InferenceOutput(
                latencyMs = elapsedMs,
                delegateUsed = activeDelegate,
                topPredictions = topPredictions,
                rawOutputShape = outputShape.toList(),
                rawOutputPreview = previewList,
                statusMessage = fallbackWarning ?: "Executed successfully on ${activeDelegate.badge}",
                isFallback = fallbackWarning != null
            )
        }
    }

    /**
     * Executes scalar / signal regression (e.g. sine model)
     */
    fun runScalarInference(inputValue: Float): InferenceOutput {
        val inter = interpreter ?: return InferenceOutput(
            latencyMs = 0f,
            delegateUsed = activeDelegate,
            statusMessage = "Interpreter not initialized"
        )

        val inputTensor = inter.getInputTensor(0)
        val inputBuffer = ByteBuffer.allocateDirect(4).apply {
            order(ByteOrder.nativeOrder())
            putFloat(inputValue)
            rewind()
        }

        val outputArray = Array(1) { FloatArray(1) }
        val start = System.nanoTime()
        inter.run(inputBuffer, outputArray)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000f

        val predicted = outputArray[0][0]
        return InferenceOutput(
            latencyMs = elapsedMs,
            delegateUsed = activeDelegate,
            topPredictions = listOf(ClassificationResult("Predicted Output", predicted, 0)),
            rawOutputShape = listOf(1, 1),
            rawOutputPreview = listOf(predicted),
            statusMessage = "Scalar evaluated on ${activeDelegate.badge}"
        )
    }

    /**
     * Executes arbitrary test tensor on loaded model
     */
    fun runArbitraryTensor(testValues: FloatArray): InferenceOutput {
        val inter = interpreter ?: return InferenceOutput(
            latencyMs = 0f,
            delegateUsed = activeDelegate,
            statusMessage = "Interpreter not initialized"
        )

        val inputTensor = inter.getInputTensor(0)
        val numBytes = inputTensor.numBytes()
        val dataType = inputTensor.dataType()

        val inputBuffer = ByteBuffer.allocateDirect(numBytes).apply {
            order(ByteOrder.nativeOrder())
            rewind()
            if (dataType == DataType.FLOAT32) {
                val floatsToPut = numBytes / 4
                for (i in 0 until floatsToPut) {
                    val v = if (testValues.isNotEmpty()) testValues[i % testValues.size] else 0.5f
                    putFloat(v)
                }
            } else {
                for (i in 0 until numBytes) {
                    val v = if (testValues.isNotEmpty()) (testValues[i % testValues.size] * 127).toInt().toByte() else 127.toByte()
                    put(v)
                }
            }
            rewind()
        }

        val outputTensor = inter.getOutputTensor(0)
        val outBytes = outputTensor.numBytes()
        val outputBuffer = ByteBuffer.allocateDirect(outBytes).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        val start = System.nanoTime()
        inter.run(inputBuffer, outputBuffer)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000f

        outputBuffer.rewind()
        val preview = mutableListOf<Float>()
        if (outputTensor.dataType() == DataType.FLOAT32) {
            val count = (outBytes / 4).coerceAtMost(10)
            for (i in 0 until count) {
                preview.add(outputBuffer.float)
            }
        } else {
            val count = outBytes.coerceAtMost(10)
            for (i in 0 until count) {
                preview.add((outputBuffer.get().toInt() and 0xFF).toFloat())
            }
        }

        return InferenceOutput(
            latencyMs = elapsedMs,
            delegateUsed = activeDelegate,
            rawOutputShape = outputTensor.shape().toList(),
            rawOutputPreview = preview,
            statusMessage = "Arbitrary tensor evaluated on ${activeDelegate.badge}"
        )
    }

    /**
     * Runs precise edge performance benchmark with warm-up cycles
     */
    fun runBenchmark(
        iterations: Int = 50,
        warmUpRuns: Int = 5
    ): BenchmarkStats {
        val inter = interpreter ?: throw IllegalStateException("Interpreter not ready")
        val inputTensor = inter.getInputTensor(0)
        val numBytes = inputTensor.numBytes()

        // Allocate dummy input buffer matching tensor byte size
        val inputBuffer = ByteBuffer.allocateDirect(numBytes).apply {
            order(ByteOrder.nativeOrder())
            for (i in 0 until numBytes) {
                put((i % 127).toByte())
            }
            rewind()
        }

        val outputTensor = inter.getOutputTensor(0)
        val outBytes = outputTensor.numBytes()
        val outputBuffer = ByteBuffer.allocateDirect(outBytes).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }

        // Warm up runs
        for (i in 0 until warmUpRuns) {
            inputBuffer.rewind()
            outputBuffer.rewind()
            inter.run(inputBuffer, outputBuffer)
        }

        val timings = ArrayList<Float>(iterations)
        val runtime = Runtime.getRuntime()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()

        for (i in 0 until iterations) {
            inputBuffer.rewind()
            outputBuffer.rewind()
            val start = System.nanoTime()
            inter.run(inputBuffer, outputBuffer)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000f
            timings.add(elapsedMs)
        }

        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        val memDeltaMb = ((memAfter - memBefore).coerceAtLeast(0)) / (1024f * 1024f)

        timings.sort()
        val minMs = timings.first()
        val maxMs = timings.last()
        val meanMs = timings.average().toFloat()
        val medianMs = timings[timings.size / 2]
        val p95Idx = ((timings.size - 1) * 0.95).toInt()
        val p99Idx = ((timings.size - 1) * 0.99).toInt()
        val p95Ms = timings[p95Idx]
        val p99Ms = timings[p99Idx]
        val throughputIps = if (meanMs > 0f) 1000f / meanMs else 0f

        // Standard deviation
        val variance = timings.map { (it - meanMs) * (it - meanMs) }.average()
        val stdDev = sqrt(variance).toFloat()

        return BenchmarkStats(
            modelName = currentModelItem?.name ?: "Unknown Model",
            delegate = activeDelegate,
            threadCount = currentThreadCount,
            iterations = iterations,
            warmUpRuns = warmUpRuns,
            minMs = minMs,
            maxMs = maxMs,
            meanMs = meanMs,
            medianMs = medianMs,
            p95Ms = p95Ms,
            p99Ms = p99Ms,
            throughputIps = throughputIps,
            stdDevMs = stdDev,
            memoryDeltaMb = memDeltaMb,
            individualTimingsMs = timings
        )
    }

    private fun closeDelegates() {
        try {
            gpuDelegate?.close()
        } catch (_: Exception) {}
        gpuDelegate = null
    }


    override fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {}
        interpreter = null
        closeDelegates()
    }
}
