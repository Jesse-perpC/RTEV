package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModelCategory
import com.example.model.ModelItem
import com.example.ui.EdgeTensorViewModel
import com.example.ui.components.TensorTable
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoVibrant

@Composable
fun ModelInspectorScreen(
    viewModel: EdgeTensorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val inputTensors by viewModel.inputTensors.collectAsState()
    val outputTensors by viewModel.outputTensors.collectAsState()
    val latestOutput by viewModel.latestOutput.collectAsState()

    var testScalarInput by remember { mutableFloatStateOf(1.57f) } // PI / 2 ~ 1.57

    // File picker launcher for importing custom .tflite files
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            var fileName: String? = null
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.importCustomModel(it, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Model Catalog & Inspector",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "FlatBuffer tensor architecture inspection & live playground",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Import Custom Model Button
        OutlinedButton(
            onClick = { modelPickerLauncher.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_tflite_model_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = "Import", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Custom .tflite Model", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Model List
        Text(
            text = "Active Models (${models.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        models.forEach { item ->
            val isSelected = item.id == selectedModel.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.selectModel(item) }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("model_card_${item.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        CyanNeon.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isSelected) CyanNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category) {
                                ModelCategory.VISION_CLASSIFIER -> Icons.Default.Layers
                                ModelCategory.SIGNAL_REGRESSION -> Icons.Default.Science
                                ModelCategory.CUSTOM_IMPORTED -> Icons.Default.Memory
                            },
                            contentDescription = item.name,
                            tint = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(CyanNeon, CircleShape)
                                        .size(6.dp)
                                )
                            }
                        }
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!item.isAsset) {
                        IconButton(
                            onClick = { viewModel.deleteCustomModel(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tensor Structure Tables
        Text(
            text = "Active Tensor Geometry: ${selectedModel.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Introspected from model FlatBuffer schema metadata",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        TensorTable(
            title = "INPUT TENSORS (${inputTensors.size})",
            tensors = inputTensors
        )

        Spacer(modifier = Modifier.height(10.dp))

        TensorTable(
            title = "OUTPUT TENSORS (${outputTensors.size})",
            tensors = outputTensors
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Interactive Execution Playground
        Card(
            modifier = Modifier.fillMaxWidth().testTag("interactive_playground_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Interactive Tensor Playground",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent
                )
                Text(
                    text = "Feed synthetic or continuous parameters directly to the model kernel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedModel.category) {
                    ModelCategory.SIGNAL_REGRESSION -> {
                        // Micro Sine Slider
                        Text(
                            text = "Input Angle (Radians): ${String.format("%.2f", testScalarInput)} rad (≈ ${(testScalarInput * 180 / Math.PI).toInt()}°)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = testScalarInput,
                            onValueChange = {
                                testScalarInput = it
                                viewModel.runScalarInference(it)
                            },
                            valueRange = 0f..6.28f,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldAccent,
                                activeTrackColor = EmeraldAccent
                            ),
                            modifier = Modifier.testTag("signal_input_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val pred = latestOutput?.topPredictions?.firstOrNull()?.confidence ?: 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "sin(x) Model Prediction:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.4f", pred),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    ModelCategory.VISION_CLASSIFIER -> {
                        Text(
                            text = "Vision models receive 224x224 RGB tensors from camera or gallery. You can evaluate model speed or test arbitrary numeric arrays below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.runArbitraryTensor(floatArrayOf(0.1f, 0.5f, 0.9f, 0.2f))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoVibrant),
                            modifier = Modifier.testTag("run_synthetic_eval_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Synthetic Tensor Pass")
                        }
                    }

                    ModelCategory.CUSTOM_IMPORTED -> {
                        Text(
                            text = "Evaluate custom imported tensor with dynamically generated test vectors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.runArbitraryTensor(floatArrayOf(1.0f, 0.0f, -1.0f, 0.5f))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.testTag("run_custom_eval_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Custom Model Pass")
                        }
                    }
                }

                latestOutput?.let { out ->
                    if (out.rawOutputPreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Raw Kernel Output Vectors (first ${out.rawOutputPreview.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = out.rawOutputPreview.joinToString(", ") { String.format("%.3f", it) },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanNeon
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latency: ${String.format("%.2f ms", out.latencyMs)}  •  ${out.statusMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
