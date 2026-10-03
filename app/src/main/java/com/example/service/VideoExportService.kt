package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.VideoProject
import java.io.File

/**
 * High-level VideoExportService forwarding to FastVideoExportService
 * for hardware-accelerated, high-FPS video export.
 */
class VideoExportService(private val context: Context) {

    private val fastExportService = FastVideoExportService(context)

    fun cancelExport() {
        fastExportService.cancelExport()
    }

    suspend fun exportProject(
        project: VideoProject,
        onProgressUpdate: (ExportProgressUpdate) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (File, Uri) -> Unit
    ) {
        fastExportService.exportProject(
            project = project,
            onProgress = onProgressUpdate,
            onError = onError,
            onSuccess = onSuccess
        )
    }

    suspend fun exportProject(
        project: VideoProject,
        onProgress: (Float, String) -> Unit,
        onError: (String) -> Unit,
        onSuccess: (File, Uri) -> Unit
    ) {
        fastExportService.exportProject(
            project = project,
            onProgress = { update ->
                onProgress(update.progress, update.stage)
            },
            onError = onError,
            onSuccess = onSuccess
        )
    }
}
