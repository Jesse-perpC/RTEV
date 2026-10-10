package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.engine.HardwareDelegate
import com.example.ui.EdgeTensorViewModel
import com.example.ui.components.DelegateSelectorChips
import com.example.ui.components.LatencyDistributionChart
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoVibrant

@Composable
fun ProfilerScreen(
    viewModel: EdgeTensorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedDelegate by viewModel.selectedDelegate.collectAsState()
    val threadCount by viewModel.threadCount.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val benchmarkStats by viewModel.benchmarkStats.collectAsState()
    val comparisonList by viewModel.benchmarkComparisonList.collectAsState()

    var iterationsCount by remember { mutableIntStateOf(50) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Hardware Profiler & Benchmarking",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Empirical latency profiling with statistical percentiles (p50, p95, p99)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Benchmark Target: ${selectedModel.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                )
                Text(
                    text = "${selectedModel.precision}  •  ${selectedModel.estimatedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                DelegateSelectorChips(
                    selectedDelegate = selectedDelegate,
                    onDelegateSelected = { viewModel.selectDelegate(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Iteration Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sample Runs:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(20, 50, 100).forEach { count ->
                        val isSelected = iterationsCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("iterations_chip_$count")
                        ) {
                            Text(
                                text = "${count}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.runBenchmark(iterationsCount, warmUp = 5) },
                        enabled = !isBenchmarking,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_benchmark_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        if (isBenchmarking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Profiler", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.runDelegateShootout(iterations = 30) },
                        enabled = !isBenchmarking,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_shootout_button")
                    ) {
                        Icon(Icons.Default.CompareArrows, contentDescription = "Shootout", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compare All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Latest Benchmark KPI Summary
        benchmarkStats?.let { stats ->
            Text(
                text = "Detailed Latency Profile (${stats.delegate.badge} • ${stats.iterations} iterations)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "MEAN LATENCY",
                    value = String.format("%.2f ms", stats.meanMs),
                    subtitle = "Average execution",
                    accentColor = CyanNeon,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "MEDIAN (P50)",
                    value = String.format("%.2f ms", stats.medianMs),
                    subtitle = "50th percentile",
                    accentColor = IndigoVibrant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "95TH PCT (P95)",
                    value = String.format("%.2f ms", stats.p95Ms),
                    subtitle = "Tail latency bound",
                    accentColor = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "THROUGHPUT",
                    value = String.format("%.1f", stats.throughputIps),
                    subtitle = "Inferences / sec",
                    accentColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "MIN / MAX",
                    value = "${String.format("%.1f", stats.minMs)} / ${String.format("%.1f", stats.maxMs)}",
                    subtitle = "Spread delta (ms)",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "JITTER (STDDEV)",
                    value = String.format("±%.2f ms", stats.stdDevMs),
                    subtitle = "Execution consistency",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Latency Sparkline Curve
            Text(
                text = "Execution Latency Distribution (Run-by-Run)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LatencyDistributionChart(timings = stats.individualTimingsMs)

            Spacer(modifier = Modifier.height(14.dp))

            // Export / Share Benchmark Report Button
            Button(
                onClick = {
                    val report = buildString {
                        appendLine("=== EdgeTensor Hardware Benchmark Report ===")
                        appendLine("Model: ${stats.modelName}")
                        appendLine("Delegate: ${stats.delegate.displayName}")
                        appendLine("Threads: ${stats.threadCount}")
                        appendLine("Sample Size: ${stats.iterations} runs (Warmup: ${stats.warmUpRuns})")
                        appendLine("Mean Latency: ${String.format("%.2f", stats.meanMs)} ms")
                        appendLine("Median (p50): ${String.format("%.2f", stats.medianMs)} ms")
                        appendLine("p95 Latency: ${String.format("%.2f", stats.p95Ms)} ms")
                        appendLine("p99 Latency: ${String.format("%.2f", stats.p99Ms)} ms")
                        appendLine("Throughput: ${String.format("%.1f", stats.throughputIps)} IPS")
                        appendLine("StdDev Jitter: ±${String.format("%.2f", stats.stdDevMs)} ms")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, report)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Benchmark Report"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_benchmark_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Benchmark Report")
            }
        }

        // Accelerator Comparison Shootout Matrix
        if (comparisonList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Hardware Accelerator Shootout Matrix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Side-by-side empirical performance comparison",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val minMean = comparisonList.minOfOrNull { it.meanMs } ?: 1f

            comparisonList.forEach { comp ->
                val ratio = if (minMean > 0f) comp.meanMs / minMean else 1f
                val isWinner = comp.meanMs == minMean

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWinner)
                            EmeraldAccent.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comp.delegate.badge,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (comp.delegate) {
                                        HardwareDelegate.CPU -> IndigoVibrant
                                        HardwareDelegate.GPU -> CyanNeon
                                        HardwareDelegate.NNAPI -> AmberAccent
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${comp.threadCount}T)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isWinner) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚡ FASTEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                            Text(
                                text = "${String.format("%.1f", comp.throughputIps)} IPS  •  p95: ${String.format("%.1f", comp.p95Ms)}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%.2f ms", comp.meanMs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isWinner) EmeraldAccent else MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isWinner) "1.0x baseline" else String.format("+%.1f%% slower", (ratio - 1f) * 100f),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isWinner) EmeraldAccent else AmberAccent
                            )
                        }
                    }
                }
            }
        }
    }
}
