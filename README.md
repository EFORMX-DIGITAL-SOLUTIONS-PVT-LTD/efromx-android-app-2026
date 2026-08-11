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
| **`Android.getFcmToken()`** | None | `String` | Returns device's Firebase Push Notification Registration Token string | `let token = Android.getFcmToken();` |
| **`Android.getLocation()`** | None | `String` (JSON) | Returns GPS Latitude, Longitude, Accuracy, Altitude, Speed, Time JSON | `let loc = JSON.parse(Android.getLocation());` |
| **`Android.getDeviceInfo()`** | None | `String` (JSON) | Returns JSON string with hardware specs (Android ID, Manufacturer, Brand, Model, Device, Product, OS Version, SDK Level, Language, Country, TimeZone, Screen Dimensions, App Version, Package Name, FCM Token) | `let info = JSON.parse(Android.getDeviceInfo());` |
| **`Android.getDeviceId()`** | None | `String` | Returns unique Android ID string (`Settings.Secure.ANDROID_ID`) | `let id = Android.getDeviceId();` |
| **`Android.getAppVersion()`** | None | `String` | Returns App Version Name (e.g. `"1.4"`) | `let ver = Android.getAppVersion();` |
| **`Android.getPackageName()`** | None | `String` | Returns Package Identifier (`"eformx.app"`) | `let pkg = Android.getPackageName();` |
| **`Android.getIpAddress()`** | None | `String` | Returns device local IPv4 address string (e.g. `"192.168.1.35"`) | `let ip = Android.getIpAddress();` |
| **`Android.getNetworkType()`** | None | `String` | Returns network type (`"WIFI"`, `"CELLULAR_MOBILE"`, `"OFFLINE"`) | `let netType = Android.getNetworkType();` |
| **`Android.getNetworkOperator()`** | None | `String` | Returns SIM carrier operator name (e.g. `"Jio"`, `"Airtel"`) | `let op = Android.getNetworkOperator();` |
| **`Android.isNetworkAvailable()`** | None | `boolean` | Returns active internet connection state (`true`/`false`) | `let online = Android.isNetworkAvailable();` |
| **`Android.speak(text)`** | `text` (String) | `void` | Speaks text using native Android Text-to-Speech engine | `Android.speak('Hello from eFormX');` |
| **`Android.hasLocationPermission()`** | None | `boolean` | Checks if Location permission (`ACCESS_FINE_LOCATION`) is granted | `let hasLoc = Android.hasLocationPermission();` |
| **`Android.hasNotificationPermission()`** | None | `boolean` | Checks if Notification permission (`POST_NOTIFICATIONS`) is granted | `let hasNotify = Android.hasNotificationPermission();` |
| **`Android.requestAllPermissions()`** | None | `void` | Triggers prompt for all missing permissions (Notifications, Location GPS) | `Android.requestAllPermissions();` |
| **`Android.exitApp()`** | None | `void` | Triggers the native eFormX Exit Confirmation Dialog directly | `Android.exitApp();` |
| **`Android.openLocationPermission()`** | None | `void` | Prompts system location permission dialog (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) | `Android.openLocationPermission();` |
| **`AndroidShare.share(title, text, url)`** | `title`, `text`, `url` (Strings) | `void` | Triggers native Android system Share Sheet intent to share links/text via WhatsApp, Email, Messages, etc. | `AndroidShare.share('eFormX', 'Check out eFormX', 'https://eformx.com');` |

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

#### 4. Native Web Share API (`navigator.share` / `injectSharePolyfill`)

The eFormX Android application automatically injects a JavaScript polyfill (`injectSharePolyfill`) into loaded web pages. This maps the standard W3C Web Share API (`navigator.share`) directly to the native Android System Share Intent sheet:

```javascript
// Standard W3C Web Share API (Automatically polyfilled inside eFormX App)
if (navigator.share) {
    navigator.share({
        title: 'eFormX Digital Portal',
        text: 'Check out eFormX services for online applications!',
        url: 'https://apply.eformx.com'
    }).then(function() {
        console.log('Shared successfully via native Android share sheet');
    }).catch(function(err) {
        console.error('Share failed:', err);
    });
} else if (window.AndroidShare) {
    // Direct JavaScript Interface fallback
    window.AndroidShare.share(
        'eFormX Digital Portal',
        'Check out eFormX services for online applications!',
        'https://apply.eformx.com'
    );
}
```

##### ⚙️ How `injectSharePolyfill` Works Under the Hood:

Inside `MainActivity.java`, the WebView automatically executes the polyfill injection on page load:

```java
private void injectSharePolyfill(WebView view) {
    String js = "if (window.AndroidShare) {" +
            "  navigator.share = function(data) {" +
            "    return new Promise(function(resolve, reject) {" +
            "      try {" +
            "        var title = (data && data.title) ? data.title : '';" +
            "        var text = (data && data.text) ? data.text : '';" +
            "        var url = (data && data.url) ? data.url : '';" +
            "        window.AndroidShare.share(title, text, url);" +
            "        resolve();" +
            "      } catch(e) { reject(e); }" +
            "    });" +
            "  };" +
            "}";
    view.evaluateJavascript(js, null);
}
```

---

### 🌐 Complete Web Portal Demo HTML Code (`index.html`)

Web developers can save this complete HTML5 file as `index.html` on their web server (`https://apply.eformx.com`) to test all native eFormX Android features, device identification, GPS coordinates, TTS, permissions, and deep links:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>eFormX Native Features Test Portal</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
        body { background: #0f172a; color: #f8fafc; padding: 16px; min-height: 100vh; }
        .header { text-align: center; padding: 20px 0; border-bottom: 1px solid #1e293b; margin-bottom: 20px; }
        .header h1 { color: #38bdf8; font-size: 24px; font-weight: 700; }
        .header p { color: #94a3b8; font-size: 14px; margin-top: 4px; }
        .grid-container { display: grid; grid-template-columns: 1fr; gap: 16px; max-width: 600px; margin: 0 auto; }
        .card { background: rgba(30, 41, 59, 0.7); border: 1px solid #334155; border-radius: 16px; padding: 16px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3); }
        .card h2 { font-size: 16px; color: #f1f5f9; margin-bottom: 12px; }
        .btn { width: 100%; padding: 12px 16px; border: none; border-radius: 12px; font-size: 14px; font-weight: 600; cursor: pointer; margin-bottom: 10px; }
        .btn-primary { background: linear-gradient(135deg, #0284c7, #2563eb); color: #ffffff; }
        .btn-success { background: linear-gradient(135deg, #059669, #10b981); color: #ffffff; }
        .btn-warning { background: linear-gradient(135deg, #d97706, #f59e0b); color: #ffffff; }
        .btn-purple { background: linear-gradient(135deg, #7c3aed, #9333ea); color: #ffffff; }
        .input-group { display: flex; gap: 8px; margin-bottom: 10px; }
        input[type="text"] { flex: 1; padding: 12px; border-radius: 12px; border: 1px solid #475569; background: #0f172a; color: #ffffff; font-size: 14px; }
        .output-box { background: #090d16; border: 1px solid #1e293b; border-radius: 12px; padding: 12px; font-family: monospace; font-size: 12px; color: #38bdf8; word-break: break-all; max-height: 150px; overflow-y: auto; margin-top: 10px; }
        .badge-green { background: #065f46; color: #34d399; padding: 2px 6px; border-radius: 4px; }
        .badge-red { background: #991b1b; color: #fca5a5; padding: 2px 6px; border-radius: 4px; }
        .link-item { color: #38bdf8; text-decoration: none; font-size: 14px; display: block; margin-bottom: 8px; word-break: break-all; }
    </style>
</head>
<body>

    <div class="header">
        <h1>eFormX Native Bridge Tester</h1>
        <p>Comprehensive Android Feature Verification</p>
    </div>

    <div class="grid-container">

        <!-- 1. Device Info & Identification -->
        <div class="card">
            <h2>📱 Device Information & Tokens</h2>
            <button class="btn btn-primary" onclick="testGetDeviceId()">Get Device ID</button>
            <button class="btn btn-primary" onclick="testGetFcmToken()">Get FCM Push Token</button>
            <button class="btn btn-primary" onclick="testGetAppVersion()">Get App Version</button>
            <button class="btn btn-primary" onclick="testGetPackageName()">Get Package Name</button>
            <button class="btn btn-primary" onclick="testIsNetworkAvailable()">Check Network Status</button>
            <button class="btn btn-warning" onclick="testGetDeviceInfo()">Get Complete Device Specs JSON</button>
            <div id="deviceOutput" class="output-box">Click any button above to see result...</div>
        </div>

        <!-- 2. GPS Location & Coordinates -->
        <div class="card">
            <h2>📍 GPS Location & Coordinates</h2>
            <button class="btn btn-success" onclick="testGetLocation()">Get GPS Coordinates (Native Bridge)</button>
            <button class="btn btn-success" onclick="testHtml5Geolocation()">Get Location (HTML5 Geolocation)</button>
            <button class="btn btn-primary" onclick="testCheckLocationPermission()">Check Location Permission</button>
            <button class="btn btn-warning" onclick="testOpenLocationPermission()">Request Location Permission Dialog</button>
            <div id="locationOutput" class="output-box">Location result will appear here...</div>
        </div>

        <!-- 3. Text to Speech (TTS) -->
        <div class="card">
            <h2>🔊 Text-to-Speech (Voice Output)</h2>
            <div class="input-group">
                <input type="text" id="ttsInput" value="Namaste, eFormX Android App me aapka swagat hai.">
                <button class="btn btn-purple" style="width: auto;" onclick="testSpeak()">Speak</button>
            </div>
        </div>

        <!-- 4. Permission Status & Prompt -->
        <div class="card">
            <h2>🛡️ Permissions Management</h2>
            <button class="btn btn-primary" onclick="testCheckPermissions()">Check Permission Statuses</button>
            <button class="btn btn-warning" onclick="testRequestAllPermissions()">Request All Pending Permissions</button>
            <div id="permissionOutput" class="output-box">Permission statuses will appear here...</div>
        </div>

        <!-- 5. Deep Links & External Navigation -->
        <div class="card">
            <h2>🔗 Deep Links & External Browsing</h2>
            <a href="https://apply.eformx.com/portal.php?browser=external" class="link-item">🌐 Open in Chrome External Browser (?browser=external)</a>
            <a href="eformx://apply/test_deep_link" class="link-item">🚀 Test Custom Deep Link (eformx://apply/test_deep_link)</a>
            <a href="https://wa.me/919876543210?text=Namaste%20eFormX" class="link-item">💬 Open WhatsApp Native Chat</a>
        </div>

        <!-- 6. Web Share API (Android Native System Share) -->
        <div class="card">
            <h2>📤 Native Web Share API</h2>
            <button class="btn btn-primary" onclick="testWebShare()">Share via Android Native Sheet</button>
            <div id="shareOutput" class="output-box">Share status will appear here...</div>
        </div>

    </div>

    <script>
        function isBridgeAvailable() { return typeof window.Android !== 'undefined'; }

        function testGetDeviceId() {
            let out = document.getElementById('deviceOutput');
            out.innerHTML = isBridgeAvailable() && window.Android.getDeviceId ? "<b>Device ID:</b> " + window.Android.getDeviceId() : "Open inside eFormX App";
        }

        function testGetFcmToken() {
            let out = document.getElementById('deviceOutput');
            out.innerHTML = isBridgeAvailable() && window.Android.getFcmToken ? "<b>FCM Token:</b><br>" + window.Android.getFcmToken() : "Open inside eFormX App";
        }

        function testGetAppVersion() {
            let out = document.getElementById('deviceOutput');
            out.innerHTML = isBridgeAvailable() && window.Android.getAppVersion ? "<b>App Version:</b> " + window.Android.getAppVersion() : "Web Browser";
        }

        function testGetPackageName() {
            let out = document.getElementById('deviceOutput');
            out.innerHTML = isBridgeAvailable() && window.Android.getPackageName ? "<b>Package:</b> " + window.Android.getPackageName() : "eformx.app";
        }

        function testIsNetworkAvailable() {
            let out = document.getElementById('deviceOutput');
            let online = isBridgeAvailable() && window.Android.isNetworkAvailable ? window.Android.isNetworkAvailable() : navigator.onLine;
            out.innerHTML = "<b>Network Status:</b> " + (online ? "<span class='badge-green'>ONLINE</span>" : "<span class='badge-red'>OFFLINE</span>");
        }

        function testGetDeviceInfo() {
            let out = document.getElementById('deviceOutput');
            if (isBridgeAvailable() && window.Android.getDeviceInfo) {
                try { out.innerHTML = "<pre>" + JSON.stringify(JSON.parse(window.Android.getDeviceInfo()), null, 2) + "</pre>"; }
                catch(e) { out.innerHTML = window.Android.getDeviceInfo(); }
            } else { out.innerHTML = "Open inside eFormX App"; }
        }

        function testGetLocation() {
            let out = document.getElementById('locationOutput');
            if (isBridgeAvailable() && window.Android.getLocation) {
                try {
                    let loc = JSON.parse(window.Android.getLocation());
                    out.innerHTML = !loc.error ? "<b>Lat:</b> " + loc.latitude + "<br><b>Lng:</b> " + loc.longitude + "<br><b>Accuracy:</b> " + loc.accuracy + "m" : loc.message;
                } catch(e) { out.innerHTML = window.Android.getLocation(); }
            } else { out.innerHTML = "Native getLocation not available"; }
        }

        function testHtml5Geolocation() {
            let out = document.getElementById('locationOutput');
            out.innerHTML = "Requesting HTML5 Geolocation...";
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    function(p) { out.innerHTML = "<b>HTML5 Lat:</b> " + p.coords.latitude + "<br><b>HTML5 Lng:</b> " + p.coords.longitude; },
                    function(e) { out.innerHTML = "Error: " + e.message; }
                );
            }
        }

        function testCheckLocationPermission() {
            let out = document.getElementById('locationOutput');
            let hasLoc = isBridgeAvailable() && window.Android.hasLocationPermission ? window.Android.hasLocationPermission() : false;
            out.innerHTML = "<b>Location Permission:</b> " + (hasLoc ? "<span class='badge-green'>GRANTED</span>" : "<span class='badge-red'>DENIED</span>");
        }

        function testOpenLocationPermission() {
            if (isBridgeAvailable() && window.Android.openLocationPermission) window.Android.openLocationPermission();
        }

        function testSpeak() {
            let text = document.getElementById('ttsInput').value;
            if (isBridgeAvailable() && window.Android.speak) window.Android.speak(text);
        }

        function testCheckPermissions() {
            let out = document.getElementById('permissionOutput');
            if (isBridgeAvailable()) {
                let hasLoc = window.Android.hasLocationPermission ? window.Android.hasLocationPermission() : false;
                let hasNotify = window.Android.hasNotificationPermission ? window.Android.hasNotificationPermission() : false;
                out.innerHTML = "<b>Location:</b> " + (hasLoc ? "<span class='badge-green'>ALLOWED</span>" : "<span class='badge-red'>PENDING</span>") +
                                "<br><b>Notifications:</b> " + (hasNotify ? "<span class='badge-green'>ALLOWED</span>" : "<span class='badge-red'>PENDING</span>");
            }
        }

        function testRequestAllPermissions() {
            if (isBridgeAvailable() && window.Android.requestAllPermissions) window.Android.requestAllPermissions();
        }

        function testWebShare() {
            let out = document.getElementById('shareOutput');
            if (navigator.share) {
                navigator.share({
                    title: 'eFormX Digital Portal',
                    text: 'Check out eFormX services for online applications!',
                    url: 'https://apply.eformx.com'
                }).then(function() {
                    out.innerHTML = "<b>Status:</b> <span class='badge-green'>SUCCESS: Share Sheet Opened</span>";
                }).catch(function(err) {
                    out.innerHTML = "<b>Status:</b> Cancelled or Error (" + err + ")";
                });
            } else if (window.AndroidShare) {
                window.AndroidShare.share('eFormX Digital Portal', 'Check out eFormX services!', 'https://apply.eformx.com');
                out.innerHTML = "<b>Status:</b> <span class='badge-green'>SUCCESS: AndroidShare Called</span>";
            } else {
                out.innerHTML = "<b>Status:</b> <span class='badge-red'>Web Share Not Supported</span>";
            }
        }
    </script>
</body>
</html>
```

---

### 🔗 Deep Links, `eformx://` Scheme & URL Query Parameters Reference

The eFormX native Android client includes built-in deep-link handling and dynamic URL query parameter routing in `MainActivity`.

#### 1️⃣ Custom `eformx://` Scheme & App Links

The app registers intent filters for both custom scheme (`eformx://`) and verified web domains (`https://apply.eformx.com` & `https://eformx.com`):

- **Custom Scheme URI Example:** `eformx://apply.eformx.com/form123`
- **Parsing Behavior:** `parseEformxUrl(rawUrl)` converts `eformx://` URIs to standard `https://` URLs and loads them directly inside the app WebView (`webView.loadUrl(targetUrl)`).
- **Duplicate Prevention:** Normalizes trailing slashes and checks current WebView URL to prevent redundant page reloads if the user is already on the target page.

```bash
# Test custom scheme deep-link via ADB CLI
adb shell am start -W -a android.intent.action.VIEW -d "eformx://apply.eformx.com/form123" eformx.app
```

---

#### 2️⃣ Dynamic URL Query Parameters (`isExternalBrowserRequested`)

Control browser navigation dynamically using query parameters attached to any URL:

| Standard Query Parameter | Supported Aliases / Variants | Behavior & App Action |
| :--- | :--- | :--- |
| **`browser=external`** | `browser=external` (also supports `browser=extrunal`) | **Forces External Browser:** Intercepts page load and opens the target URL in Chrome / Phone Default Browser (`Intent.ACTION_VIEW`). |
| **`callback=app`** | `callback=app` (also supports `calback=app`) | **Forces App WebView:** Overrides `browser=external` and keeps navigation inside the native App WebView. Ideal for payment callbacks and redirect URLs. |
| **`share_link=true`** | `share_link=true` | **Web Share Override:** Overrides external browser redirection to process Web Share sheets (`navigator.share`) directly inside app. |

##### Example Usage:

```html
<!-- Open link in external Chrome browser -->
<a href="https://external-site.com/docs?browser=external">Open in Chrome</a>

<!-- Payment gateway callback returning back to app WebView -->
<a href="https://apply.eformx.com/success.php?callback=app">Return to App</a>
```

---

### 🔔 Push Notifications & FCM Engine (`MyFirebaseMessagingService`)
- **FCM Data-Only High Priority Delivery:** Background & foreground push notification engine supporting custom titles, messages, big picture images, and custom target URLs.
- **Continuous Speech Announcements:** Text-to-Speech (TTS) engine (`speakOutText`) continuously loops speech announcements during call notifications.
- **Device Reboot / Restart Survival:** Declares `RECEIVE_BOOT_COMPLETED` permission and relies on Google Play Services daemon so push notifications and call alerts continue working 100% reliably even after phone restart or power-off.
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

Use a single unified API endpoint (`http://localhost:3000/api/send-notification`) to dispatch notifications. Customize payload behavior by adding or building upon the Base Default Schema.

#### 1️⃣ Base Default Payload (Standard Notification)
```json
{
  "title": "eFormX Digital Services",
  "message": "Your application update is available."
}
```

#### 2️⃣ Master Unified JSON Schema (All Supported Attributes)
```json
{
  "token": "OPTIONAL_SPECIFIC_USER_FCM_TOKEN",
  "topic": "all",
  "title": "eFormX Notification",
  "message": "You have a new update.",
  "target_url": "https://apply.eformx.com/status.php?id=123",
  "image_url": "https://apply.eformx.com/banner.jpg",
  "speak_text": "Hindi or English voice speech text",
  "audio_url": "http://eformx.com/sample.mp3",
  "sound_type": "notification",
  "open_type": "app_webview"
}
```

---

#### 3️⃣ All 7 Supported Notification Types & Payload Examples

##### 1. 📩 Standard Text Notification (Default Tone, No Voice)
```json
{
  "title": "eFormX Application Alert",
  "message": "Your eFormX digital application status has been updated."
}
```

##### 2. 🖼️ Big Picture / Banner Image Notification
```json
{
  "title": "eFormX Special Offer",
  "message": "Check out the latest digital scheme banner.",
  "image_url": "https://picsum.photos/800/400"
}
```

##### 3. 🗣️ Voice TTS Notification (Reads Text Out Loud, Silent Chime)
```json
{
  "title": "eFormX Voice Alert",
  "message": "Your form has been submitted successfully.",
  "speak_text": "Hello, your eFormX digital application has been submitted successfully."
}
```

##### 4. 🎵 Custom MP3 Audio Notification (Instant Stream Play, Silent Chime)
```json
{
  "title": "eFormX Custom Audio Alert",
  "message": "Playing custom online MP3 audio sound.",
  "audio_url": "http://eformx.com/sample.mp3"
}
```

##### 5. 📞 Incoming Call Alert Notification (Repeating Ringtone Loop + WakeLock)
```json
{
  "title": "📞 Incoming Call Request",
  "message": "Admin is calling from eFormX portal...",
  "sound_type": "ringtone",
  "speak_text": "Incoming call request from admin."
}
```

##### 6. ⏰ Urgent Alarm Notification (High Priority Alarm Category)
```json
{
  "title": "⏰ Critical Deadline Warning",
  "message": "Your document submission deadline is expiring today.",
  "sound_type": "alarm",
  "speak_text": "Attention! Your document submission deadline is expiring today."
}
```

##### 7. 🔗 Web / Deep Link Notification (Opens URL inside App WebView)
```json
{
  "title": "eFormX Web Portal",
  "message": "Tap to open the application portal.",
  "target_url": "https://apply.eformx.com"
}
```

---

#### 4️⃣ JSON Field Parameter Effect Table

| JSON Key / Parameter | Type | Default Value | Value Options / Example | Effect & App Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **`token`** | `String` | *(Empty)* | `"eX8kL1mN...xyz"` | **Single User Target:** When provided, notification is sent ONLY to this 1 specific user device. |
| **`topic`** | `String` | `"all"` | `"all"` | **Mass Unlimited Broadcast:** When `token` is omitted, broadcasts notification simultaneously to ALL registered users (Unlimited: 1 Lakh+ / 100,000+ devices via topic `"all"`). |
| **`title`** | `String` | `"eFormX Notification"` | `"eFormX Alert"` | Sets the bold header title displayed on the Android notification card. |
| **`message`** (or `body`) | `String` | `"You have a new update."` | `"Your form status updated."` | Sets the description text body on the notification card. |
| **`target_url`** (or `url`) | `String` | *(Empty)* | `"https://apply.eformx.com"` | **Target Webpage:** Tapping the notification card opens this specific webpage link inside the app WebView. |
| **`image_url`** (or `imageUrl`)| `String` | *(Empty)* | `"https://.../banner.jpg"` | **Banner Image:** Downloads and renders a full expandable Big Picture banner image on the notification card. |
| **`speak_text`** (or `tts_text`)| `String` | *(Empty)* | `"Hello, your form is submitted."` | **Text-to-Speech Output:** Triggers native Hindi/English voice speech out loud (bypasses default chime tone). |
| **`audio_url`** (or `audio`) | `String` | *(Empty)* | `"http://.../audio.mp3"` | **Remote MP3 Audio:** Streams and plays a custom online MP3 audio sound instantly on arrival. |
| **`sound_type`** (or `sound`)| `String` | `"notification"` | `"ringtone"` / `"call"` / `"alarm"` / `"notification"` | `"notification"` = Standard Beep; `"ringtone"` / `"call"` = High Priority Call Alert + Ringtone Loop; `"alarm"` = High Priority Alarm Category. |
| **`open_type`** | `String` | `"app_webview"` | `"app_webview"` | `"app_webview"` opens target URL inside app WebView. |

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
- **Compile SDK:** 37 (Android Latest)
- **Target SDK:** 37 (Android Latest)
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

The compiled **Debug APK** will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

To compile and assemble the **Production Release APK** (Optimized & Minified):

```bash
# On Windows PowerShell / Command Prompt
.\gradlew assembleRelease

# On Linux / macOS
./gradlew assembleRelease
```

The compiled **Release APK** will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## ⚙️ Configuration & Default Launch URL

The default web portal URL is loaded in `MainActivity.java`:
- **Default URL:** `https://apply.eformx.com`
