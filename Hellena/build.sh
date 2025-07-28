#!/bin/bash

# Hellena Build Script
echo "Building Hellena Android App..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Gradle wrapper not found. Creating gradlew..."
    gradle wrapper
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean and build the project
echo "Cleaning project..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
    
    # Check if APK exists
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        ls -la app/build/outputs/apk/debug/app-debug.apk
        echo ""
        echo "To install on device:"
        echo "adb install app/build/outputs/apk/debug/app-debug.apk"
    fi
else
    echo "❌ Build failed!"
    exit 1
fi