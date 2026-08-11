# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class eformx.app.AndroidBridge { *; }
-keep class eformx.app.MainActivity$WebAppInterface { *; }
-keep class eformx.app.MainActivity$* { *; }

# Keep Firebase and Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep standard AndroidX components
-keep class androidx.** { *; }
-dontwarn androidx.**
