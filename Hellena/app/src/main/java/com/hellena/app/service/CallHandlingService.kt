package com.hellena.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hellena.app.R
import com.hellena.app.manager.RecorderManager
import com.hellena.app.manager.TTSManager
import com.hellena.app.storage.StorageHelper
import com.hellena.app.ui.MainActivity
import java.io.File

/**
 * Foreground service that handles incoming calls, plays messages, and records responses
 */
class CallHandlingService : Service() {
    
    companion object {
        private const val TAG = "CallHandlingService"
        const val ACTION_HANDLE_INCOMING_CALL = "com.hellena.app.HANDLE_INCOMING_CALL"
        const val ACTION_STOP_SERVICE = "com.hellena.app.STOP_SERVICE"
        const val EXTRA_CALLER_NUMBER = "caller_number"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hellena_call_service"
        private const val ANSWER_DELAY_MS = 2000L // 2 seconds delay before answering
        private const val MESSAGE_DELAY_MS = 1000L // 1 second delay before playing message
    }
    
    private lateinit var storageHelper: StorageHelper
    private lateinit var ttsManager: TTSManager
    private lateinit var recorderManager: RecorderManager
    private lateinit var audioManager: AudioManager
    private lateinit var telecomManager: TelecomManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var currentCallerNumber: String? = null
    private var isHandlingCall = false
    
    override fun onCreate() {
        super.onCreate()
        
        storageHelper = StorageHelper(this)
        ttsManager = TTSManager(this)
        recorderManager = RecorderManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        
        createNotificationChannel()
        
        // Initialize TTS
        ttsManager.initialize { success ->
            if (success) {
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
        
        Log.d(TAG, "CallHandlingService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANDLE_INCOMING_CALL -> {
                currentCallerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER)
                Log.d(TAG, "Handling incoming call from: $currentCallerNumber")
                
                startForeground(NOTIFICATION_ID, createNotification("Handling incoming call"))
                handleIncomingCall()
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Stopping service")
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun handleIncomingCall() {
        if (isHandlingCall) {
            Log.w(TAG, "Already handling a call")
            return
        }
        
        isHandlingCall = true
        
        // Wait a bit before answering to ensure call is fully established
        handler.postDelayed({
            answerCall()
        }, ANSWER_DELAY_MS)
    }
    
    private fun answerCall() {
        try {
            Log.d(TAG, "Attempting to answer call")
            
            // Try to answer the call using TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.acceptRingingCall()
                Log.d(TAG, "Call answered using TelecomManager")
            }
            
            // Set audio mode for call
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = false
            
            // Wait a moment for call to be established, then play message
            handler.postDelayed({
                playGreetingMessage()
            }, MESSAGE_DELAY_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
            // If we can't answer, still try to play message in case call gets answered manually
            handler.postDelayed({
                playGreetingMessage()
            }, MESSAGE_DELAY_MS)
        }
    }
    
    private fun playGreetingMessage() {
        try {
            updateNotification("Playing greeting message")
            
            val shouldUseTTS = storageHelper.shouldUseTTS()
            
            if (shouldUseTTS && ttsManager.isTTSAvailable()) {
                val message = storageHelper.getCustomMessage()
                Log.d(TAG, "Playing TTS message: $message")
                
                ttsManager.speak(message) {
                    Log.d(TAG, "TTS message completed, starting recording")
                    startRecording()
                }
            } else {
                // For now, if no pre-recorded message is available, use TTS as fallback
                val fallbackMessage = "Hi, I'm currently unavailable. Please leave a message."
                Log.d(TAG, "Playing fallback TTS message")
                
                ttsManager.speak(fallbackMessage) {
                    Log.d(TAG, "Fallback message completed, starting recording")
                    startRecording()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing greeting message", e)
            // If message fails, still try to record
            startRecording()
        }
    }
    
    private fun startRecording() {
        try {
            updateNotification("Recording voice message")
            
            val audioDir = storageHelper.getAudioDirectory()
            val fileName = storageHelper.generateAudioFileName(currentCallerNumber)
            val outputFile = File(audioDir, fileName)
            
            Log.d(TAG, "Starting recording to: ${outputFile.absolutePath}")
            
            val success = recorderManager.startRecording(outputFile.absolutePath) { filePath, duration ->
                Log.d(TAG, "Recording completed: $filePath, duration: ${duration}ms")
                onRecordingComplete(filePath, duration)
            }
            
            if (!success) {
                Log.e(TAG, "Failed to start recording")
                completeCallHandling()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            completeCallHandling()
        }
    }
    
    private fun onRecordingComplete(filePath: String, duration: Long) {
        try {
            val file = File(filePath)
            val fileSize = if (file.exists()) file.length() else 0L
            
            // Create and save voice message
            val voiceMessage = storageHelper.createVoiceMessage(
                currentCallerNumber, 
                filePath, 
                duration, 
                fileSize
            )
            
            val saved = storageHelper.saveVoiceMessage(voiceMessage)
            if (saved) {
                Log.d(TAG, "Voice message saved successfully")
                updateNotification("Voice message saved")
            } else {
                Log.e(TAG, "Failed to save voice message")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recorded message", e)
        }
        
        completeCallHandling()
    }
    
    private fun completeCallHandling() {
        Log.d(TAG, "Completing call handling")
        
        try {
            // Stop any ongoing recording
            recorderManager.release()
            
            // Stop TTS if still speaking
            ttsManager.stop()
            
            // Reset audio mode
            audioManager.mode = AudioManager.MODE_NORMAL
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
        
        isHandlingCall = false
        currentCallerNumber = null
        
        // Stop the service after a short delay
        handler.postDelayed({
            stopSelf()
        }, 2000L)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hellena Call Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles incoming calls when unavailable"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, CallHandlingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hellena")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_phone)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        try {
            recorderManager.release()
            ttsManager.shutdown()
            
            // Reset audio mode
            audioManager.mode = AudioManager.MODE_NORMAL
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during service destruction", e)
        }
        
        Log.d(TAG, "CallHandlingService destroyed")
    }
}