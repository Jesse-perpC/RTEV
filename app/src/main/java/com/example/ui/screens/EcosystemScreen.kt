package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeviceSpecs
import com.example.ui.EdgeTensorViewModel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoVibrant

data class EdgeFrameworkInfo(
    val title: String,
    val vendor: String,
    val status: String,
    val targetHardware: String,
    val runtime: String,
    val description: String,
    val accentColor: Color
)

@Composable
fun EcosystemScreen(
    viewModel: EdgeTensorViewModel,
    modifier: Modifier = Modifier
) {
    val specs: DeviceSpecs = viewModel.deviceSpecs

    val frameworks = listOf(
        EdgeFrameworkInfo(
            title = "TensorRT-Edge-LLM",
            vendor = "NVIDIA",
            status = "Enterprise Production-Ready",
            targetHardware = "NVIDIA Jetson Orin / Thor / IGX",
            runtime = "C++ Core • CUDA • INT4/FP8 AWQ",
            description = "Highly optimized C++ inference runtime designed specifically for low-latency LLM and vision transformer inference at the edge.",
            accentColor = EmeraldAccent
        ),
        EdgeFrameworkInfo(
            title = "llm-edge-studio",
            vendor = "NXP Semiconductors (nxp-imx-support)",
            status = "Production-Stable Embedded",
            targetHardware = "NXP i.MX95 / i.MX8M Plus",
            runtime = "eIQ Neutron NPU • Edge Toolchain",
            description = "Official NXP UI and tool stack to test, deploy, and accelerate small language models and neural nets on edge intelligence platforms.",
            accentColor = CyanNeon
        ),
        EdgeFrameworkInfo(
            title = "OpenLLM-Studio",
            vendor = "Icecubesaad Open Source",
            status = "Highly Active Local Studio",
            targetHardware = "Offline Edge / Desktops / Edge PC",
            runtime = "Tauri • Rust • React • Vulkan/Metal",
            description = "A local studio built to manage, benchmark, and run edge AI models privately with full hardware acceleration for offline deployments.",
            accentColor = IndigoVibrant
        ),
        EdgeFrameworkInfo(
            title = "LiteRT (TensorFlow Lite)",
            vendor = "Google Open Source",
            status = "Industry Standard Mobile Runtime",
            targetHardware = "Android Mobile • Edge TPUs • Embedded Linux",
            runtime = "Zero-Copy FlatBuffers • XNNPACK • GPU • NNAPI",
            description = "Production-grade edge runtime powering billions of mobile devices with instant memory-mapped model weights and hardware delegates.",
            accentColor = AmberAccent
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Edge AI Ecosystem & Hardware",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Production-ready edge runtimes & real-time device hardware telemetry",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Device Hardware Telemetry Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_telemetry_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = "Hardware",
                        tint = CyanNeon,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Hardware & Silicon Specs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SpecRow("Model & Brand", "${specs.manufacturer} ${specs.deviceModel}")
                SpecRow("OS Version", "Android ${specs.androidVersion} (API ${specs.apiLevel})")
                SpecRow("CPU Architecture", "${specs.cpuAbi} (${specs.cpuCores} Cores)")
                SpecRow("SoC / Board", "${specs.socModel} [${specs.hardwareBoard}]")
                SpecRow("RAM Capacity", "${specs.availRamMb} MB free / ${specs.totalRamMb} MB total")
                SpecRow("OpenGL ES Driver", specs.glEsVersion)
                SpecRow("NNAPI Support", if (specs.isNnapiSupported) "Native Support (API 27+)" else "Unavailable", isHighlight = specs.isNnapiSupported)
                SpecRow("GPU Delegate", if (specs.isGpuDelegateSupported) "Supported (GLES 3.1+)" else "Fallback to CPU", isHighlight = specs.isGpuDelegateSupported)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Enterprise Edge Frameworks Reference
        Text(
            text = "Production-Ready Edge AI Runtimes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Industry-standard frameworks for enterprise edge and offline deployment",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        frameworks.forEach { fw ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(1.dp, fw.accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fw.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = fw.accentColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(fw.accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = fw.vendor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = fw.accentColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Status: ${fw.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = fw.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: ${fw.targetHardware}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Stack: ${fw.runtime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Best Practices for Edge Inference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Best Practices",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Production Edge Optimization Principles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Zero-Copy FlatBuffers: Models are memory-mapped directly from storage without heap allocations.\n" +
                            "• INT8 Quantization (PTQ/QAT): Shrinks weight footprint 4x while speeding up inference 2-3x via ARM NEON SIMD.\n" +
                            "• GPU / NNAPI Delegating: Offloads compute graph from CPU cores to conserve battery and eliminate UI thread blocking.\n" +
                            "• Warm-up Cycles: Mandatory 5-10 warm-up runs to compile OpenCL shader kernels and cache memory pages before latency measurement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SpecRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlight) EmeraldAccent else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}
