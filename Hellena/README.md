# Hellena - Personal Call Assistant

Hellena is an intelligent Android assistant that automatically answers calls when you're unavailable and records voice messages from callers.

## Features

### Core Functionality
- **Automatic Call Detection**: Detects incoming phone calls using `CallScreeningService`
- **Auto-Answer**: Automatically answers calls when you're marked as "unavailable"
- **Greeting Messages**: Plays customizable TTS or pre-recorded messages to callers
- **Voice Recording**: Records caller's voice messages up to 30 seconds
- **Local Storage**: Saves audio files locally with timestamp and caller ID
- **Inbox UI**: Simple interface to view, play, and manage voice messages

### Key Components
- **CallScreeningService**: Detects and screens incoming calls
- **CallHandlingService**: Foreground service that handles call answering and recording
- **TTSManager**: Manages Text-to-Speech functionality
- **RecorderManager**: Handles audio recording with MediaRecorder
- **StorageHelper**: Manages local storage of messages and app settings
- **MainActivity**: Main UI with availability toggle and message inbox

## Requirements

- **Android 10 (API 29) or higher**
- **Permissions**:
  - `READ_PHONE_STATE` - To detect incoming calls
  - `ANSWER_PHONE_CALLS` - To automatically answer calls
  - `RECORD_AUDIO` - To record voice messages
  - `FOREGROUND_SERVICE` - To run in background
  - `MODIFY_AUDIO_SETTINGS` - To manage audio during calls

## Installation

1. Clone or download the project
2. Open in Android Studio
3. Build and install on your device
4. Grant all required permissions when prompted
5. Set Hellena as your default call screening app (Android 10+)

## Usage

### Initial Setup
1. Launch the app and grant all requested permissions
2. Set up call screening role (Android 10+)
3. Configure your availability status and message preferences

### Main Interface
- **Auto-Answer Switch**: Toggle to enable/disable automatic call answering
- **TTS Switch**: Choose between Text-to-Speech or pre-recorded messages
- **Voice Messages List**: View all recorded messages with play/delete options
- **Settings FAB**: Customize your greeting message

### How It Works
1. When a call comes in, `CallScreeningService` checks your availability status
2. If you're unavailable, `CallHandlingService` starts and answers the call
3. The service plays your greeting message using TTS or pre-recorded audio
4. After the message, it records the caller's response for up to 30 seconds
5. The recording is saved locally and appears in your inbox

## Technical Architecture

### Services
- **CallScreeningService**: Screens incoming calls and triggers call handling
- **CallHandlingService**: Foreground service that manages the entire call flow
- **BootReceiver**: Restarts services after device reboot

### Managers
- **TTSManager**: Handles Text-to-Speech initialization and playback
- **RecorderManager**: Manages MediaRecorder for voice message capture
- **StorageHelper**: Handles local data persistence using SharedPreferences and file system

### Data Models
- **VoiceMessage**: Data class representing recorded voice messages with metadata

## Privacy & Security

- **Local-Only**: All voice messages are stored locally on your device
- **No Cloud Integration**: No data is sent to external servers
- **Privacy Controls**: Voice messages are excluded from device backups
- **User Control**: Full control over message deletion and app settings

## Limitations

- **Android 10+ Required**: Call screening APIs are only available on Android 10+
- **Device Compatibility**: Some manufacturers may restrict call handling capabilities
- **Permission Dependent**: All features require appropriate system permissions
- **30-Second Limit**: Voice messages are limited to 30 seconds to manage storage

## Known Issues

- Some devices may not support automatic call answering due to manufacturer restrictions
- TTS quality depends on device's text-to-speech engine
- Background restrictions may affect service reliability on some devices

## Development Notes

### Key Implementation Details
- Uses `CallScreeningService` for Android 10+ compatibility
- Implements foreground service with proper notification management
- Handles audio focus and call state management
- Includes proper lifecycle management for background operations
- Implements Material Design 3 UI components

### Testing
- Test on physical devices (call functionality doesn't work in emulators)
- Verify permissions are granted correctly
- Test with different call scenarios (known/unknown numbers)
- Validate audio recording quality and playback

## Future Enhancements

### Phase 2 Features (Not Included)
- **Voice Transcription**: Convert voice messages to text using Vosk or Whisper
- **Cloud Integration**: Optional Firebase integration for backup and sync
- **Smart Replies**: AI-generated response suggestions
- **Advanced Filtering**: Custom rules for different callers
- **Multiple Languages**: Multi-language TTS support

## License

This project is for educational and personal use. Please respect privacy laws and regulations in your jurisdiction when using call recording features.

## Support

For issues or questions:
1. Check device compatibility and permissions
2. Verify call screening role is properly set
3. Review logs for debugging information
4. Test with a known working phone number

---

**Note**: This app is designed for personal use and may not be suitable for distribution on Google Play Store due to call handling restrictions. Consider sideloading or enterprise distribution channels.