package com.hellena.app.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.hellena.app.storage.StorageHelper

/**
 * Service to screen incoming calls and auto-answer when user is unavailable
 */
class CallScreeningService : CallScreeningService() {
    
    companion object {
        private const val TAG = "CallScreeningService"
    }
    
    private lateinit var storageHelper: StorageHelper
    
    override fun onCreate() {
        super.onCreate()
        storageHelper = StorageHelper(this)
        Log.d(TAG, "CallScreeningService created")
    }
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Screening incoming call from: ${callDetails.handle}")
        
        try {
            // Check if user is available
            val isUserAvailable = storageHelper.isUserAvailable()
            
            if (!isUserAvailable) {
                Log.d(TAG, "User is unavailable, will auto-answer call")
                
                // Allow the call to proceed (don't block it)
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                
                respondToCall(callDetails, response)
                
                // Start the call handling service to answer and record
                val intent = Intent(this, CallHandlingService::class.java).apply {
                    action = CallHandlingService.ACTION_HANDLE_INCOMING_CALL
                    putExtra(CallHandlingService.EXTRA_CALLER_NUMBER, 
                        callDetails.handle?.schemeSpecificPart)
                }
                startForegroundService(intent)
                
            } else {
                Log.d(TAG, "User is available, allowing normal call handling")
                
                // Let the call proceed normally
                val response = CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
                
                respondToCall(callDetails, response)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error screening call", e)
            
            // In case of error, allow the call to proceed normally
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            
            respondToCall(callDetails, response)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallScreeningService destroyed")
    }
}