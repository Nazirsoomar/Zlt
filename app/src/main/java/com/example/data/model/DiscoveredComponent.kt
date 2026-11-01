package com.example.data.model

data class DiscoveredItem(
    val id: String,
    val name: String,
    val pathOrCmd: String,
    val description: String,
    val observedValue: String,
    val status: String,
    val isTweakable: Boolean,
    val recommendedTweak: String? = null
)

data class PingSample(
    val timestamp: Long,
    val host: String,
    val latencyMs: Float,
    val jitterMs: Float,
    val isLoss: Boolean = false
)
