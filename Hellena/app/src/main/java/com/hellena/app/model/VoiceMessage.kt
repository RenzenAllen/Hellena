package com.hellena.app.model

import java.util.Date

/**
 * Data class representing a voice message recorded from an incoming call
 */
data class VoiceMessage(
    val id: String,
    val callerNumber: String?,
    val timestamp: Date,
    val audioFilePath: String,
    val duration: Long = 0L, // Duration in milliseconds
    val isPlayed: Boolean = false,
    val fileSize: Long = 0L // File size in bytes
) {
    /**
     * Get formatted caller display name
     */
    fun getDisplayName(): String {
        return callerNumber ?: "Unknown Number"
    }
    
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        return android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", timestamp).toString()
    }
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            String.format("%d:%02d", minutes, remainingSeconds)
        } else {
            String.format("0:%02d", remainingSeconds)
        }
    }
}