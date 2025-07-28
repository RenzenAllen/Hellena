package com.hellena.app.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.hellena.app.model.VoiceMessage
import java.io.File
import java.util.Date
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper class for managing local storage of voice messages and app settings
 */
class StorageHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "StorageHelper"
        private const val PREFS_NAME = "hellena_prefs"
        private const val KEY_VOICE_MESSAGES = "voice_messages"
        private const val KEY_IS_AVAILABLE = "is_available"
        private const val KEY_USE_TTS = "use_tts"
        private const val KEY_CUSTOM_MESSAGE = "custom_message"
        private const val AUDIO_DIR = "voice_messages"
    }
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Get the directory for storing audio files
     */
    fun getAudioDirectory(): File {
        val audioDir = File(context.filesDir, AUDIO_DIR)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }
    
    /**
     * Generate a unique filename for a new voice message
     */
    fun generateAudioFileName(callerNumber: String?): String {
        val timestamp = System.currentTimeMillis()
        val caller = callerNumber?.replace("[^\\d]".toRegex(), "") ?: "unknown"
        return "voice_${timestamp}_${caller}.3gp"
    }
    
    /**
     * Save a voice message to storage
     */
    fun saveVoiceMessage(voiceMessage: VoiceMessage): Boolean {
        return try {
            val messages = getVoiceMessages().toMutableList()
            messages.add(0, voiceMessage) // Add to beginning for chronological order
            
            val jsonArray = JSONArray()
            messages.forEach { message ->
                val jsonObject = JSONObject().apply {
                    put("id", message.id)
                    put("callerNumber", message.callerNumber ?: "")
                    put("timestamp", message.timestamp.time)
                    put("audioFilePath", message.audioFilePath)
                    put("duration", message.duration)
                    put("isPlayed", message.isPlayed)
                    put("fileSize", message.fileSize)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPrefs.edit()
                .putString(KEY_VOICE_MESSAGES, jsonArray.toString())
                .apply()
            
            Log.d(TAG, "Voice message saved successfully: ${voiceMessage.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving voice message", e)
            false
        }
    }
    
    /**
     * Get all voice messages from storage
     */
    fun getVoiceMessages(): List<VoiceMessage> {
        return try {
            val jsonString = sharedPrefs.getString(KEY_VOICE_MESSAGES, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonString)
            val messages = mutableListOf<VoiceMessage>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val message = VoiceMessage(
                    id = jsonObject.getString("id"),
                    callerNumber = jsonObject.getString("callerNumber").takeIf { it.isNotEmpty() },
                    timestamp = Date(jsonObject.getLong("timestamp")),
                    audioFilePath = jsonObject.getString("audioFilePath"),
                    duration = jsonObject.optLong("duration", 0L),
                    isPlayed = jsonObject.optBoolean("isPlayed", false),
                    fileSize = jsonObject.optLong("fileSize", 0L)
                )
                messages.add(message)
            }
            
            messages
        } catch (e: Exception) {
            Log.e(TAG, "Error loading voice messages", e)
            emptyList()
        }
    }
    
    /**
     * Mark a voice message as played
     */
    fun markMessageAsPlayed(messageId: String): Boolean {
        return try {
            val messages = getVoiceMessages().map { message ->
                if (message.id == messageId) {
                    message.copy(isPlayed = true)
                } else {
                    message
                }
            }
            
            val jsonArray = JSONArray()
            messages.forEach { message ->
                val jsonObject = JSONObject().apply {
                    put("id", message.id)
                    put("callerNumber", message.callerNumber ?: "")
                    put("timestamp", message.timestamp.time)
                    put("audioFilePath", message.audioFilePath)
                    put("duration", message.duration)
                    put("isPlayed", message.isPlayed)
                    put("fileSize", message.fileSize)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPrefs.edit()
                .putString(KEY_VOICE_MESSAGES, jsonArray.toString())
                .apply()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as played", e)
            false
        }
    }
    
    /**
     * Delete a voice message and its audio file
     */
    fun deleteVoiceMessage(messageId: String): Boolean {
        return try {
            val messages = getVoiceMessages()
            val messageToDelete = messages.find { it.id == messageId }
            
            // Delete audio file
            messageToDelete?.let { message ->
                val audioFile = File(message.audioFilePath)
                if (audioFile.exists()) {
                    audioFile.delete()
                }
            }
            
            // Remove from list
            val updatedMessages = messages.filter { it.id != messageId }
            
            val jsonArray = JSONArray()
            updatedMessages.forEach { message ->
                val jsonObject = JSONObject().apply {
                    put("id", message.id)
                    put("callerNumber", message.callerNumber ?: "")
                    put("timestamp", message.timestamp.time)
                    put("audioFilePath", message.audioFilePath)
                    put("duration", message.duration)
                    put("isPlayed", message.isPlayed)
                    put("fileSize", message.fileSize)
                }
                jsonArray.put(jsonObject)
            }
            
            sharedPrefs.edit()
                .putString(KEY_VOICE_MESSAGES, jsonArray.toString())
                .apply()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting voice message", e)
            false
        }
    }
    
    // App Settings
    
    /**
     * Set whether the user is available (true) or unavailable (false)
     */
    fun setUserAvailable(isAvailable: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_IS_AVAILABLE, isAvailable)
            .apply()
    }
    
    /**
     * Check if user is currently available
     */
    fun isUserAvailable(): Boolean {
        return sharedPrefs.getBoolean(KEY_IS_AVAILABLE, true) // Default to available
    }
    
    /**
     * Set whether to use TTS (true) or pre-recorded message (false)
     */
    fun setUseTTS(useTTS: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_USE_TTS, useTTS)
            .apply()
    }
    
    /**
     * Check if TTS should be used
     */
    fun shouldUseTTS(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_TTS, true) // Default to TTS
    }
    
    /**
     * Set custom TTS message
     */
    fun setCustomMessage(message: String) {
        sharedPrefs.edit()
            .putString(KEY_CUSTOM_MESSAGE, message)
            .apply()
    }
    
    /**
     * Get custom TTS message
     */
    fun getCustomMessage(): String {
        return sharedPrefs.getString(KEY_CUSTOM_MESSAGE, 
            "Hi, I'm currently unavailable. Please leave a message.") ?: 
            "Hi, I'm currently unavailable. Please leave a message."
    }
    
    /**
     * Create a new voice message object
     */
    fun createVoiceMessage(callerNumber: String?, audioFilePath: String, duration: Long, fileSize: Long): VoiceMessage {
        return VoiceMessage(
            id = UUID.randomUUID().toString(),
            callerNumber = callerNumber,
            timestamp = Date(),
            audioFilePath = audioFilePath,
            duration = duration,
            fileSize = fileSize
        )
    }
}