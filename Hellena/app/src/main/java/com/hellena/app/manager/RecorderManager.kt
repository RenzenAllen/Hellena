package com.hellena.app.manager

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Manager class for handling audio recording functionality
 */
class RecorderManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RecorderManager"
        private const val MAX_RECORDING_DURATION = 30000 // 30 seconds in milliseconds
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    private var currentOutputFile: String? = null
    private var onRecordingCompleteListener: ((String, Long) -> Unit)? = null
    
    /**
     * Start recording audio to the specified file
     */
    fun startRecording(outputFilePath: String, onComplete: (String, Long) -> Unit): Boolean {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return false
        }
        
        return try {
            // Ensure the directory exists
            val outputFile = File(outputFilePath)
            outputFile.parentFile?.mkdirs()
            
            currentOutputFile = outputFilePath
            onRecordingCompleteListener = onComplete
            
            // Create and configure MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFilePath)
                setMaxDuration(MAX_RECORDING_DURATION)
                
                // Set up listener for max duration reached
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d(TAG, "Max recording duration reached")
                        stopRecording()
                    }
                }
                
                prepare()
                start()
            }
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            
            Log.d(TAG, "Recording started: $outputFilePath")
            true
            
        } catch (e: IOException) {
            Log.e(TAG, "Error starting recording", e)
            cleanup()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting recording", e)
            cleanup()
            false
        }
    }
    
    /**
     * Stop the current recording
     */
    fun stopRecording(): Boolean {
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "No recording in progress")
            return false
        }
        
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            
            val duration = System.currentTimeMillis() - recordingStartTime
            val outputFile = currentOutputFile
            
            isRecording = false
            mediaRecorder = null
            
            if (outputFile != null) {
                // Get file size
                val file = File(outputFile)
                val fileSize = if (file.exists()) file.length() else 0L
                
                Log.d(TAG, "Recording stopped. Duration: ${duration}ms, Size: ${fileSize} bytes")
                onRecordingCompleteListener?.invoke(outputFile, duration)
            }
            
            cleanup()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanup()
            false
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean {
        return isRecording
    }
    
    /**
     * Get the current recording duration in milliseconds
     */
    fun getCurrentRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }
    
    /**
     * Cancel the current recording and delete the file
     */
    fun cancelRecording(): Boolean {
        if (!isRecording) {
            return false
        }
        
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            
            // Delete the partially recorded file
            currentOutputFile?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted cancelled recording file: $filePath")
                }
            }
            
            cleanup()
            Log.d(TAG, "Recording cancelled")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling recording", e)
            cleanup()
            false
        }
    }
    
    /**
     * Clean up resources
     */
    private fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            recordingStartTime = 0L
            currentOutputFile = null
            onRecordingCompleteListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        if (isRecording) {
            stopRecording()
        }
        cleanup()
        Log.d(TAG, "RecorderManager released")
    }
    
    /**
     * Check if the device supports audio recording
     */
    fun isRecordingSupported(): Boolean {
        return try {
            val testRecorder = MediaRecorder()
            testRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            testRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            testRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            testRecorder.release()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Recording not supported", e)
            false
        }
    }
}