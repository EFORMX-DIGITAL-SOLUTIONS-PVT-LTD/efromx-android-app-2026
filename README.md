# eFormX Android Application

An ultra-fast, professional, hardware-accelerated Android WebView application for **eFormX Digital Services**. Built with native Java, modern AndroidX components, custom deep linking, Firebase Cloud Messaging (FCM) integration, offline handling, and smooth onboarding workflows.

---

## 🌟 Key Features

### 🚀 Performance & Rendering
- **GPU Hardware Layer Acceleration:** Uses `View.LAYER_TYPE_HARDWARE` on WebView for 60fps smooth scrolling and instant page rendering.
- **Smart Memory Caching:** `WebSettings.LOAD_DEFAULT` optimized with DOM Storage, Web Database, and Cookie Persistence for high-speed dynamic loading.

### 🔔 Push Notifications & Messaging (Firebase Cloud Messaging)
- **FCM Service (`MyFirebaseMessagingService`):** Background & foreground push notification engine supporting custom titles, messages, big picture images, and custom action links.
- **Rich Media & Deep Link Handling:** Automatic image downloading for notifications and click-through navigation directly into target WebViews or external schemes.
- **Text-to-Speech (TTS) & Custom Alerts:** Optional audio playback and Text-to-Speech announcements for critical incoming alerts.
- **Notification Channel:** Custom notification channel (`eformx_notification_channel`) with high priority, custom sound, and vibration support.

### 🎨 Modern UI & UX
- **Onboarding Carousel (`SplashActivity`):** Interactive onboarding experience with auto-advancing slides, modern indicator dots, and sleek action buttons.
- **Minimal Branded Loading Screen:** Dynamic full-page loading overlay matching custom branding with real-time status titles ("Loading Application Form...", "Loading Dashboard...", "Opening Login Portal..."). Locked on screen until 100% web page load completion.
- **Pixel-Perfect Offline Error Screen:** Custom Canvas-drawn Wi-Fi slash icon badge, "Try Again" reload action, "Check Connection" system settings shortcut, and "Contact Support" WhatsApp integration.

### 🛡️ User Protection & Data Safety
- **Form Data Protection:** Auto-reload is disabled on network reconnect to prevent loss of user-entered HTML form data, input fields, and text entries.
- **App Exit Confirmation Alert:** Displays an interactive Exit Confirmation Dialog ("Exit App?") on the last back press to prevent accidental app closure.
- **Android JavaScript Interface Bridge (`AndroidBridge`):** Exposes `Android` object to WebView JavaScript allowing web pages to retrieve device info, device ID, trigger Text-To-Speech output, and request location permissions.

---

## 📱 Android JavaScript Interface (`AndroidBridge`)

The application exposes the `AndroidBridge` JavaScript interface object named `Android` inside WebView:

| Method | Parameters | Return Type | Description | JavaScript Usage Example |
| :--- | :--- | :--- | :--- | :--- |
| **`Android.speak(text)`** | `text` (String) | `void` | Speaks the provided text using native Android Text-to-Speech engine | `Android.speak('Hello from eFormX');` |
| **`Android.openLocationPermission()`** | None | `void` | Prompts system location permission dialog (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) | `Android.openLocationPermission();` |
| **`Android.getDeviceInfo()`** | None | `String` (JSON) | Returns JSON string with hardware specs (Android ID, Manufacturer, Model, OS, Screen Size, Timezone, etc.) | `let info = JSON.parse(Android.getDeviceInfo());` |
| **`Android.getDeviceId()`** | None | `String` | Returns unique Android ID string (`Settings.Secure.ANDROID_ID`) | `let id = Android.getDeviceId();` |

---

## 🔗 Deep Links, Query Parameters & Callback Reference

The application features advanced URL parsing and intent handling to intercept special schemes and parameters sent from web portals:

| Query Parameter / Scheme | Practical Usage Example | Behavior & Description |
| :--- | :--- | :--- |
| **`browser=external`** | `https://apply.eformx.com/portal.php?browser=external` | Intercepted in `isExternalBrowserRequested()`. Opens the requested URL outside the WebView using Chrome Custom Tabs or the device's default web browser. *(Supports legacy typo `browser=extrunal`)* |
| **`cache=ID`** | `https://eformx.com?cache=1` | Smart Cache-ID Interceptor. Intercepted in `applySmartCacheStrategy()`. Compares incoming `cache` ID parameter against saved disk ID. If matching, forces `WebSettings.LOAD_CACHE_ONLY` for 100% network-independent instant 0ms local disk cache loading. |
| **`share_link=true`** | `https://apply.eformx.com/form.php?share_link=true` | Intercepted URL parameter. Automatically triggers native Android Share Intent sheet allowing users to share the current URL across installed apps. |
| **`callback=app`** | `https://apply.eformx.com/success.php?callback=app` | App return callback query parameter. Signals completion of external actions and returns the user to the app's clean WebView state. *(Also handles `calback=app`)* |
| **`eformx://`** | `eformx://apply/form123` | Custom deep link scheme. Automatically parsed by `parseEformxUrl()` to construct the target HTTPS URL (`https://apply.eformx.com/apply/form123`) and loaded seamlessly inside the app. |
| **`eformx:/?callback=app`** | `eformx:/?callback=app` | Short callback URL interceptor handled in `isShortCallbackUrl()`. Returns a 200 OK empty HTML response to prevent error screens and returns user cleanly back to app. |
| **`wa.me` / `whatsapp://`** | `https://wa.me/919876543210?text=Hello` | Auto-converted by `convertToWhatsappScheme()` into native `whatsapp://send?phone=919876543210&text=Hello` schemes to launch WhatsApp directly. |
| **Non-HTTP Schemes** | `tel:+919876543210`<br>`mailto:support@eformx.com`<br>`sms:+919876543210` | Intercepted in `handleNonHttpScheme()`. Launches external system apps via Android Intents (e.g. Phone Dialer, Mail Client, SMS, Payment Apps like UPI). |

---

## 🔔 Push Notification Types & FCM Payload Specifications

The application supports multiple dynamic notification types, custom sound modes, rich media, and TTS handled by `MyFirebaseMessagingService`:

### 📢 Supported Notification Types

1. **Standard System Notification (`sound_type: "notification"` / default)**
   - Displays a high-priority system notification with default chime sound and vibration on channel `eformx_notification_channel_v3`.
2. **Ringtone / Call Alert Notification (`sound_type: "ringtone"` / `"call"`)**
   - Continuously loops system ringtone (`RingtoneManager.TYPE_RINGTONE`) with `FLAG_INSISTENT` on channel `eformx_call_channel_v3` for urgent incoming alerts or call events.
3. **Alarm Alert Notification (`sound_type: "alarm"`)**
   - Plays alarm sound (`RingtoneManager.TYPE_ALARM`) on channel `eformx_alarm_channel_v3`.
4. **Voice Speech / Text-To-Speech Notification (`speak_text: "..."` or `sound_type: "voice"` / `"silent"`)**
   - Mutes default sound on channel `eformx_silent_channel_v1` and speaks the alert text out loud in natural voice using Android's native Text-to-Speech (TTS) engine (supporting Hindi/English).
5. **Custom Remote MP3 Audio Alert (`audio_url: "https://..."`)**
   - Downloads and streams a custom remote MP3 file via `MediaPlayer` immediately upon notification arrival.
6. **Rich Media Image Notification (`imageUrl: "https://..."`)**
   - Asynchronously downloads dynamic image URL and renders an expanded `BigPictureStyle` banner notification.

---

### 📋 FCM Data Payload Schema & Parameters Reference

| Data Payload Key | Alternative Payload Keys | Purpose & Description | Practical Usage Example |
| :--- | :--- | :--- | :--- |
| **`title`** | - | Title text of the notification card | `"Form Approved"` |
| **`message`** | `body` | Main notification body text | `"Your eForm #8492 has been processed."` |
| **`imageUrl`** | `image`, `image_url` | Direct URL for BigPictureStyle expanded image | `"https://eformx.com/img/banner.jpg"` |
| **`target_url`** | `url`, `link` | Target Web URL or deep link to open on click | `"https://apply.eformx.com/status.php"` |
| **`open_type`** | - | Target window (`app_webview` or `external`) | `"app_webview"` |
| **`sound_type`** | `sound` | Alert tone mode (`notification`, `call`, `ringtone`, `alarm`, `silent`, `voice`) | `"call"` |
| **`speak_text`** | `tts_text`, `tts=true` | Reads out text via native Text-To-Speech (TTS) | `"Aapka application status update ho gaya hai."` |
| **`audio_url`** | `audio`, `mp3_url` | Direct URL to remote MP3 file played on receipt | `"https://eformx.com/audio/alert.mp3"` |

---

### 🎯 Notification Targeting Modes (Recipient Types)

| Targeting Mode | Payload `"to"` Value | Description & Use Case |
| :--- | :--- | :--- |
| **All App Users (Mass Broadcast)** | `"to": "/topics/all"` | Sends notification simultaneously to every installed eFormX app instance subscribed to global topic. |
| **Specific Topic Group** | `"to": "/topics/<topic_name>"` | Sends notification to a subset of users subscribed to a custom topic (e.g. `/topics/news`, `/topics/alerts`). |
| **Single Specific User Device** | `"to": "<FCM_DEVICE_TOKEN>"` | Targets a single specific user device using their unique FCM Registration Token. |

---

### 💻 Sample FCM Push Notification JSON Payloads

#### 1. Broadcast Notification Payload (Send to ALL Users)

```json
{
  "to": "/topics/all",
  "data": {
    "title": "Important Service Announcement",
    "message": "New eForm portals are now live! Tap to view details.",
    "imageUrl": "https://eformx.com/assets/broadcast_banner.png",
    "target_url": "https://apply.eformx.com/announcement.php",
    "open_type": "app_webview",
    "sound_type": "notification",
    "speak_text": "Important announcement update for all eFormX users."
  }
}
```

#### 2. Single User Notification Payload (Send to Specific Token)

```json
{
  "to": "fcm_device_registration_token_here",
  "data": {
    "title": "Application Status Update",
    "message": "Your eForm #1092 has been successfully verified.",
    "imageUrl": "https://eformx.com/assets/notification_banner.png",
    "target_url": "https://apply.eformx.com/dashboard.php?id=1092",
    "open_type": "app_webview",
    "sound_type": "call",
    "speak_text": "Aapka form verify ho gaya hai."
  }
}
```

---

### 🖥️ Firebase Admin Node.js / Express Server Integration Example

Complete backend server implementation using Node.js, Express, and `firebase-admin` to send push notifications, ringtone call alerts, and mass broadcasts:

```javascript
const express = require("express");
const { initializeApp, cert } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const cors = require("cors");
const serviceAccount = require("./serviceAccountKey.json");

const app = express();
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin
initializeApp({
  credential: cert(serviceAccount),
});

// Helper function to build High-Priority Data-Only FCM Message
function buildFcmMessage(reqBody) {
  const {
    token,
    topic,
    title,
    message,
    body,
    image_url,
    imageUrl,
    image,
    audio_url,
    audio,
    mp3_url,
    target_url,
    url,
    open_type,
    sound_type,
    sound,
    speak_text,
    tts_text
  } = reqBody;

  const finalTitle = title || "eFormX Notification";
  const finalBody = message || body || "You have a new update.";
  const finalImg = image_url || imageUrl || image || "";
  const finalAudioUrl = audio_url || audio || mp3_url || "";
  const finalTargetUrl = target_url || url || "https://eformx.com/app.php";
  const finalOpenType = open_type || "app_webview";
  const finalSoundType = sound_type || sound || "notification";
  const finalSpeakText = speak_text || tts_text || finalBody;

  // Use valid token if provided, else default to broadcast topic "all"
  const targetObj = (token && token !== "YOUR_FCM_TOKEN" && token.length > 20) 
    ? { token } 
    : { topic: topic || "all" };

  return {
    ...targetObj,
    android: {
      priority: "high",
      ttl: 0
    },
    data: {
      title: String(finalTitle),
      body: String(finalBody),
      message: String(finalBody),
      target_url: String(finalTargetUrl),
      open_type: String(finalOpenType),
      sound_type: String(finalSoundType),
      speak_text: String(finalSpeakText),
      audio_url: String(finalAudioUrl),
      image_url: String(finalImg),
      imageUrl: String(finalImg),
      timestamp: String(Date.now()),
    },
  };
}

// 1. General Notification API
app.post("/api/send-notification", async (req, res) => {
  try {
    const payload = buildFcmMessage(req.body);
    const responseId = await getMessaging().send(payload);
    console.log("✅ Notification sent successfully! ID:", responseId);
    return res.status(200).json({ success: true, messageId: responseId });
  } catch (error) {
    console.error("❌ Error sending notification:", error);
    return res.status(500).json({ success: false, error: error.message });
  }
});

// 2. Incoming Call Ringtone Notification API
app.post("/api/send-call", async (req, res) => {
  try {
    const body = {
      ...req.body,
      title: req.body.title || "📞 Incoming Call Request",
      message: req.body.message || req.body.body || "eFormX Admin is calling...",
      sound_type: "ringtone",
      speak_text: req.body.speak_text || "Incoming call request from admin"
    };
    const payload = buildFcmMessage(body);
    const responseId = await getMessaging().send(payload);
    console.log("✅ Call Notification sent successfully! ID:", responseId);
    return res.status(200).json({ success: true, messageId: responseId });
  } catch (error) {
    console.error("❌ Error sending call notification:", error);
    return res.status(500).json({ success: false, error: error.message });
  }
});

// 3. Broadcast Topic Notification API
app.post("/api/send-all", async (req, res) => {
  try {
    const body = {
      ...req.body,
      topic: req.body.topic || "all"
    };
    const payload = buildFcmMessage(body);
    const responseId = await getMessaging().send(payload);
    console.log("✅ Broadcast Notification sent successfully! ID:", responseId);
    return res.status(200).json({ success: true, messageId: responseId });
  } catch (error) {
    console.error("❌ Error sending broadcast notification:", error);
    return res.status(500).json({ success: false, error: error.message });
  }
});

app.listen(3000, () => console.log("🚀 Express server running on port 3000"));
```

---

## 🔑 Permissions Reference

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Required for loading web pages and connecting to online services. |
| `android.permission.ACCESS_NETWORK_STATE` | Monitors device connection status for offline handling and connectivity state updates. |
| `android.permission.POST_NOTIFICATIONS` | Allows posting push notifications on Android 13+ (API level 33+). |
| `android.permission.WAKE_LOCK` | Keeps the processor awake when handling high-priority background notification payloads. |
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
│   ├── MainActivity.java                 # Main Web View, Network Callback, Offline Screen & Exit Dialog
│   ├── SplashActivity.java               # Branded Onboarding Carousel & Splash Screen
│   ├── MyFirebaseMessagingService.java   # Firebase Cloud Messaging Service & Push Notifications
│   └── WebAppInterface.java              # Android Share Javascript Interface
├── res/
│   ├── drawable/                         # Custom shapes, gradients, and icons
│   ├── layout/                           # XML layouts
│   └── values/                           # Colors, strings, themes
└── AndroidManifest.xml                   # Deep links, permissions, & activity/service declarations
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

- **Default Launch URL:** `https://eformx.com/app.php`
- **Deep Link Intent Filter Hosts:** `apply.eformx.com`, `eformx.com`
- **Custom Deep Link Scheme:** `eformx://`
- **FCM Channel ID:** `eformx_notification_channel`

---

## 📄 License

Copyright © 2026 eFormX. All rights reserved.

