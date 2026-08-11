# eFormX Android Application

An ultra-fast, professional, hardware-accelerated Android WebView application for **eFormX Digital Services**. Built with native Java, modern AndroidX components, custom deep linking, Firebase Cloud Messaging (FCM) integration, hardware-accelerated rendering, offline handling, native JavaScript bridges, and smart background audio execution.

---

## 🌟 Key Features & How They Work

### 🚀 Performance & Rendering Engine
- **GPU Hardware Layer Acceleration:** Uses `View.LAYER_TYPE_HARDWARE` on WebView for 60fps smooth scrolling and instant page rendering.
- **Smart Memory Caching:** `WebSettings.LOAD_DEFAULT` optimized with DOM Storage, Web Database, and Cookie Persistence for high-speed dynamic loading.
- **HTML5 Geolocation Support:** Hardware-accelerated geolocation enabled with custom `WebChromeClient` callback (`onGeolocationPermissionsShowPrompt`) for seamless `navigator.geolocation.getCurrentPosition(...)` calls.

---

### 📱 Android JavaScript Interface (`AndroidBridge`)
Exposes `Android` object to WebView JavaScript allowing web pages to retrieve GPS coordinates, device info, device ID, app version, package name, network status, trigger Text-To-Speech output, and request location permissions:

| Method | Parameters | Return Type | Description | JavaScript Usage Example |
| :--- | :--- | :--- | :--- | :--- |
| **`Android.getLocation()`** | None | `String` (JSON) | Returns GPS Latitude, Longitude, Accuracy, Altitude, Speed, Time JSON | `let loc = JSON.parse(Android.getLocation());` |
| **`Android.getDeviceInfo()`** | None | `String` (JSON) | Returns JSON string with hardware specs (Android ID, Manufacturer, Brand, Model, Device, Product, OS Version, SDK Level, Language, Country, TimeZone, Screen Dimensions, App Version, Package Name) | `let info = JSON.parse(Android.getDeviceInfo());` |
| **`Android.getDeviceId()`** | None | `String` | Returns unique Android ID string (`Settings.Secure.ANDROID_ID`) | `let id = Android.getDeviceId();` |
| **`Android.getAppVersion()`** | None | `String` | Returns App Version Name (e.g. `"1.0"`) | `let ver = Android.getAppVersion();` |
| **`Android.getPackageName()`** | None | `String` | Returns Package Identifier (`"eformx.app"`) | `let pkg = Android.getPackageName();` |
| **`Android.isNetworkAvailable()`** | None | `boolean` | Returns active internet connection state (`true`/`false`) | `let online = Android.isNetworkAvailable();` |
| **`Android.speak(text)`** | `text` (String) | `void` | Speaks text using native Android Text-to-Speech engine | `Android.speak('Hello from eFormX');` |
| **`Android.openLocationPermission()`** | None | `void` | Prompts system location permission dialog (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) | `Android.openLocationPermission();` |

---

### 💻 Frontend JavaScript Integration Guide

Web developers can integrate with the eFormX Android App using the following JavaScript snippets:

#### 1. Fetching GPS Latitude & Longitude Coordinates
```javascript
if (window.Android && window.Android.getLocation) {
    let locationResult = JSON.parse(window.Android.getLocation());
    if (!locationResult.error) {
        console.log("Latitude:", locationResult.latitude);
        console.log("Longitude:", locationResult.longitude);
        console.log("Accuracy:", locationResult.accuracy + " meters");
    } else {
        console.warn("Location error:", locationResult.message);
        window.Android.openLocationPermission();
    }
}
```

#### 2. Fetching Complete Device Specifications
```javascript
if (window.Android && window.Android.getDeviceInfo) {
    let deviceInfo = JSON.parse(window.Android.getDeviceInfo());
    console.log("Device Info:", deviceInfo);
}
```

#### 3. Standard HTML5 Geolocation Fallback
```javascript
if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
        function (pos) {
            console.log("HTML5 Lat:", pos.coords.latitude, "Lng:", pos.coords.longitude);
        },
        function (err) {
            console.error("HTML5 Error:", err.message);
        }
    );
}
```

---

### 🔔 Push Notifications & FCM Engine (`MyFirebaseMessagingService`)
- **FCM Data-Only High Priority Delivery:** Background & foreground push notification engine supporting custom titles, messages, big picture images, and custom target URLs.
- **Continuous Speech Announcements:** Text-to-Speech (TTS) engine (`speakOutText`) continuously loops speech announcements during call notifications.
- **OPPO / ColorOS Background Execution:** Wakes up CPU and Screen from deep sleep using `PowerManager.WakeLock` (`FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP`) for guaranteed background delivery when the app is closed or killed.
- **Branded Notification Cards:** Displays the official eFormX App Logo (`ic_launcher`) clearly on notification cards.

---

### 🔇 3-Way Smart Notification Audio Stop Engine
The application provides three immediate methods to stop background audio and repeating TTS speech:
1. **Swipe-to-Dismiss Stop (`NotificationDismissReceiver`):** Swiping away or removing the notification card immediately triggers `setDeleteIntent` to stop speech and cancel active notifications (`stopAllMediaAndTTS()`).
2. **App Open Auto-Stop:** Opening the eFormX application (via notification tap, home icon, or deep link) instantly clears background speech and audio playback in `MainActivity` lifecycle methods (`onCreate`, `onStart`, `onResume`, `onNewIntent`).
3. **Hardware Volume Button Mute (`VolumeButtonReceiver` & `onKeyDown`):** Pressing hardware Volume Down, Volume Up, or Mute buttons anywhere on the phone triggers `android.media.VOLUME_CHANGED_ACTION` to instantly silence background speech and notification audio.

---

### 📡 Master FCM Notification API Reference (`POST /api/send-notification`)

Use a single unified API endpoint (`http://localhost:3000/api/send-notification`) to dispatch notifications. Customize payload behavior by adding or changing key parameters in the Master JSON Schema.

#### 1️⃣ Master Unified JSON Schema

```json
{
  "token": "OPTIONAL_SPECIFIC_USER_FCM_TOKEN",
  "topic": "all",
  "title": "Notification Title",
  "message": "Message text description",
  "speak_text": "Text to speak out loud continuously",
  "sound_type": "ringtone",
  "image_url": "https://apply.eformx.com/banner.jpg",
  "target_url": "https://apply.eformx.com/status.php?id=123",
  "open_type": "app_webview",
  "audio_url": "https://apply.eformx.com/chime.mp3"
}
```

```bash
curl -X POST http://localhost:3000/api/send-notification \
  -H "Content-Type: application/json" \
  -d '{
    "title": "📞 eFormX Call Alert",
    "message": "Namaste, EFORMX call notification.",
    "speak_text": "Namaste, EFORMX call notification.",
    "sound_type": "ringtone"
  }'
```

---

#### 2️⃣ JSON Field Parameter Effect Table

| JSON Key / Parameter | Type | Default Value | Value Options / Example | Effect & App Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **`token`** | `String` | *(Empty)* | `"eX8kL1mN...xyz"` | **Single User Target:** When provided, notification is sent ONLY to this 1 specific user device. |
| **`topic`** | `String` | `"all"` | `"all"` | **Mass Broadcast:** When `token` is omitted, broadcasts notification to ALL 500+ registered users. |
| **`title`** | `String` | `"eFormX Notification"` | `"📞 Incoming Call Request"` | Sets the bold header title displayed on the Android notification card. |
| **`message`** (or `body`) | `String` | `"You have a new update."` | `"Namaste, Admin is calling..."` | Sets the description text body on the notification card. |
| **`speak_text`** (or `tts_text`)| `String` | *(Message Body)* | `"Namaste Ramesh, Admin call kar rahe hain."` | **Continuous Speech Loop:** Triggers native Text-to-Speech to continuously speak this text until swiped, opened, or volume muted! |
| **`sound_type`** (or `sound`)| `String` | `"notification"` | `"ringtone"` / `"call"` / `"notification"` / `"silent"` | Sets notification channel behavior. `"ringtone"` or `"call"` enables High-Priority Call Mode. |
| **`image_url`** (or `imageUrl`)| `String` | *(Empty)* | `"https://.../banner.jpg"` | **Banner Image:** Downloads and renders a full expandable Big Picture banner image on the notification card. |
| **`target_url`** (or `url`) | `String` | `"https://eformx.com"` | `"https://apply.eformx.com/form123"` | **Target Webpage:** Tapping the notification card opens this specific webpage link inside the app. |
| **`open_type`** | `String` | `"app_webview"` | `"app_webview"` / `"external_browser"` | `"app_webview"` opens URL inside app; `"external_browser"` opens URL in Chrome Custom Tabs. |
| **`audio_url`** (or `audio`) | `String` | *(Empty)* | `"https://.../audio.mp3"` | **Remote MP3 Audio:** Streams and plays a custom online MP3 audio sound when notification arrives. |

---

### 🎨 Modern UI & UX
- **Onboarding Carousel (`SplashActivity`):** Interactive onboarding experience with auto-advancing slides, modern indicator dots, and sleek action buttons.
- **Minimal Branded Loading Screen:** Dynamic full-page loading overlay matching custom branding with real-time status titles.
- **Pixel-Perfect Offline Error Screen:** Custom Canvas-drawn Wi-Fi slash icon badge, "Try Again" reload action, "Check Connection" system settings shortcut, and "Contact Support" WhatsApp integration.

### 🛡️ User Protection & Data Safety
- **Form Data Protection:** Auto-reload is disabled on network reconnect to prevent loss of user-entered HTML form data, input fields, and text entries.
- **App Exit Confirmation Alert:** Displays an interactive Exit Confirmation Dialog ("Exit App?") on the last back press to prevent accidental app closure.

---

## 🔑 Permissions Reference

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required for loading web pages and connecting to online services. |
| `android.permission.ACCESS_NETWORK_STATE` | Monitors device connection status for offline handling and connectivity updates. |
| `android.permission.POST_NOTIFICATIONS` | Allows posting push notifications on Android 13+ (API level 33+). |
| `android.permission.WAKE_LOCK` | Keeps CPU awake when handling high-priority background notifications and call alerts. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ensures timely delivery of real-time push notifications without battery throttling. |
| `android.permission.ACCESS_FINE_LOCATION` | Allows WebViews and native features to access precise device GPS location. |
| `android.permission.ACCESS_COARSE_LOCATION` | Allows access to approximate network-based device location. |

---

## 🛠️ Architecture & Tech Stack

- **Language:** Java 17 / Kotlin
- **Build System:** Gradle 8.13 with AGP 8.9.0
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **Primary Package:** `eformx.app`
- **Push Notification Service:** Firebase Cloud Messaging (FCM)

---

## 📦 Project Dependencies

### 🖥️ Backend / Notification Server Dependencies (Node.js)

```json
{
  "dependencies": {
    "cors": "^2.8.6",
    "express": "^5.2.1",
    "firebase-admin": "^14.2.0"
  }
}
```

| Package Name | Version | Purpose & Description |
| :--- | :--- | :--- |
| **`cors`** | `^2.8.6` | Enables Cross-Origin Resource Sharing (CORS) for API requests from web applications. |
| **`express`** | `^5.2.1` | Web application framework for Node.js powering FCM notification dispatch endpoints. |
| **`firebase-admin`** | `^14.2.0` | Firebase Admin SDK to send high-priority FCM push notifications, topics, and data payloads to Android devices. |

### 📱 Android Application Dependencies (Gradle)

| Dependency | Purpose |
| :--- | :--- |
| **`com.google.firebase:firebase-messaging`** | Firebase Cloud Messaging (FCM) push notification engine. |
| **`androidx.browser:browser:1.8.0`** | Chrome Custom Tabs integration for external link navigation. |
| **`androidx.core:core-ktx`** | Core Kotlin extensions for Android development. |
| **`androidx.activity:activity-compose`** | Activity integration for Jetpack Compose. |
| **`androidx.compose.material3:material3`** | Material Design 3 components. |
| **`androidx.lifecycle:lifecycle-runtime-ktx`** | Lifecycle-aware coroutine support. |

---

## 📱 Core Project Structure

```
app/src/main/
├── java/eformx/app/
│   ├── MainActivity.java                 # Main Web View, Network Callback, Offline Screen, Exit Dialog & Volume Key Handler
│   ├── SplashActivity.java               # Branded Onboarding Carousel & Splash Screen
│   ├── AndroidBridge.java                # Standalone JavaScript Interface Bridge (Location, Device Info, Device ID, App Version, Package Name, TTS)
│   ├── MyFirebaseMessagingService.java   # FCM Push Service, WakeLock, Looping TTS Speech & Audio Cleanup
│   ├── NotificationDismissReceiver.java  # BroadcastReceiver handling notification swipe-to-dismiss audio/TTS cleanup
│   └── VolumeButtonReceiver.java         # BroadcastReceiver muting notification speech/audio on hardware volume key press
├── res/
│   ├── drawable/                         # Custom shapes, gradients, and icons
│   ├── layout/                           # XML layouts
│   └── values/                           # Colors, strings, themes
└── AndroidManifest.xml                   # Intent filters, permissions, activities, services & receivers
```

---

## 📦 Building the Project

To compile and assemble the debug APK:

```bash
# On Windows PowerShell / Command Prompt
.\gradlew assembleDebug

# On Linux / macOS
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚙️ Configuration & Default Launch URL

The default web portal URL is loaded in `MainActivity.java`:
- **Default URL:** `https://apply.eformx.com`
