package com.example.model

import java.util.UUID

/**
 * Data class representing a continuous spoken thought or utterance detected via
 * speech-to-text with word-level timestamps, semantic completion score, and natural pause detection.
 */
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
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
    val wordCount: Int get() = words.size

    val formattedTimeRange: String
        get() = String.format("%.2fs - %.2fs", startMs / 1000f, endMs / 1000f)

    fun findWordAt(timestampMs: Long): WordTimestamp? {
        return words.firstOrNull { timestampMs in it.startMs..it.endMs }
    }
}
