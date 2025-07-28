package com.hellena.app.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manager class for handling Text-to-Speech functionality
 */
class TTSManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSManager"
        private const val TTS_UTTERANCE_ID = "hellena_tts"
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechCompleteListener: (() -> Unit)? = null
    
    /**
     * Initialize TTS engine
     */
    fun initialize(onInitComplete: (Boolean) -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English if default locale is not supported
                    textToSpeech?.setLanguage(Locale.ENGLISH)
                }
                
                // Set up utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS started speaking")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS finished speaking")
                        onSpeechCompleteListener?.invoke()
                    }
                    
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "TTS error occurred")
                        onSpeechCompleteListener?.invoke()
                    }
                })
                
                isInitialized = true
                onInitComplete(true)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
                isInitialized = false
                onInitComplete(false)
            }
        }
    }
    
    /**
     * Speak the given text
     */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized || textToSpeech == null) {
            Log.e(TAG, "TTS not initialized")
            onComplete?.invoke()
            return
        }
        
        onSpeechCompleteListener = onComplete
        
        try {
            val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text")
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while speaking", e)
            onComplete?.invoke()
        }
    }
    
    /**
     * Stop current speech
     */
    fun stop() {
        try {
            textToSpeech?.stop()
            onSpeechCompleteListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }
    
    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }
    
    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }
    
    /**
     * Set speech pitch (0.5 = lower pitch, 1.0 = normal, 2.0 = higher pitch)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }
    
    /**
     * Release TTS resources
     */
    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            onSpeechCompleteListener = null
            Log.d(TAG, "TTS shutdown completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS shutdown", e)
        }
    }
    
    /**
     * Check if TTS is available on the device
     */
    fun isTTSAvailable(): Boolean {
        return isInitialized && textToSpeech != null
    }
}