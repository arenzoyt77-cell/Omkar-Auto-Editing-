package com.example.model

enum class MotionMode(val displayName: String, val description: String) {
    REFERENCE_MOTION(
        displayName = "REFERENCE MOTION",
        description = "Applies authoritative motion curve analyzed from YouCut reference"
    ),
    AUTO_MOTION(
        displayName = "AUTO MOTION",
        description = "Multi-keyframe motion synchronized with speech cadence & audio transients"
    ),
    CUSTOM_MOTION(
        displayName = "CUSTOM MOTION",
        description = "Manual user keyframe control with custom scale, position, and curves"
    )
}
