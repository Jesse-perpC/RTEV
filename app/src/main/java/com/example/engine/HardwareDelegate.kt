package com.example.engine

enum class HardwareDelegate(
    val displayName: String,
    val badge: String,
    val description: String
) {
    CPU(
        displayName = "CPU (XNNPACK)",
        badge = "CPU",
        description = "Optimized multi-threaded runtime with XNNPACK kernel acceleration for ARM NEON."
    ),
    GPU(
        displayName = "GPU (OpenCL/GL)",
        badge = "GPU",
        description = "Hardware-accelerated parallel execution via OpenCL and OpenGL ES compute shaders."
    ),
    NNAPI(
        displayName = "NNAPI (NPU/DSP)",
        badge = "NPU",
        description = "Android Neural Networks API routing to hardware NPU, DSP, or Hexagon accelerators."
    )
}
