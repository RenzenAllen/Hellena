# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Hellena specific classes
-keep class com.hellena.app.model.** { *; }
-keep class com.hellena.app.service.** { *; }
-keep class com.hellena.app.manager.** { *; }
-keep class com.hellena.app.storage.** { *; }

# Keep classes that are used by Android framework
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.telecom.CallScreeningService

# Keep MediaRecorder and MediaPlayer related classes
-keep class android.media.** { *; }
-keep class android.speech.tts.** { *; }
-keep class android.telecom.** { *; }

# Keep JSON related classes for data storage
-keepclassmembers class * {
    @org.json.** <fields>;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}