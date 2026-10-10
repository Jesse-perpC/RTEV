package com.example.model

import com.example.engine.HardwareDelegate

enum class ModelCategory {
    VISION_CLASSIFIER,
    SIGNAL_REGRESSION,
    CUSTOM_IMPORTED
}

data class ModelItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val filename: String,
    val isAsset: Boolean,
    val category: ModelCategory,
    val precision: String, // "INT8 Quantized", "FP32 Float", etc.
    val expectedInput: String, // e.g. "[1, 224, 224, 3]"
    val estimatedSize: String,
    val labelsFilename: String? = null
)

data class TensorInfo(
    val index: Int,
    val name: String,
    val shape: List<Int>,
    val dataType: String,
    val byteSize: Int,
    val quantScale: Float,
    val quantZeroPoint: Int
)

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val classIndex: Int
)

data class BenchmarkStats(
    val modelName: String,
    val delegate: HardwareDelegate,
    val threadCount: Int,
    val iterations: Int,
    val warmUpRuns: Int,
    val minMs: Float,
    val maxMs: Float,
    val meanMs: Float,
    val medianMs: Float,
    val p95Ms: Float,
    val p99Ms: Float,
    val throughputIps: Float,
    val stdDevMs: Float,
    val memoryDeltaMb: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val individualTimingsMs: List<Float> = emptyList()
)

data class InferenceOutput(
    val latencyMs: Float,
    val delegateUsed: HardwareDelegate,
    val topPredictions: List<ClassificationResult> = emptyList(),
    val rawOutputShape: List<Int> = emptyList(),
    val rawOutputPreview: List<Float> = emptyList(),
    val statusMessage: String = "Success",
    val isFallback: Boolean = false
)
