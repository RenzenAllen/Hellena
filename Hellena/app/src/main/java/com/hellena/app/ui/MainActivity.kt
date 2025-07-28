package com.hellena.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hellena.app.R
import com.hellena.app.storage.StorageHelper

/**
 * Main activity for Hellena app
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
        )
    }
    
    private lateinit var storageHelper: StorageHelper
    private lateinit var voiceMessageAdapter: VoiceMessageAdapter
    
    private lateinit var availabilitySwitch: SwitchMaterial
    private lateinit var ttsSwitch: SwitchMaterial
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var fabSettings: FloatingActionButton
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.isEmpty()) {
            Log.d(TAG, "All permissions granted")
            setupCallScreeningService()
        } else {
            Log.w(TAG, "Some permissions were denied: $deniedPermissions")
            showPermissionDeniedDialog(deniedPermissions.toList())
        }
    }
    
    // Call screening service setup launcher
    private val callScreeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkCallScreeningServiceStatus()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        storageHelper = StorageHelper(this)
        
        initializeViews()
        setupRecyclerView()
        setupListeners()
        
        // Check permissions on startup
        checkAndRequestPermissions()
        
        // Load voice messages
        refreshVoiceMessages()
    }
    
    private fun initializeViews() {
        availabilitySwitch = findViewById(R.id.switch_availability)
        ttsSwitch = findViewById(R.id.switch_tts)
        recyclerView = findViewById(R.id.recycler_voice_messages)
        emptyStateText = findViewById(R.id.tv_empty_state)
        fabSettings = findViewById(R.id.fab_settings)
        
        // Set initial switch states
        availabilitySwitch.isChecked = !storageHelper.isUserAvailable() // Switch shows "unavailable" state
        ttsSwitch.isChecked = storageHelper.shouldUseTTS()
    }
    
    private fun setupRecyclerView() {
        voiceMessageAdapter = VoiceMessageAdapter(
            messages = emptyList(),
            onMessagePlayed = { messageId ->
                storageHelper.markMessageAsPlayed(messageId)
                refreshVoiceMessages()
            },
            onMessageDelete = { messageId ->
                showDeleteConfirmationDialog(messageId)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = voiceMessageAdapter
        }
    }
    
    private fun setupListeners() {
        // Availability switch - when ON, user is UNAVAILABLE
        availabilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            storageHelper.setUserAvailable(!isChecked) // Invert because switch shows unavailable state
            val status = if (isChecked) "unavailable" else "available"
            Toast.makeText(this, "You are now $status", Toast.LENGTH_SHORT).show()
        }
        
        // TTS switch
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            storageHelper.setUseTTS(isChecked)
            val method = if (isChecked) "Text-to-Speech" else "Pre-recorded message"
            Toast.makeText(this, "Using $method", Toast.LENGTH_SHORT).show()
        }
        
        // Settings FAB
        fabSettings.setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $missingPermissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All permissions already granted")
            setupCallScreeningService()
        }
    }
    
    private fun setupCallScreeningService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    Log.d(TAG, "Requesting call screening role")
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    callScreeningLauncher.launch(intent)
                } else {
                    Log.d(TAG, "Call screening role already granted")
                }
            } else {
                Log.w(TAG, "Call screening role not available on this device")
                showCallScreeningNotAvailableDialog()
            }
        } else {
            // For older Android versions, check if we can use TelecomManager
            checkCallScreeningServiceStatus()
        }
    }
    
    private fun checkCallScreeningServiceStatus() {
        val telecomManager = getSystemService(TelecomManager::class.java)
        // For now, assume it's working if we have the required permissions
        Log.d(TAG, "Call screening service status checked")
    }
    
    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Hellena needs the following permissions to work properly:\n\n" +
                    deniedPermissions.joinToString("\n") { getPermissionDisplayName(it) } +
                    "\n\nPlease grant these permissions in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCallScreeningNotAvailableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Call Screening Not Available")
            .setMessage("Your device doesn't support automatic call screening. Hellena may not be able to automatically answer calls.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(messageId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this voice message?")
            .setPositiveButton("Delete") { _, _ ->
                if (storageHelper.deleteVoiceMessage(messageId)) {
                    refreshVoiceMessages()
                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSettingsDialog() {
        val currentMessage = storageHelper.getCustomMessage()
        val editText = EditText(this).apply {
            setText(currentMessage)
            hint = "Enter your custom message"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Custom Message")
            .setMessage("Customize the message that will be played to callers:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newMessage = editText.text.toString().trim()
                if (newMessage.isNotEmpty()) {
                    storageHelper.setCustomMessage(newMessage)
                    Toast.makeText(this, "Message updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_PHONE_STATE -> "• Phone State (to detect calls)"
            Manifest.permission.ANSWER_PHONE_CALLS -> "• Answer Calls (to auto-answer)"
            Manifest.permission.RECORD_AUDIO -> "• Record Audio (to capture messages)"
            Manifest.permission.FOREGROUND_SERVICE -> "• Foreground Service (to run in background)"
            else -> "• $permission"
        }
    }
    
    private fun refreshVoiceMessages() {
        val messages = storageHelper.getVoiceMessages()
        voiceMessageAdapter.updateMessages(messages)
        
        // Show/hide empty state
        if (messages.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            emptyStateText.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            emptyStateText.visibility = android.view.View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        refreshVoiceMessages()
        
        // Update switch states in case they were changed elsewhere
        availabilitySwitch.isChecked = !storageHelper.isUserAvailable()
        ttsSwitch.isChecked = storageHelper.shouldUseTTS()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceMessageAdapter.release()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshVoiceMessages()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Hellena")
            .setMessage("Hellena is your personal call assistant that answers calls when you're unavailable and records voice messages.\n\nVersion 1.0")
            .setPositiveButton("OK", null)
            .show()
    }
}