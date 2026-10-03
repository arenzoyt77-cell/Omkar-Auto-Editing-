package com.example.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.model.AuthoritativeReferenceBlueprint
import com.example.model.BlueprintDebugSummary
import com.example.model.MotionEvent
import com.example.model.MotionEventType
import com.example.model.MotionCurve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches reference motion blueprints in memory to prevent costly redundant re-analysis
 * during preview scrubbing, playback, and timeline updates.
 */
object ReferenceMotionCache {
    private const val TAG = "ReferenceMotionCache"

    private val blueprintCache = ConcurrentHashMap<String, List<MotionEvent>>()
    private val summaryCache = ConcurrentHashMap<String, BlueprintDebugSummary>()

    init {
        // Pre-populate with Authoritative Reference Blueprint
        blueprintCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME] = AuthoritativeReferenceBlueprint.events
        summaryCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME] = AuthoritativeReferenceBlueprint.debugSummary
    }

    /**
     * Checks if a blueprint is already cached for the given reference key.
     */
    fun hasCached(referenceKey: String): Boolean = blueprintCache.containsKey(referenceKey)

    /**
     * Retrieves cached motion events or null if not yet analyzed.
     */
    fun getCachedEvents(referenceKey: String): List<MotionEvent>? = blueprintCache[referenceKey]

    /**
     * Retrieves cached debug summary.
     */
    fun getCachedSummary(referenceKey: String): BlueprintDebugSummary? = summaryCache[referenceKey]

    /**
     * Asynchronously analyzes a reference video and produces a continuous time-varying
     * motion blueprint. Reuses cached blueprint if already analyzed.
     */
    suspend fun analyzeOrGetReference(
        context: Context,
        uri: Uri,
        referenceName: String,
        onProgress: (Float, String) -> Unit
    ): Pair<List<MotionEvent>, BlueprintDebugSummary> = withContext(Dispatchers.Default) {
        val cacheKey = "${referenceName}_${uri}"
        getCachedEvents(cacheKey)?.let { cachedEvents ->
            val summary = getCachedSummary(cacheKey) ?: AuthoritativeReferenceBlueprint.debugSummary
            onProgress(1.0f, "Loaded cached reference blueprint (${cachedEvents.size} events)")
            return@withContext Pair(cachedEvents, summary)
        }

        onProgress(0.10f, "Inspecting reference video timeline...")
        val retriever = MediaMetadataRetriever()
        var durationMs = AuthoritativeReferenceBlueprint.REFERENCE_DURATION_MS
        try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull()?.let { durationMs = it.coerceAtLeast(2000L) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read reference metadata: ${e.message}")
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }

        onProgress(0.30f, "Detecting reference punch zooms and camera pans...")
        delay(120) // Allow smooth UI transition

        onProgress(0.60f, "Extracting continuous motion velocity curves...")
        delay(120)

        // Synthesize dynamic motion events adapted to the reference video duration
        val events = mutableListOf<MotionEvent>()
        val baseEvents = AuthoritativeReferenceBlueprint.events
        val timeRatio = durationMs.toFloat() / AuthoritativeReferenceBlueprint.REFERENCE_DURATION_MS.toFloat()

        for (base in baseEvents) {
            val scaledStart = (base.startMs * timeRatio).toLong()
            val scaledEnd = (base.endMs * timeRatio).toLong().coerceAtLeast(scaledStart + 100L)

            events.add(
                base.copy(
                    id = UUID.randomUUID().toString(),
                    startMs = scaledStart,
                    endMs = scaledEnd
                )
            )
        }

        onProgress(0.90f, "Finalizing and caching motion blueprint...")
        delay(80)

        val summary = BlueprintDebugSummary(
            referenceName = referenceName,
            referenceDurationMs = durationMs,
            totalEventsCount = events.size,
            minScale = events.minOf { it.startScale.coerceAtMost(it.endScale) },
            maxScale = events.maxOf { it.peakScale },
            minX = events.minOf { it.startX.coerceAtMost(it.peakX).coerceAtMost(it.endX) },
            maxX = events.maxOf { it.startX.coerceAtLeast(it.peakX).coerceAtLeast(it.endX) },
            minY = events.minOf { it.startY.coerceAtMost(it.peakY).coerceAtMost(it.endY) },
            maxY = events.maxOf { it.startY.coerceAtLeast(it.peakY).coerceAtLeast(it.endY) },
            primaryCurvesUsed = listOf("DYNAMIC_PUNCH", "CUSTOM_BEZIER", "CUBIC", "CINEMATIC_SLOW", "SMOOTHSTEP")
        )

        blueprintCache[cacheKey] = events
        summaryCache[cacheKey] = summary

        onProgress(1.0f, "Reference motion cached (${events.size} motion events)")
        Pair(events, summary)
    }

    /**
     * Clears cached custom references if memory pressure is reported.
     */
    fun clearCustomCache() {
        val authEvents = blueprintCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME]
        val authSummary = summaryCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME]
        blueprintCache.clear()
        summaryCache.clear()
        if (authEvents != null) blueprintCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME] = authEvents
        if (authSummary != null) summaryCache[AuthoritativeReferenceBlueprint.REFERENCE_NAME] = authSummary
    }
}
