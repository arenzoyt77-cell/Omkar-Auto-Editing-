package com.example.model

data class WordTimestamp(
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float = 0.95f
)
