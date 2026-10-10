package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HardwareDelegate
import com.example.ui.EdgeTensorViewModel
import com.example.ui.components.CameraInferencePreview
import com.example.ui.components.ClassificationBar
import com.example.ui.components.DelegateSelectorChips
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoVibrant

@Composable
fun VisionScreen(
    viewModel: EdgeTensorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedDelegate by viewModel.selectedDelegate.collectAsState()
    val threadCount by viewModel.threadCount.collectAsState()
    val latestOutput by viewModel.latestOutput.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val sampleImages = viewModel.sampleImages

    var inputModeIndex by remember { mutableIntStateOf(0) } // 0 = Live Camera, 1 = Sample Images
    var selectedSampleId by remember { mutableStateOf(sampleImages.first().id) }

    // Custom user image picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    bmp?.let { b -> viewModel.classifyBitmap(b) }
                }
            } catch (_: Exception) {}
        }
    }

    // Trigger classification on selected sample image on launch or selection change
    LaunchedEffect(inputModeIndex, selectedSampleId, selectedModel, selectedDelegate) {
        if (inputModeIndex == 1) {
            val sample = sampleImages.firstOrNull { it.id == selectedSampleId } ?: sampleImages.first()
            val bmp = BitmapFactory.decodeResource(context.resources, sample.drawableResId)
            bmp?.let { viewModel.classifyBitmap(it) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Real-Time Edge Vision",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${selectedModel.name} • ${selectedModel.precision}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Delegate Selector
        Text(
            text = "Hardware Acceleration Delegate",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        DelegateSelectorChips(
            selectedDelegate = selectedDelegate,
            onDelegateSelected = { viewModel.selectDelegate(it) }
        )

        // Thread count slider (only when CPU is active)
        AnimatedVisibility(visible = selectedDelegate == HardwareDelegate.CPU) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CPU Thread Count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$threadCount Threads (NEON)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoVibrant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { viewModel.setThreadCount(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoVibrant,
                        activeTrackColor = IndigoVibrant
                    ),
                    modifier = Modifier.testTag("thread_count_slider")
                )
            }
        }

        // Status or Fallback Banner
        AnimatedVisibility(visible = statusBanner != null) {
            statusBanner?.let { banner ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("status_banner_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latestOutput?.isFallback == true)
                            AmberAccent.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (latestOutput?.isFallback == true) Icons.Default.WarningAmber else Icons.Default.PlayArrow,
                            contentDescription = "Status",
                            tint = if (latestOutput?.isFallback == true) AmberAccent else CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Source Tabs
        TabRow(
            selectedTabIndex = inputModeIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = inputModeIndex == 0,
                onClick = { inputModeIndex = 0 },
                text = { Text("Live Camera Feed", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_live_camera")
            )
            Tab(
                selected = inputModeIndex == 1,
                onClick = { inputModeIndex = 1 },
                text = { Text("Sample Benchmark", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Collections, contentDescription = "Samples", modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_sample_benchmark")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Feed Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
        ) {
            if (inputModeIndex == 0) {
                // CameraX Real-time Feed
                CameraInferencePreview(
                    isAnalyzing = true,
                    onFrameAvailable = { frameBmp ->
                        viewModel.classifyBitmap(frameBmp)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Sample image preview
                val currentSample = sampleImages.firstOrNull { it.id == selectedSampleId } ?: sampleImages.first()
                Image(
                    painter = painterResource(id = currentSample.drawableResId),
                    contentDescription = currentSample.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // HUD Overlay: Latency, FPS, Delegate
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lat = latestOutput?.latencyMs ?: 0f
                    val fps = if (lat > 0f) (1000f / lat).coerceAtMost(60f) else 0f

                    Text(
                        text = String.format("%.1f ms", lat),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${String.format("%.0f", fps)} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${latestOutput?.delegateUsed?.badge ?: selectedDelegate.badge}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Sample selector carousel (visible in sample mode)
        AnimatedVisibility(visible = inputModeIndex == 1) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Select Benchmark Asset or Upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleImages) { sample ->
                        val isSelected = sample.id == selectedSampleId
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanNeon else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedSampleId = sample.id
                                }
                                .testTag("sample_chip_${sample.id}")
                        ) {
                            Image(
                                painter = painterResource(id = sample.drawableResId),
                                contentDescription = sample.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            }
                            .testTag("upload_custom_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Classification Results Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("classification_results_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Model Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1001-Class ImageNet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val predictions = latestOutput?.topPredictions.orEmpty()
                if (predictions.isEmpty()) {
                    Text(
                        text = "Awaiting input stream to classify...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    predictions.take(5).forEachIndexed { index, prediction ->
                        ClassificationBar(prediction = prediction, rank = index + 1)
                        if (index < predictions.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Latency KPI metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val lat = latestOutput?.latencyMs ?: 0f
            val ips = if (lat > 0f) 1000f / lat else 0f
            MetricCard(
                title = "INFERENCE",
                value = String.format("%.1f ms", lat),
                subtitle = "Hardware Latency",
                accentColor = CyanNeon,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "THROUGHPUT",
                value = String.format("%.1f", ips),
                subtitle = "Inferences / Sec",
                accentColor = EmeraldAccent,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "DELEGATE",
                value = latestOutput?.delegateUsed?.badge ?: selectedDelegate.badge,
                subtitle = selectedDelegate.displayName,
                accentColor = AmberAccent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
