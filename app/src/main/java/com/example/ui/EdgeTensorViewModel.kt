package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ModelRepository
import com.example.engine.DeviceHardwareInfo
import com.example.engine.DeviceSpecs
import com.example.engine.HardwareDelegate
import com.example.engine.TfliteInferenceEngine
import com.example.model.BenchmarkStats
import com.example.model.InferenceOutput
import com.example.model.ModelItem
import com.example.model.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EdgeTensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModelRepository(application)
    val models: StateFlow<List<ModelItem>> = repository.modelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sampleImages = repository.sampleImages

    private val engine = TfliteInferenceEngine(application)

    val deviceSpecs: DeviceSpecs = DeviceHardwareInfo.getSpecs(application)

    private val _selectedModel = MutableStateFlow(repository.modelsFlow.value.first())
    val selectedModel: StateFlow<ModelItem> = _selectedModel.asStateFlow()

    private val _selectedDelegate = MutableStateFlow(HardwareDelegate.CPU)
    val selectedDelegate: StateFlow<HardwareDelegate> = _selectedDelegate.asStateFlow()

    private val _threadCount = MutableStateFlow(4)
    val threadCount: StateFlow<Int> = _threadCount.asStateFlow()

    private val _inputTensors = MutableStateFlow<List<TensorInfo>>(emptyList())
    val inputTensors: StateFlow<List<TensorInfo>> = _inputTensors.asStateFlow()

    private val _outputTensors = MutableStateFlow<List<TensorInfo>>(emptyList())
    val outputTensors: StateFlow<List<TensorInfo>> = _outputTensors.asStateFlow()

    private val _latestOutput = MutableStateFlow<InferenceOutput?>(null)
    val latestOutput: StateFlow<InferenceOutput?> = _latestOutput.asStateFlow()

    private val _isInferring = MutableStateFlow(false)
    val isInferring: StateFlow<Boolean> = _isInferring.asStateFlow()

    private val _benchmarkStats = MutableStateFlow<BenchmarkStats?>(null)
    val benchmarkStats: StateFlow<BenchmarkStats?> = _benchmarkStats.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _benchmarkComparisonList = MutableStateFlow<List<BenchmarkStats>>(emptyList())
    val benchmarkComparisonList: StateFlow<List<BenchmarkStats>> = _benchmarkComparisonList.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        // Initialize with default model
        reloadEngine()
    }

    fun selectModel(model: ModelItem) {
        if (_selectedModel.value.id == model.id) return
        _selectedModel.value = model
        reloadEngine()
    }

    fun selectDelegate(delegate: HardwareDelegate) {
        if (_selectedDelegate.value == delegate) return
        _selectedDelegate.value = delegate
        reloadEngine()
    }

    fun setThreadCount(count: Int) {
        val clamped = count.coerceIn(1, 8)
        if (_threadCount.value == clamped) return
        _threadCount.value = clamped
        if (_selectedDelegate.value == HardwareDelegate.CPU) {
            reloadEngine()
        }
    }

    private fun reloadEngine() {
        viewModelScope.launch(Dispatchers.Default) {
            val model = _selectedModel.value
            val delegate = _selectedDelegate.value
            val threads = _threadCount.value
            val result = engine.loadModel(model, delegate, threads)
            if (result.isSuccess) {
                _inputTensors.value = engine.getInputTensors()
                _outputTensors.value = engine.getOutputTensors()
                val warning = engine.delegateWarning
                _statusBanner.value = warning ?: "Loaded ${model.name} on ${engine.activeHardwareDelegate.badge}"
            } else {
                _statusBanner.value = "Failed to load model: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun classifyBitmap(bitmap: Bitmap) {
        if (_isInferring.value) return
        _isInferring.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val output = engine.classifyImage(bitmap)
            _latestOutput.value = output
            _isInferring.value = false
        }
    }

    fun runScalarInference(value: Float) {
        viewModelScope.launch(Dispatchers.Default) {
            val output = engine.runScalarInference(value)
            _latestOutput.value = output
        }
    }

    fun runArbitraryTensor(testValues: FloatArray) {
        viewModelScope.launch(Dispatchers.Default) {
            val output = engine.runArbitraryTensor(testValues)
            _latestOutput.value = output
        }
    }

    fun runBenchmark(iterations: Int = 50, warmUp: Int = 5) {
        if (_isBenchmarking.value) return
        _isBenchmarking.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val stats = engine.runBenchmark(iterations, warmUp)
                _benchmarkStats.value = stats
                // Add to comparison list
                val updated = _benchmarkComparisonList.value.filterNot {
                    it.modelName == stats.modelName && it.delegate == stats.delegate && it.threadCount == stats.threadCount
                } + stats
                _benchmarkComparisonList.value = updated
                _statusBanner.value = "Benchmark completed: ${stats.meanMs}ms avg (${String.format("%.1f", stats.throughputIps)} IPS)"
            } catch (e: Exception) {
                _statusBanner.value = "Benchmark failed: ${e.localizedMessage}"
            } finally {
                _isBenchmarking.value = false
            }
        }
    }

    /**
     * Executes side-by-side shootout across CPU (1 thread), CPU (4 threads), GPU, and NNAPI
     */
    fun runDelegateShootout(iterations: Int = 30) {
        if (_isBenchmarking.value) return
        _isBenchmarking.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val results = mutableListOf<BenchmarkStats>()
            val model = _selectedModel.value

            val configurations = listOf(
                Pair(HardwareDelegate.CPU, 1),
                Pair(HardwareDelegate.CPU, 4),
                Pair(HardwareDelegate.GPU, 4),
                Pair(HardwareDelegate.NNAPI, 4)
            )

            for ((delegate, threads) in configurations) {
                engine.loadModel(model, delegate, threads)
                try {
                    val stats = engine.runBenchmark(iterations, warmUpRuns = 5)
                    results.add(stats)
                } catch (e: Exception) {
                    // Skip failed delegate
                }
            }

            // Restore user selected configuration
            engine.loadModel(model, _selectedDelegate.value, _threadCount.value)

            _benchmarkComparisonList.value = results
            _isBenchmarking.value = false
            _statusBanner.value = "Hardware accelerator comparison shootout completed (${results.size} runs)."
        }
    }

    fun importCustomModel(uri: Uri, name: String?) {
        viewModelScope.launch {
            val res = repository.importModelFromUri(uri, name)
            if (res.isSuccess) {
                val imported = res.getOrThrow()
                selectModel(imported)
                _statusBanner.value = "Successfully imported ${imported.name}"
            } else {
                _statusBanner.value = "Model import failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteCustomModel(model: ModelItem) {
        viewModelScope.launch {
            val ok = repository.deleteImportedModel(model)
            if (ok) {
                if (_selectedModel.value.id == model.id) {
                    repository.modelsFlow.value.firstOrNull()?.let { selectModel(it) }
                }
                _statusBanner.value = "Deleted model ${model.name}"
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }
}
