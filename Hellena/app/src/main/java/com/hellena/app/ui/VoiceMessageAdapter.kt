package com.hellena.app.ui

import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hellena.app.R
import com.hellena.app.model.VoiceMessage
import java.io.File

/**
 * Adapter for displaying voice messages in RecyclerView
 */
class VoiceMessageAdapter(
    private var messages: List<VoiceMessage>,
    private val onMessagePlayed: (String) -> Unit,
    private val onMessageDelete: (String) -> Unit
) : RecyclerView.Adapter<VoiceMessageAdapter.VoiceMessageViewHolder>() {
    
    companion object {
        private const val TAG = "VoiceMessageAdapter"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingId: String? = null
    
    class VoiceMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val callerName: TextView = itemView.findViewById(R.id.tv_caller_name)
        val timestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val duration: TextView = itemView.findViewById(R.id.tv_duration)
        val playButton: ImageButton = itemView.findViewById(R.id.btn_play)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)
        val unreadIndicator: View = itemView.findViewById(R.id.view_unread_indicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_message, parent, false)
        return VoiceMessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VoiceMessageViewHolder, position: Int) {
        val message = messages[position]
        
        holder.callerName.text = message.getDisplayName()
        holder.timestamp.text = message.getFormattedDate()
        holder.duration.text = message.getFormattedDuration()
        
        // Show/hide unread indicator
        holder.unreadIndicator.visibility = if (message.isPlayed) View.GONE else View.VISIBLE
        
        // Update play button state
        val isCurrentlyPlaying = currentPlayingId == message.id && mediaPlayer?.isPlaying == true
        holder.playButton.setImageResource(
            if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        
        // Play button click listener
        holder.playButton.setOnClickListener {
            if (isCurrentlyPlaying) {
                stopPlayback()
            } else {
                playMessage(message)
            }
        }
        
        // Delete button click listener
        holder.deleteButton.setOnClickListener {
            onMessageDelete(message.id)
        }
        
        // Mark as read when item is clicked
        holder.itemView.setOnClickListener {
            if (!message.isPlayed) {
                onMessagePlayed(message.id)
            }
        }
    }
    
    override fun getItemCount(): Int = messages.size
    
    private fun playMessage(message: VoiceMessage) {
        try {
            // Stop any current playback
            stopPlayback()
            
            val audioFile = File(message.audioFilePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${message.audioFilePath}")
                return
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(message.audioFilePath)
                prepareAsync()
                
                setOnPreparedListener { mp ->
                    mp.start()
                    currentPlayingId = message.id
                    notifyDataSetChanged() // Update UI to show pause button
                    
                    // Mark message as played
                    if (!message.isPlayed) {
                        onMessagePlayed(message.id)
                    }
                }
                
                setOnCompletionListener {
                    stopPlayback()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopPlayback()
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing message", e)
            stopPlayback()
        }
    }
    
    private fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            currentPlayingId = null
            notifyDataSetChanged() // Update UI to show play buttons
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }
    
    fun updateMessages(newMessages: List<VoiceMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
    
    fun release() {
        stopPlayback()
    }
}