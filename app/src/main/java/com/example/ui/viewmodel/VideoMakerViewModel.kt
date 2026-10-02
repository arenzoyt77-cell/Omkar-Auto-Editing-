package com.example.ui.viewmodel

import android.app.Application

typealias VideoMakerUiState = VideoProcessingUiState

/**
 * Backwards-compatible alias/subclass for VideoProcessingViewModel.
 */
class VideoMakerViewModel(application: Application) : VideoProcessingViewModel(application)
