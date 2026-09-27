# ===================================================================================
# eFormX Military-Grade Obfuscation & Anti-Reverse-Engineering Rules (R8 Full Mode)
# ===================================================================================

# -----------------------------------------------------------------------------
# 1. AGGRESSIVE OBFUSCATION, INLINING & CLASS REPACKAGING
# -----------------------------------------------------------------------------
# Repackage all internal app classes into a single flat obfuscated namespace ('o')
# This completely destroys package structure visibility (services, api, utils etc.)
-repackageclasses 'eformx.app.o'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

# -----------------------------------------------------------------------------
# 2. STRIP DEBUGGING METADATA, SOURCE CODE ATTRIBUTES & LINE NUMBERS
# -----------------------------------------------------------------------------
# Strips original java file names, line numbers, local variables and debug tables.
# Decompilers (JADX, CFR, Ghidra) cannot recover original line numbers or filenames.
-renamesourcefileattribute ""
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------------
# 3. STRIP ALL LOGGING & DEBUG OUTPUT STATEMENTS FROM BYTECODE
# -----------------------------------------------------------------------------
# Completely eliminates all Log calls and their string parameters in release builds.
# Attackers cannot read debug statements, token logs, or API URLs from logs.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# -----------------------------------------------------------------------------
# 4. ESSENTIAL MANIFEST ENTRY POINTS (CLASSES ONLY - INTERNALS ARE OBFUSCATED)
# -----------------------------------------------------------------------------
# Keep only the class names required by AndroidManifest.xml so Android OS can launch them.
# All internal methods, private fields, and variables remain 100% obfuscated.
-keep public class eformx.app.SplashActivity extends android.app.Activity
-keep public class eformx.app.MainActivity extends android.app.Activity
-keep public class eformx.app.MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService
-keep public class eformx.app.NotificationDismissReceiver extends android.content.BroadcastReceiver
-keep public class eformx.app.VolumeButtonReceiver extends android.content.BroadcastReceiver

# -----------------------------------------------------------------------------
# 5. WEBVIEW JAVASCRIPT INTERFACE PROTECTION
# -----------------------------------------------------------------------------
# Only keep the specific methods annotated with @JavascriptInterface required for WebView bridge.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class eformx.app.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class eformx.app.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# -----------------------------------------------------------------------------
# 6. SECURE STRING VAULT OBFUSCATION
# -----------------------------------------------------------------------------
# Allow SecureConfig methods to be aggressively inlined and renamed
-keepclassmembers class eformx.app.SecureConfig {
    public static java.lang.String get*(...);
}

# -----------------------------------------------------------------------------
# 7. FIREBASE & GOOGLE PLAY SERVICES
# -----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# -----------------------------------------------------------------------------
# 8. ANDROIDX & SUPPORT LIBRARIES
# -----------------------------------------------------------------------------
-keep class androidx.** { *; }
-dontwarn androidx.**

# -----------------------------------------------------------------------------
# 9. ANDROID R RESOURCE CLASSES
# -----------------------------------------------------------------------------
-dontwarn **.R$*
-dontwarn **.R
-keepclassmembers class **.R$* {
    public static <fields>;
}
