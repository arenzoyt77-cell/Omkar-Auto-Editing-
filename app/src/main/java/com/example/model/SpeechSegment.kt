package com.example.model

import java.util.UUID

data class SpeechSegment(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val words: List<WordTimestamp> = emptyList(),
    val isCompleteThought: Boolean = true,
    val semanticScore: Float = 0.9f,
    val pauseDurationAfterMs: Long = 0L,
    val confidence: Float = 0.95f
)
