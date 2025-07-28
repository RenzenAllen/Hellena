# Hellena Setup Guide

## Quick Start

1. **Open in Android Studio**
   - Import the project into Android Studio
   - Wait for Gradle sync to complete

2. **Build the Project**
   ```bash
   ./build.sh
   ```
   Or use Android Studio's build system

3. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Initial Configuration

### 1. Grant Permissions
When you first launch Hellena, you'll be prompted to grant several permissions:
- **Phone State**: To detect incoming calls
- **Answer Calls**: To automatically answer calls
- **Record Audio**: To capture voice messages
- **Foreground Service**: To run in background

### 2. Set Call Screening Role (Android 10+)
- Hellena will request to be set as your call screening app
- This is required for automatic call detection and answering
- Grant this permission in the system dialog

### 3. Configure Settings
- **Auto-Answer Switch**: Turn this ON to make Hellena answer calls when you're unavailable
- **TTS Switch**: Choose between Text-to-Speech or pre-recorded messages
- **Custom Message**: Tap the settings FAB to customize your greeting message

## Testing

### Safe Testing Method
1. Use a secondary phone or ask a friend to call you
2. Set yourself as "unavailable" in the app
3. Have them call your number
4. Hellena should answer and play your message
5. They can leave a voice message
6. Check your inbox for the recorded message

### Troubleshooting

**Call Not Being Answered:**
- Verify all permissions are granted
- Check if call screening role is properly set
- Some manufacturers (Samsung, Xiaomi) may have additional restrictions
- Try disabling battery optimization for Hellena

**No Audio Recording:**
- Ensure microphone permission is granted
- Check if other apps are using the microphone
- Verify audio settings in device preferences

**TTS Not Working:**
- Check if Text-to-Speech is enabled in device settings
- Try downloading additional TTS voices
- Test TTS in device accessibility settings

## Important Notes

- **Physical Device Required**: Call functionality doesn't work in emulators
- **Manufacturer Restrictions**: Some Android skins may limit call handling
- **Privacy**: All recordings are stored locally on your device
- **Storage**: Voice messages are limited to 30 seconds each

## File Structure

```
Hellena/
├── app/
│   ├── src/main/
│   │   ├── java/com/hellena/app/
│   │   │   ├── manager/          # TTSManager, RecorderManager
│   │   │   ├── model/            # VoiceMessage data class
│   │   │   ├── service/          # CallScreeningService, CallHandlingService
│   │   │   ├── storage/          # StorageHelper
│   │   │   └── ui/               # MainActivity, VoiceMessageAdapter
│   │   └── res/                  # Layouts, drawables, strings
│   └── build.gradle              # App dependencies
├── build.gradle                  # Project configuration
├── settings.gradle               # Project settings
└── README.md                     # Detailed documentation
```

## Development

### Key Classes
- **CallScreeningService**: Detects incoming calls
- **CallHandlingService**: Handles call answering and recording
- **TTSManager**: Text-to-speech functionality
- **RecorderManager**: Audio recording with MediaRecorder
- **StorageHelper**: Local data persistence
- **MainActivity**: Main UI and user interactions

### Architecture
- Uses modern Android architecture components
- Implements Material Design 3
- Local-first approach (no cloud dependencies)
- Proper lifecycle management for background services

## Next Steps

After basic setup:
1. Customize your greeting message
2. Test with different phone numbers
3. Manage your voice message inbox
4. Consider future enhancements (transcription, cloud backup)

For issues or questions, refer to the main README.md file.