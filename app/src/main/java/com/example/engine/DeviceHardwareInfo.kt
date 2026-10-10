package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class DeviceSpecs(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuAbi: String,
    val cpuCores: Int,
    val hardwareBoard: String,
    val socModel: String,
    val glEsVersion: String,
    val totalRamMb: Long,
    val availRamMb: Long,
    val isNnapiSupported: Boolean,
    val isGpuDelegateSupported: Boolean
)

object DeviceHardwareInfo {
    fun getSpecs(context: Context): DeviceSpecs {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val glEsVersion = actManager?.deviceConfigurationInfo?.glEsVersion ?: "Unknown"

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }

        // NNAPI is natively supported on API 27+
        val isNnapi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

        // GPU delegate requires OpenGL ES 3.1+ or OpenCL
        val isGpuSupported = try {
            val parts = glEsVersion.split(".")
            if (parts.size >= 2) {
                val major = parts[0].toIntOrNull() ?: 3
                val minor = parts[1].toIntOrNull() ?: 0
                major > 3 || (major == 3 && minor >= 1)
            } else true
        } catch (_: Exception) {
            true
        }

        return DeviceSpecs(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            hardwareBoard = Build.BOARD,
            socModel = soc.ifBlank { Build.HARDWARE },
            glEsVersion = glEsVersion,
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            isNnapiSupported = isNnapi,
            isGpuDelegateSupported = isGpuSupported
        )
    }
}
