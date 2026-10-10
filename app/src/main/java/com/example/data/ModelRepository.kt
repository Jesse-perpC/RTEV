package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.model.ModelCategory
import com.example.model.ModelItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SampleImageItem(
    val id: String,
    val title: String,
    val description: String,
    val drawableResId: Int
)

class ModelRepository(private val context: Context) {

    private val builtInModels = listOf(
        ModelItem(
            id = "mobilenet_v1_quant",
            name = "MobileNetV1 Quantized",
            subtitle = "Vision Classifier • 224x224 • INT8",
            description = "Authentic 8-bit quantized ImageNet classifier optimized for mobile edge NPUs and ARM NEON SIMD.",
            filename = "mobilenet_v1_1.0_224_quant.tflite",
            isAsset = true,
            category = ModelCategory.VISION_CLASSIFIER,
            precision = "INT8 / UINT8",
            expectedInput = "[1, 224, 224, 3]",
            estimatedSize = "4.1 MB",
            labelsFilename = "labels_imagenet.txt"
        ),
        ModelItem(
            id = "mobilenet_v2_fp32",
            name = "MobileNetV2 FP32",
            subtitle = "Vision Classifier • 224x224 • FP32",
            description = "Full 32-bit floating point ImageNet vision model. Provides the baseline for precision comparison against quantized INT8.",
            filename = "mobilenet_v2_1.0_224.tflite",
            isAsset = true,
            category = ModelCategory.VISION_CLASSIFIER,
            precision = "FP32 Float",
            expectedInput = "[1, 224, 224, 3]",
            estimatedSize = "14.0 MB",
            labelsFilename = "labels_imagenet.txt"
        ),
        ModelItem(
            id = "sine_micro",
            name = "Micro Signal Predictor",
            subtitle = "Continuous Telemetry • 1x1 • Sub-ms",
            description = "Ultra-compact neural network for edge signal analysis and continuous real-time IoT regression.",
            filename = "sine_predictor_micro.tflite",
            isAsset = true,
            category = ModelCategory.SIGNAL_REGRESSION,
            precision = "FP32 Micro",
            expectedInput = "[1, 1]",
            estimatedSize = "3.1 KB",
            labelsFilename = null
        )
    )

    val sampleImages = listOf(
        SampleImageItem("cat", "Tabby Cat", "ImageNet Class #282", R.drawable.sample_cat),
        SampleImageItem("car", "Sports Car", "ImageNet Class #817", R.drawable.sample_sports_car),
        SampleImageItem("mug", "Coffee Mug", "ImageNet Class #504", R.drawable.sample_coffee_mug)
    )

    private val _modelsFlow = MutableStateFlow<List<ModelItem>>(builtInModels)
    val modelsFlow: StateFlow<List<ModelItem>> = _modelsFlow.asStateFlow()

    init {
        loadImportedModels()
    }

    private fun loadImportedModels() {
        val customDir = File(context.filesDir, "custom_models")
        if (customDir.exists()) {
            val customFiles = customDir.listFiles { file -> file.extension.lowercase() == "tflite" } ?: emptyArray()
            val imported = customFiles.map { file ->
                val sizeKb = file.length() / 1024
                val sizeStr = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else "$sizeKb KB"
                ModelItem(
                    id = "custom_${file.name}",
                    name = file.nameWithoutExtension.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    subtitle = "User Imported • $sizeStr",
                    description = "Custom user-imported TensorFlow Lite model stored in sandboxed storage.",
                    filename = "custom_models/${file.name}",
                    isAsset = false,
                    category = ModelCategory.CUSTOM_IMPORTED,
                    precision = "User Defined",
                    expectedInput = "Dynamic Inspection",
                    estimatedSize = sizeStr,
                    labelsFilename = null
                )
            }
            _modelsFlow.value = builtInModels + imported
        }
    }

    suspend fun importModelFromUri(uri: Uri, displayName: String?): Result<ModelItem> = withContext(Dispatchers.IO) {
        try {
            val customDir = File(context.filesDir, "custom_models").apply { mkdirs() }
            val cleanName = (displayName ?: "imported_${System.currentTimeMillis()}.tflite")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetName = if (cleanName.endsWith(".tflite")) cleanName else "$cleanName.tflite"
            val targetFile = File(customDir, targetName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for URI"))

            val sizeKb = targetFile.length() / 1024
            val sizeStr = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else "$sizeKb KB"

            val newItem = ModelItem(
                id = "custom_${targetFile.name}",
                name = targetFile.nameWithoutExtension.replace('_', ' ').replaceFirstChar { it.uppercase() },
                subtitle = "User Imported • $sizeStr",
                description = "Custom user-imported TensorFlow Lite model stored in sandboxed storage.",
                filename = "custom_models/${targetFile.name}",
                isAsset = false,
                category = ModelCategory.CUSTOM_IMPORTED,
                precision = "User Defined",
                expectedInput = "Dynamic Inspection",
                estimatedSize = sizeStr,
                labelsFilename = null
            )

            loadImportedModels()
            Result.success(newItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteImportedModel(modelItem: ModelItem): Boolean {
        if (modelItem.isAsset) return false
        val file = File(context.filesDir, modelItem.filename)
        val deleted = file.delete()
        if (deleted) {
            loadImportedModels()
        }
        return deleted
    }
}
