package com.example.model

import java.util.UUID

data class SplitPoint(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val reason: String = "Semantic Boundary",
    val isAutomatic: Boolean = true,
    val frameIndex: Long = 0L,
    val alignedWord: String? = null
)
