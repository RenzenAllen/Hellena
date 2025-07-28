package com.hellena.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver to restart services after device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or package replaced, checking if service should restart")
                
                // Note: CallScreeningService is automatically managed by the system
                // We don't need to manually restart it here as it's declared in the manifest
                // The system will automatically bind to it when needed
                
                Log.d(TAG, "Boot receiver processing completed")
            }
        }
    }
}