# eFormX Android Application

An ultra-fast, professional, hardware-accelerated Android WebView application for **eFormX Digital Services**. Built with native Java, modern AndroidX components, custom deep linking, Firebase Cloud Messaging (FCM) integration, hardware-accelerated rendering, offline handling, native JavaScript bridges, and smart background audio execution.

---

## 🌟 Key Features & How They Work

### 🚀 Performance & Rendering Engine
- **GPU Hardware Layer Acceleration:** Uses `View.LAYER_TYPE_HARDWARE` on WebView for 60fps smooth scrolling and instant page rendering.
- **Instant Top Progress Bar:** Sleek 2.5dp horizontal progress bar (`#4F46E5`) at the top of the WebView providing immediate 0ms visual tactile response on link clicks before page rendering begins.
- **Automatic Device Parameter Enrichment:** Transparently appends `platform_refrence=android&platform_refrence_id=<android_id>` to all in-app, deep link, and external browser navigation URLs.
- **Smart Memory Caching:** `WebSettings.LOAD_DEFAULT` optimized with DOM Storage, Web Database, and Cookie Persistence for high-speed dynamic loading.
- **HTML5 Geolocation Support:** Hardware-accelerated geolocation enabled with custom `WebChromeClient` callback (`onGeolocationPermissionsShowPrompt`) for seamless `navigator.geolocation.getCurrentPosition(...)` calls.
- **Native GPS Hardware Location Provider:** Direct native satellite fix (`ACCESS_FINE_LOCATION`) query to fetch high-precision coordinates instantly for web forms and maps.
- **Dynamic URL Routing on Launch:** `SplashActivity` asynchronously checks `https://api.eformx.in/?api=install/app` and dynamically loads the server's `redirect_url` into the WebView.

---

### 📱 Android JavaScript Interface (`AndroidBridge`)
Exposes `Android` object to WebView JavaScript allowing web pages to retrieve GPS coordinates, device info, device ID, app version, package name, network status, trigger Text-To-Speech output, and request location permissions:

| Method | Parameters | Return Type | Description | JavaScript Usage Examp<br/>+le                                            |
| :--- | :--- | :--- | :--- |:--------------------------------------------------------------------------|
| **`Android.getFcmToken()`** | None | `String` | Returns device's Firebase Push Notification Registration Token string | `let token = Android.getFcmToken();`                                      |
| **`Android.getLocation()`** | None | `String` (JSON) | Returns GPS Latitude, Longitude, Accuracy, Altitude, Speed, Time JSON | `let loc = JSON.parse(Android.getLocation());`                            |
| **`Android.getDeviceInfo()`** | None | `String` (JSON) | Returns JSON string with hardware specs (Android ID, Manufacturer, Brand, Model, Device, Product, OS Version, SDK Level, Language, Country, TimeZone, Screen Dimensions, App Version, Package Name, FCM Token) | `let info = JSON.parse(Android.getDeviceInfo());`                         |
| **`Android.getDeviceId()`** | None | `String` | Returns unique Android ID string (`Settings.Secure.ANDROID_ID`) | `let id = Android.getDeviceId();`                                         |
| **`Android.getAppVersion()`** | None | `String` | Returns App Version Name (e.g. `"1.5"`) | `let ver = Android.getAppVersion();`                                      |
| **`Android.getPackageName()`** | None | `String` | Returns Package Identifier (`"eformx.app"`) | `let pkg = Android.getPackageName();`                                     |
| **`Android.getIpAddress()`** | None | `String` | Returns device local IPv4 address string (e.g. `"192.168.1.35"`) | `let ip = Android.getIpAddress();`                                        |
| **`Android.getNetworkType()`** | None | `String` | Returns network type (`"WIFI"`, `"CELLULAR_MOBILE"`, `"OFFLINE"`) | `let netType = Android.getNetworkType();`                                 |
| **`Android.getNetworkOperator()`** | None | `String` | Returns SIM carrier operator name (e.g. `"Jio"`, `"Airtel"`) | `let op = Android.getNetworkOperator();`                                  |
| **`Android.isNetworkAvailable()`** | None | `boolean` | Returns active internet connection state (`true`/`false`) | `let online = Android.isNetworkAvailable();`                              |
| **`Android.speak(text)`** | `text` (String) | `void` | Speaks text using native Android Text-to-Speech engine | `Android.speak('Hello from eFormX');`                                     |
| **`Android.hasLocationPermission()`** | None | `boolean` | Checks if Location permission (`ACCESS_FINE_LOCATION`) is granted | `let hasLoc = Android.hasLocationPermission();`                           |
| **`Android.hasNotificationPermission()`** | None | `boolean` | Checks if Notification permission (`POST_NOTIFICATIONS` / app notification setting) is enabled | `let hasNotify = Android.hasNotificationPermission();`                    |
| **`Android.openNotificationPermission()`** | None | `void` | Prompts native system notification permission dialog (or opens App Notification Settings if denied) | `Android.openNotificationPermission();`                                   |
| **`Android.openNotificationSettings()`** | None | `void` | Directly opens system App Notification Settings page | `Android.openNotificationSettings();`                                     |
| **`Android.requestAllPermissions()`** | None | `void` | Triggers prompt for all missing permissions (Notifications, Location GPS) | `Android.requestAllPermissions();`                                        |
| **`Android.exitApp()`** | None | `void` | Triggers the native eFormX Exit Confirmation Dialog directly | `Android.exitApp();`                                                      |
| **`Android.openLocationPermission()`** | None | `void` | Prompts system location permission dialog (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) | `Android.openLocationPermission();`                                       |
| **`Android.showProcessLoader(page)`** | `page` (String, optional) | `void` | Triggers native full-screen 'Processing your request...' loading overlay and top progress bar | `Android.showProcessLoader('property');`                                  |
| **`AndroidShare.share(title, text, url)`** | `title`, `text`, `url` (Strings) | `void` | Triggers native Android system Share Sheet intent to share links/text via WhatsApp, Email, Messages, etc. | `AndroidShare.share('eFormX', 'Check out eFormX', 'https://eformx.com');` |
| **`AndroidShare.reloadApp()`** | None | `void` | Refreshes and reloads the active portal URL in WebView on the UI thread | `AndroidShare.reloadApp();`                                               |

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

#### 5. Trigger Native App Exit Dialog via JavaScript (`Android.exitApp`)
Web portals can trigger the native eFormX exit dialog when a user clicks "Logout" or an in-page "Exit" button:

```javascript
if (window.Android && window.Android.exitApp) {
    window.Android.exitApp();
}
```

#### 6. WebView Cache & Storage Management
When web developers need to flush client cache, session data, or force clean state:

```javascript
// Example: Clear web storage (localStorage, sessionStorage)
localStorage.clear();
sessionStorage.clear();

// In Native Android (Java), cache can be flushed completely via:
// webView.clearCache(true);
// android.webkit.WebStorage.getInstance().deleteAllData();
// android.webkit.CookieManager.getInstance().removeAllCookies(null);
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

        <!-- 5. Deep Links, Process Loader & External Navigation -->
        <div class="card">
            <h2>🔗 Deep Links, Process Loader & Navigation</h2>
            <a href="https://eformx.in" data-android="open" data-page="property" class="link-item" style="color: #38bdf8; font-weight: bold;">🏠 Open Property (data-android="open" data-page="property")</a>
            <a href="https://apply.eformx.com" data-android="open" data-page="kyc verification" class="link-item" style="color: #38bdf8; font-weight: bold;">📋 Open KYC Portal (data-android="open" data-page="kyc verification")</a>
            <a href="https://apply.eformx.com/portal.php?browser=external" class="link-item">🌐 Open in Chrome External Browser (?browser=external)</a>
            <a href="https://apply.eformx.com/logout?android=exit" class="link-item" style="color: #f43f5e;">🚪 Exit Intercept Test (?android=exit)</a>
            <a href="eformx://apply.eformx.com/status?id=1001" class="link-item">🚀 Test Custom Scheme (eformx://apply.eformx.com/status?id=1001)</a>
            <a href="eformx://https://eformx.com/" class="link-item">🚀 Test Explicit HTTPS Scheme (eformx://https://eformx.com/)</a>
            <a href="https://wa.me/919876543210?text=Namaste%20eFormX" class="link-item">💬 Open WhatsApp Native Chat</a>
            <button class="btn btn-purple" style="margin-top: 8px;" onclick="testShowProcessLoader()">⚙️ Trigger JS Loader: Android.showProcessLoader('Property')</button>
        </div>

        <!-- 6. Platform Reference URL Parameter Status -->
        <div class="card">
            <h2>🔍 Auto Platform Reference Parameters</h2>
            <div id="urlParamStatus" class="output-box">Inspecting current URL...</div>
        </div>

        <!-- 7. Web Share API (Android Native System Share) -->
        <div class="card">
            <h2>📤 Native Web Share API & Exit Trigger</h2>
            <button class="btn btn-primary" onclick="testWebShare()">Share via Android Native Sheet</button>
            <button class="btn btn-warning" onclick="testExitApp()">Trigger Native Exit App Dialog</button>
            <div id="shareOutput" class="output-box">Status will appear here...</div>
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

        function testExitApp() {
            if (isBridgeAvailable() && window.Android.exitApp) {
                window.Android.exitApp();
            } else {
                alert("Native Android bridge not available");
            }
        }

        function testShowProcessLoader() {
            if (isBridgeAvailable() && window.Android.showProcessLoader) {
                window.Android.showProcessLoader('Property Search');
            } else {
                alert("Android.showProcessLoader is only available inside eFormX App");
            }
        }

        window.addEventListener('DOMContentLoaded', function() {
            var params = new URLSearchParams(window.location.search);
            var platform = params.get('platform_refrence');
            var platformId = params.get('platform_refrence_id');
            var el = document.getElementById('urlParamStatus');
            if (el) {
                if (platform === 'android' && platformId) {
                    el.innerHTML = "<b>Status:</b> <span class='badge-green'>ACTIVE (ENRICHED)</span><br>" +
                                   "<b>platform_refrence:</b> " + platform + "<br>" +
                                   "<b>platform_refrence_id:</b> " + platformId;
                } else {
                    el.innerHTML = "<b>Status:</b> <span class='badge-red'>MISSING IN CURRENT URL</span><br>" +
                                   "Note: Click any link to test auto-injection!";
                }
            }
        });
    </script>
</body>
</html>
```

---

### 🔗 Deep Links, `eformx://` Scheme & URL Query Parameters Reference

The eFormX native Android client includes built-in deep-link handling and dynamic URL query parameter routing in `MainActivity`.

#### 1️⃣ Custom `eformx://` Scheme & App Links

The app registers intent filters for both custom scheme (`eformx://`) and verified web domains (`https://apply.eformx.com` & `https://eformx.com`):

- **Supported Formats:**
  - `eformx://apply.eformx.com/status?id=1001` (Scheme-less domain)
  - `eformx://https://eformx.com/` (Explicit HTTPS prefix)
  - `eformx://eformx.com/dashboard` (Direct domain target)
- **Parsing Behavior:** `parseEformxUrl(rawUrl)` converts all `eformx://` URIs to standard `https://` URLs, attaches Android identification parameters, and loads them directly inside the app WebView (`webView.loadUrl(targetUrl)`).
- **Duplicate Prevention:** Normalizes trailing slashes and checks current WebView URL to prevent redundant page reloads if the user is already on the target page.

```bash
# Test custom scheme deep-link via ADB CLI
adb shell am start -W -a android.intent.action.VIEW -d "eformx://apply.eformx.com/form123" eformx.app
```

---

#### 2️⃣ Automatic Platform Reference Parameters (`platform_refrence` & `platform_refrence_id`)

Every URL handled by the application—including initial app launch, internal link clicks, custom deep-links, and external browser redirections—is automatically enriched with device identifier parameters:

| Query Parameter | Value Format | Description |
| :--- | :--- | :--- |
| **`platform_refrence`** | `android` | Identifies client platform as Android OS. |
| **`platform_refrence_id`** | `<64-bit Hex Android ID>` | Unique device identifier (`Settings.Secure.ANDROID_ID`, e.g. `31a542b89ce14f20`). |

- **Example Input:** `https://apply.eformx.com/status?id=1001`
- **Enriched Output:** `https://apply.eformx.com/status?id=1001&platform_refrence=android&platform_refrence_id=31a542b89ce14f20`
- **Duplicate Safe:** If the URL already contains `platform_refrence`, parameters are preserved without duplicate appending.

---

#### 3️⃣ HTML DOM Hook: `data-android="open"` (Instant Native Process Loader)

Web developers can trigger the native full-screen **"Processing your request..."** overlay on any link or button simply by adding the `data-android="open"` attribute:

```html
<!-- Native Process Loader automatically triggers on click -->
<a href="https://eformx.in" data-android="open" data-page="property">
    Open Property
</a>
```

- **Attributes Supported:**
  - `data-android="open"`: Enables instant 0ms native full-screen loader + top progress bar.
  - `data-page="<name>"` *(optional)*: Customizes the loader title to `Processing your request (<Name>)...`.
- **Auto-Dismiss:** Automatically hides when the target page finishes rendering (`onPageFinished`), or after the 5-second safety watchdog timer.

---

#### 4️⃣ Dynamic URL Query Parameters (`isExternalBrowserRequested`)

Control browser navigation dynamically using query parameters attached to any URL:

| Standard Query Parameter | Supported Aliases / Variants | Behavior & App Action |
| :--- | :--- | :--- |
| **`browser=external`** | `browser=external` (also supports `browser=extrunal`) | **Forces External Browser:** Intercepts page load, appends platform reference parameters, and opens the target URL in Chrome / Phone Default Browser (`Intent.ACTION_VIEW` / Custom Tabs). |
| **`callback=app`** | `callback=app` (also supports `calback=app`) | **Forces App WebView:** Overrides `browser=external` and keeps navigation inside the native App WebView. Ideal for payment callbacks and redirect URLs. |
| **`share_link=true`** | `share_link=true` | **Web Share Override:** Overrides external browser redirection to process Web Share sheets (`navigator.share`) directly inside app. |
| **`android=exit`** | `android=exit` | **Immediate Exit Intercept:** When attached to any URL (e.g. `logout.php?android=exit`), hardware back-button press bypasses WebView history traversal and directly triggers the native **eFormX Exit Confirmation Dialog**. |

##### 3️⃣ HTML Testing Snippets (Ready to Use)

###### 📄 HTML Code 1: Custom Deep Link (`eformx://`) Testing Page
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>eFormX Deep Link Test</title>
    <style>
        body { font-family: sans-serif; padding: 20px; line-height: 1.6; }
        .btn { display: inline-block; background: #007bff; color: #fff; padding: 12px 20px; text-decoration: none; border-radius: 6px; margin: 10px 0; font-weight: bold; }
    </style>
</head>
<body>
    <h2>🔗 eFormX Deep Link Testing</h2>
    <p>Tap below links inside Chrome or another app to trigger eFormX app opening:</p>
    
    <!-- Test eformx:// Custom Scheme -->
    <a href="eformx://apply.eformx.com/status?id=1001" class="btn">Test eformx:// Custom Deep Link</a>
    
    <!-- Test HTTPS App Link -->
    <a href="https://apply.eformx.com/status?id=1001" class="btn" style="background: #28a745;">Test HTTPS App Link</a>
</body>
</html>
```

###### 📄 HTML Code 2: URL Query Parameters (`browser=external`, `callback=app`, & `android=exit`) Testing Page
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>eFormX Query Parameters Test</title>
    <style>
        body { font-family: sans-serif; padding: 20px; line-height: 1.6; }
        .btn { display: block; background: #17a2b8; color: #fff; padding: 14px 20px; text-decoration: none; border-radius: 6px; margin: 12px 0; text-align: center; font-weight: bold; }
        .external { background: #dc3545; }
        .callback { background: #28a745; }
        .exit { background: #e11d48; }
    </style>
</head>
<body>
    <h2>🚀 eFormX URL Query Parameter Test</h2>

    <!-- Test 1: Open in External Chrome Browser -->
    <a href="https://google.com?browser=external" class="btn external">1. Open Google in Chrome (browser=external)</a>

    <!-- Test 2: Stay/Return inside App WebView -->
    <a href="https://apply.eformx.com/success.php?callback=app" class="btn callback">2. Return to App WebView (callback=app)</a>

    <!-- Test 3: Exit Intercept Test -->
    <a href="https://apply.eformx.com/logout?android=exit" class="btn exit">3. Test Exit Intercept (android=exit)</a>

    <!-- Test 4: Legacy Spellings Test -->
    <a href="https://eformx.com?browser=extrunal" class="btn external">4. Test Legacy Spelling (browser=extrunal)</a>
    <a href="https://eformx.com?calback=app" class="btn callback">5. Test Legacy Callback (calback=app)</a>
</body>
</html>
```

###### 📄 HTML Code 3: Complete All-In-One Feature Test Page (`all-features-test.html`)
Save or host this single HTML page to test 100% of eFormX Android App features simultaneously:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>eFormX Complete Feature Test Portal</title>
    <style>
        :root {
            --primary: #4F46E5;
            --primary-light: #EEF2FF;
            --success: #10B981;
            --danger: #EF4444;
            --warning: #F59E0B;
            --dark: #0F172A;
            --card-bg: #1E293B;
            --border: #334155;
            --text-main: #F8FAFC;
            --text-muted: #94A3B8;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--dark); color: var(--text-main); padding: 16px 16px 50px 16px; line-height: 1.5; }
        .header { text-align: center; margin-bottom: 20px; padding: 12px; border-bottom: 1px solid var(--border); }
        .header h1 { font-size: 20px; color: #fff; margin-bottom: 4px; }
        .badge { display: inline-block; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: bold; }
        .badge-native { background: #064E3B; color: #6EE7B7; border: 1px solid #059669; }
        .badge-web { background: #78350F; color: #FCD34D; border: 1px solid #D97706; }
        .badge-active { background: #064E3B; color: #6EE7B7; }
        .badge-missing { background: #7F1D1D; color: #FCA5A5; }
        .card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 14px; padding: 16px; margin-bottom: 16px; }
        .card h2 { font-size: 15px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; color: #E2E8F0; }
        .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 13px; }
        .info-row:last-child { border-bottom: none; }
        .info-label { color: var(--text-muted); }
        .info-value { font-family: monospace; color: #38BDF8; word-break: break-all; text-align: right; max-width: 60%; }
        .btn-grid { display: grid; grid-template-columns: 1fr; gap: 10px; margin-top: 8px; }
        .btn { display: flex; align-items: center; justify-content: center; gap: 8px; width: 100%; padding: 13px 16px; border-radius: 10px; font-size: 14px; font-weight: 600; text-decoration: none; border: none; cursor: pointer; text-align: center; color: #fff; transition: opacity 0.2s; }
        .btn:active { opacity: 0.8; transform: scale(0.98); }
        .btn-primary { background: var(--primary); }
        .btn-success { background: var(--success); }
        .btn-danger { background: var(--danger); }
        .btn-warning { background: var(--warning); color: #000; }
        .btn-outline { background: transparent; border: 1px solid var(--border); color: #E2E8F0; }
        .param-box { background: rgba(0,0,0,0.25); border-radius: 8px; padding: 10px; margin-top: 8px; font-size: 12px; font-family: monospace; word-break: break-all; }
    </style>
</head>
<body>

    <!-- Header -->
    <div class="header">
        <h1>🚀 eFormX All-In-One Feature Testbed</h1>
        <div id="bridgeBadge" class="badge badge-native">Detecting Native App...</div>
    </div>

    <!-- 1. Live Platform Reference Parameters Inspector -->
    <div class="card">
        <h2>🔍 1. Automatic Platform URL Parameters</h2>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 10px;">
            Verifies that current URL contains auto-injected device reference parameters:
        </p>
        <div class="info-row">
            <span class="info-label">platform_refrence:</span>
            <span id="paramPlatform" class="info-value">Checking...</span>
        </div>
        <div class="info-row">
            <span class="info-label">platform_refrence_id:</span>
            <span id="paramPlatformId" class="info-value">Checking...</span>
        </div>
        <div class="info-row">
            <span class="info-label">Inspection Status:</span>
            <span id="paramStatus" class="badge">Checking...</span>
        </div>
        <div class="param-box" id="fullUrlDisplay">Full URL: ...</div>
    </div>

    <!-- 2. Native Process Loader Hook (data-android="open") -->
    <div class="card">
        <h2>⚡ 2. Native Full-Screen Process Loader Hook</h2>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 10px;">
            Links with <code>data-android="open"</code> trigger instant 0ms full-screen "Processing your request..." loader before navigating:
        </p>
        <div class="btn-grid">
            <!-- Property Page Link -->
            <a href="https://eformx.in" data-android="open" data-page="property" class="btn btn-primary">
                🏠 Open Property (data-page="property")
            </a>
            <!-- KYC Portal Link -->
            <a href="https://apply.eformx.com" data-android="open" data-page="kyc verification" class="btn btn-primary">
                📋 Open KYC (data-page="kyc verification")
            </a>
            <!-- Direct JavaScript Trigger -->
            <button onclick="triggerJsLoader()" class="btn btn-outline">
                ⚙️ Direct JS Call: Android.showProcessLoader('Loan Request')
            </button>
        </div>
    </div>

    <!-- 3. Deep Link Schemes (eformx://) -->
    <div class="card">
        <h2>🔗 3. Deep Link Schemes (eformx://)</h2>
        <p style="font-size: 12px; color: var(--text-muted); margin-bottom: 10px;">
            Test custom URL schemes resolved by parseEformxUrl():
        </p>
        <div class="btn-grid">
            <a href="eformx://apply.eformx.com/status?id=1001" class="btn btn-success">
                🔗 Scheme-less: eformx://apply.eformx.com/status?id=1001
            </a>
            <a href="eformx://https://eformx.com/" class="btn btn-success">
                🔗 Explicit HTTPS: eformx://https://eformx.com/
            </a>
            <a href="eformx://eformx.com/dashboard" class="btn btn-success">
                🔗 Direct Domain: eformx://eformx.com/dashboard
            </a>
        </div>
    </div>

    <!-- 4. Dynamic URL Query Parameters -->
    <div class="card">
        <h2>🌐 4. Browser Navigation Query Parameters</h2>
        <div class="btn-grid">
            <!-- External Browser with Auto-Enriched Params -->
            <a href="https://auth.eformx.in/?auth_url=https://shubharambhrealty.eformx.com/custumer&browser=external" class="btn btn-warning">
                🌐 Open in Chrome / CustomTabs (browser=external)
            </a>
            <!-- Keep in App WebView -->
            <a href="https://apply.eformx.com/?callback=app" class="btn btn-outline">
                📲 Stay in App WebView (callback=app)
            </a>
            <!-- Exit Confirmation Dialog Intercept -->
            <a href="https://apply.eformx.com/logout?android=exit" class="btn btn-danger">
                🚪 Trigger Exit Dialog (android=exit)
            </a>
        </div>
    </div>

    <!-- 5. Native Hardware & JavaScript Bridge Controls -->
    <div class="card">
        <h2>📱 5. Native Android Bridge & Sensors</h2>
        <div class="info-row">
            <span class="info-label">Android ID:</span>
            <span id="txtDeviceId" class="info-value">--</span>
        </div>
        <div class="info-row">
            <span class="info-label">FCM Token:</span>
            <span id="txtFcmToken" class="info-value">--</span>
        </div>
        <div class="info-row">
            <span class="info-label">Network Type:</span>
            <span id="txtNetwork" class="info-value">--</span>
        </div>
        <div class="info-row">
            <span class="info-label">App Version:</span>
            <span id="txtAppVer" class="info-value">--</span>
        </div>
        <div class="info-row">
            <span class="info-label">GPS Fix:</span>
            <span id="txtGpsLoc" class="info-value">Tap Get GPS below</span>
        </div>

        <div class="btn-grid" style="margin-top: 14px;">
            <button onclick="fetchNativeLocation()" class="btn btn-primary">📍 1. Fetch GPS Satellite Fix</button>
            <button onclick="testTtsSpeak()" class="btn btn-outline">🗣️ 2. Test Text-to-Speech (Hindi/English)</button>
            <button onclick="testNativeShare()" class="btn btn-success">📤 3. Open Native Android Share Sheet</button>
            <button onclick="requestPermissions()" class="btn btn-outline">🔐 4. Request System Permissions</button>
        </div>
    </div>

    <script>
        // Run on Page Load
        document.addEventListener('DOMContentLoaded', function() {
            inspectUrlParams();
            detectBridge();
        });

        // 1. Inspect URL parameters
        function inspectUrlParams() {
            var urlParams = new URLSearchParams(window.location.search);
            var platform = urlParams.get('platform_refrence');
            var platformId = urlParams.get('platform_refrence_id');

            document.getElementById('fullUrlDisplay').innerText = "Full URL: " + window.location.href;
            document.getElementById('paramPlatform').innerText = platform ? platform : "Not Present";
            document.getElementById('paramPlatformId').innerText = platformId ? platformId : "Not Present";

            var statusEl = document.getElementById('paramStatus');
            if (platform === 'android' && platformId) {
                statusEl.innerText = "ACTIVE (ENRICHED)";
                statusEl.className = "badge badge-active";
            } else {
                statusEl.innerText = "MISSING PARAMETERS";
                statusEl.className = "badge badge-missing";
            }
        }

        // 2. Detect Android Bridge
        function detectBridge() {
            var badge = document.getElementById('bridgeBadge');
            if (window.Android) {
                badge.innerText = "Connected to Native eFormX App";
                badge.className = "badge badge-native";

                // Populate Specs
                if (window.Android.getDeviceId) document.getElementById('txtDeviceId').innerText = window.Android.getDeviceId();
                if (window.Android.getFcmToken) document.getElementById('txtFcmToken').innerText = window.Android.getFcmToken().substring(0, 20) + "...";
                if (window.Android.getNetworkType) document.getElementById('txtNetwork').innerText = window.Android.getNetworkType();
                if (window.Android.getAppVersion) document.getElementById('txtAppVer').innerText = window.Android.getAppVersion();
            } else {
                badge.innerText = "Running in Standard Web Browser";
                badge.className = "badge badge-web";
            }
        }

        // 3. Trigger JS Loader Directly
        function triggerJsLoader() {
            if (window.Android && window.Android.showProcessLoader) {
                window.Android.showProcessLoader('Loan Request');
            } else {
                alert('window.Android.showProcessLoader is only available inside native Android app');
            }
        }

        // 4. GPS Fix
        function fetchNativeLocation() {
            if (window.Android && window.Android.getLocation) {
                try {
                    var loc = JSON.parse(window.Android.getLocation());
                    if (!loc.error) {
                        document.getElementById('txtGpsLoc').innerText = loc.latitude.toFixed(5) + ", " + loc.longitude.toFixed(5) + " (±" + loc.accuracy + "m)";
                    } else {
                        document.getElementById('txtGpsLoc').innerText = "Err: " + loc.message;
                        if (window.Android.openLocationPermission) window.Android.openLocationPermission();
                    }
                } catch(e) {
                    document.getElementById('txtGpsLoc').innerText = "Parse Error";
                }
            } else {
                alert('Native GPS bridge unavailable');
            }
        }

        // 5. Text to Speech
        function testTtsSpeak() {
            if (window.Android && window.Android.speak) {
                window.Android.speak("ई-फॉर्म-एक्स एंड्रॉइड ऐप में आपका स्वागत है। टेस्ट सफल रहा।");
            } else {
                alert('Native TTS unavailable');
            }
        }

        // 6. Native Share Sheet
        function testNativeShare() {
            if (navigator.share) {
                navigator.share({
                    title: 'eFormX App Feature Test',
                    text: 'Testing all eFormX Android features!',
                    url: window.location.href
                }).catch(function(){});
            } else if (window.AndroidShare && window.AndroidShare.share) {
                window.AndroidShare.share('eFormX Test', 'Testing eFormX App!', window.location.href);
            } else {
                alert('Share sheet unavailable');
            }
        }

        // 7. Request Permissions
        function requestPermissions() {
            if (window.Android && window.Android.requestAllPermissions) {
                window.Android.requestAllPermissions();
            } else {
                alert('Permission API unavailable');
            }
        }
    </script>
</body>
</html>
```

---

---

### 🧹 WebView Cache & Storage Management Reference

Android WebView caches web content across multiple layers: **HTTP RAM & Disk Cache**, **DOM LocalStorage / SessionStorage**, **IndexedDB / Web Databases**, and **Cookies**.

#### 1. Native Android Cache Clearing APIs (Java)

| Cache Component | Native Java Code | Purpose |
| :--- | :--- | :--- |
| **HTTP Memory & Disk Cache** | `webView.clearCache(true);` | Deletes temporary images, CSS, JavaScript, and HTML cached files from RAM and storage. |
| **DOM Storage & IndexedDB** | `WebStorage.getInstance().deleteAllData();` | Clears all `localStorage`, `sessionStorage`, and client databases saved by websites. |
| **Cookies & Sessions** | `CookieManager.getInstance().removeAllCookies(null);`<br>`CookieManager.getInstance().flush();` | Destroys all stored login cookies, session tokens, and tracking cookies. |
| **History & Form Autocomplete** | `webView.clearHistory();`<br>`webView.clearFormData();` | Resets back-forward navigation stack and removes saved form inputs. |
| **App Internal Cache Directory** | `deleteDir(context.getCacheDir());` | Removes deep physical cache files located in `/data/data/eformx.app/cache/`. |

#### 2. Deep Clean Helper Implementation
```java
public void clearAllWebViewData(WebView webView, Context context) {
    if (webView != null) {
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
    }
    // Clear DOM databases and localStorage
    android.webkit.WebStorage.getInstance().deleteAllData();
    
    // Clear cookies
    android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
    cookieManager.removeAllCookies(null);
    cookieManager.flush();
    
    // Clear physical cache directory
    try {
        java.io.File cacheDir = context.getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            deleteDirectory(cacheDir);
        }
    } catch (Exception ignored) {}
}

private boolean deleteDirectory(java.io.File dir) {
    if (dir != null && dir.isDirectory()) {
        String[] children = dir.list();
        if (children != null) {
            for (String child : children) {
                boolean success = deleteDirectory(new java.io.File(dir, child));
                if (!success) return false;
            }
        }
        return dir.delete();
    } else if (dir != null && dir.isFile()) {
        return dir.delete();
    }
    return false;
}
```

#### 3. WebView Cache Mode Modes in `WebSettings`

| Cache Mode | Code | Behavior |
| :--- | :--- | :--- |
| **`LOAD_DEFAULT`** | `webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);` | Standard HTTP cache: checks cache validity against server headers before loading. |
| **`LOAD_CACHE_ELSE_NETWORK`** | `webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);` | Loads from cache if available (instant millisecond rendering); only hits network if cache is absent or expired. |
| **`LOAD_NO_CACHE`** | `webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);` | Completely bypasses local cache; forces network request for every single asset. |
| **`LOAD_CACHE_ONLY`** | `webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);` | Never hits network; relies strictly on locally cached data (offline mode). |

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

### 📡 Master FCM Notification Engine & Payload Guide

eFormX Android App incorporates an enterprise-grade push notification handler (`MyFirebaseMessagingService`) engineered for maximum background reliability, voice accessibility, and rich interactive media.

> [!IMPORTANT]
> **CRITICAL: Always Send as DATA-ONLY Message (`message.data`)**
> To ensure notifications trigger background Text-to-Speech (TTS), wake the screen/CPU, and play custom ringtones when the app is **closed / background / killed**, do **NOT** use the top-level Firebase `notification: {...}` object in production. Always send your payload inside `data: {...}` with `priority: "HIGH"`. When sent as Data-Only, Android's `onMessageReceived()` is guaranteed to run.

---

#### 🎛️ Special Audio & Call Features Implemented

1. **📞 Smart Call vs Speech Priority:**
   - When `sound_type: "call"` is received **with speech** (`"speetch": "..."`), the phone's standard loud bell ringtone is automatically turned **OFF**, and the announcement voice speaks out clearly so the user understands who is calling.
   - When `sound_type: "call"` is received **without speech**, the continuous call ringtone loops until answered or dismissed.
2. **🔊 Silent / Vibrate Mode Bypass:**
   - Voice announcements utilize `AudioManager.STREAM_ALARM` with `AudioAttributes.USAGE_ALARM`. This ensures speech is spoken aloud even if the user's phone is currently on **Silent** or **Vibrate** mode.
3. **🔇 Instant Mute via Hardware Volume Buttons:**
   - If user presses **Volume Up** or **Volume Down** button anywhere while the voice or ringtone is playing, the audio stops immediately.
4. **👆 Instant Stop on Swipe or App Open:**
   - Swiping away the notification card (`NotificationDismissReceiver`) or opening the app immediately silences all background speech and ringtones.

---

### 📦 Payload Schemas for Every Notification Type

---

#### 1️⃣ 📞 Live Call Alert + Voice Speech (Ringtone OFF, Clear Speech, Silent Bypass)
Use this when an agent/admin wants to trigger an urgent call alert with custom voice message:

```json
{
  "token": "cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...",
  "title": "📞 एडमिन लाइव कॉल अलर्ट",
  "body": "एडमिन आपसे तुरंत बात करना चाहते हैं।",
  "speetch": "एडमिन आपसे तुरंत बात करना चाहते हैं। कृपया ई-फॉर्म-एक्स ऐप खोलें।",
  "sound_type": "call",
  "target_url": "https://eformx.com/call/room123",
  "open_type": "app_webview"
}
```

* **Behavior:** Ringtone stays OFF, voice speaks aloud (even if phone is on Silent/Vibrate). Volume button or swipe cancels immediately. Tapping opens video/audio call page inside app WebView.

---

#### 2️⃣ 📞 Incoming Call Alert with Repeating Ringtone Only (No Speech)
Use this for a standard ringing call notification:

```json
{
  "token": "cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...",
  "title": "📞 Incoming Call",
  "body": "Incoming call from Support Team...",
  "sound_type": "call",
  "target_url": "https://eformx.com/call/support",
  "open_type": "app_webview"
}
```

* **Behavior:** Loops phone's default ringtone continuously, wakes the screen, and stays until dismissed or accepted.

---

#### 3️⃣ 🗣️ Pure Voice Speech Announcement (Hindi / English TTS)
Use this to read out news, transactional updates, or order confirmations aloud to the user:

```json
{
  "token": "cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...",
  "title": "📢 ज़रूरी सूचना",
  "body": "रमेश जी, आपका नया प्रॉपर्टी डॉक्यूमेंट अपलोड हो चुका है।",
  "speetch": "नमस्ते रमेश जी, आपका नया प्रॉपर्टी डॉक्यूमेंट अपलोड हो चुका है। कृपया ई-फॉर्म-एक्स ऐप खोलें।",
  "sound_type": "notification",
  "target_url": "https://eformx.com/documents",
  "open_type": "app_webview"
}
```

* **Behavior:** Notification card appears and the phone clearly speaks the text in Hindi/English. Standard notification beep is suppressed so speech sounds clean.

---

#### 4️⃣ 📢 Mass Broadcast to ALL App Users (Unlimited 100,000+ Devices)
To broadcast a message to every user without specifying individual tokens, target the topic `"all"`:

```json
{
  "topic": "all",
  "title": "🎉 दीपावली विशेष ऑफर!",
  "body": "ई-फॉर्म-एक्स की सभी सेवाओं पर फ्लैट 50% छूट। आज ही आवेदन करें!",
  "speetch": "दीपावली की हार्दिक शुभकामनाएं! ई-फॉर्म-एक्स पर आज विशेष छूट उपलब्ध है।",
  "image_url": "https://eformx.com/banners/diwali-offer.jpg",
  "target_url": "https://eformx.com/offers",
  "open_type": "app_webview"
}
```

* **Behavior:** Every single user who has the eFormX app installed receives this notification simultaneously.

---

#### 5️⃣ 🖼️ Big Picture / Banner Image Card
Use this to display promotional marketing banners or product posters:

```json
{
  "topic": "all",
  "title": "📄 नया सरकारी फॉर्म लाइव!",
  "body": "बिहार स्कॉलरशिप 2026 के लिए ऑनलाइन फॉर्म शुरू हो चुके हैं।",
  "image_url": "https://eformx.com/images/scholarship_banner.png",
  "target_url": "https://eformx.com/scholarship-apply",
  "open_type": "app_webview"
}
```

* **Behavior:** Renders an expandable, rich HD banner graphic card directly in the notification shade.

---

#### 6️⃣ 🎵 Custom Remote MP3 Streaming Sound
Use this to play a custom branded chime, audio message, or notification tune:

```json
{
  "token": "cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...",
  "title": "💳 भुगतान सफल!",
  "body": "आपका ₹500 का भुगतान सफलतापूर्वक प्राप्त हुआ।",
  "audio_url": "https://eformx.com/sounds/payment_success.mp3",
  "target_url": "https://eformx.com/transactions/tx987",
  "open_type": "app_webview"
}
```

* **Behavior:** Streams and plays the remote MP3 file in the background immediately upon notification arrival.

---

#### 7️⃣ 🌍 External Browser Link Notification
Use this when you want the notification tap to open outside the app (in Chrome / default browser):

```json
{
  "token": "cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...",
  "title": "🌐 बाह्य वेबसाइट लिंक",
  "body": "सरकारी पोर्टल पर जाने के लिए यहाँ क्लिक करें।",
  "target_url": "https://uidai.gov.in",
  "open_type": "browser"
}
```

* **Behavior:** Tapping the notification launches the URL in the user's external web browser instead of the in-app WebView.

---

### 📋 Complete Payload Parameter Reference Table

| Key Name | Accepted Aliases | Type | Default | Description & Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **`token`** | - | `String` | `null` | **Target Specific Device:** Unique FCM token of 1 recipient. |
| **`topic`** | - | `String` | `"all"` | **Target Mass Audience:** Broadcasts to all users when `"all"` is used. |
| **`title`** | - | `String` | `"eFormX Notification"` | Header title rendered in bold on the notification card. |
| **`body`** | `message` | `String` | `""` | Primary description text displayed on the notification card. |
| **`speetch`** | `speech`, `speak_text`, `tts_text` | `String` | `null` | **Text-to-Speech:** Native voice reads this text aloud. Bypasses Silent mode via Alarm stream. |
| **`sound_type`** | `sound` | `String` | `"notification"` | `"call"` / `"ringtone"` = Call alert engine; `"notification"` = Standard tone; `"alarm"` = High urgency alarm. |
| **`target_url`** | `web_url`, `url`, `link` | `String` | `""` | Target URL launched when user taps the notification card. |
| **`open_type`** | - | `String` | `"app_webview"` | `"app_webview"` = Opens URL inside in-app WebView; `"browser"` = Opens URL in Chrome/external browser. |
| **`image_url`** | `image`, `banner` | `String` | `null` | Remote image URL for expandable Big Picture banner. |
| **`audio_url`** | `audio`, `mp3` | `String` | `null` | Remote MP3 URL streamed and played immediately upon arrival. |

---

### 🐘 Complete PHP Backend Integration Script (Firebase HTTP v1 API)

Save this file as `fcm_send.php` on your PHP server to dispatch any of the above notifications:

```php
<?php
/**
 * eFormX FCM Push Notification Dispatcher (Google Firebase HTTP v1 API)
 */

function getFirebaseAccessToken($serviceAccountPath) {
    $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
    
    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
    $now = time();
    $payload = json_encode([
        'iss' => $serviceAccount['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600
    ]);
    
    $base64UrlHeader = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($header));
    $base64UrlPayload = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($payload));
    
    $signature = '';
    openssl_sign($base64UrlHeader . "." . $base64UrlPayload, $signature, $serviceAccount['private_key'], OPENSSL_ALGO_SHA256);
    $base64UrlSignature = str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($signature));
    
    $jwt = $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ]));
    $response = json_decode(curl_exec($ch), true);
    curl_close($ch);
    
    return $response['access_token'];
}

function sendEformxNotification($serviceAccountPath, $projectId, $payload) {
    $accessToken = getFirebaseAccessToken($serviceAccountPath);
    
    // Prepare Data-Only Message Structure
    $dataPayload = [
        'title'      => (string)($payload['title'] ?? 'eFormX Alert'),
        'body'       => (string)($payload['body'] ?? $payload['message'] ?? ''),
        'sound_type' => (string)($payload['sound_type'] ?? 'notification'),
        'open_type'  => (string)($payload['open_type'] ?? 'app_webview')
    ];
    
    if (!empty($payload['speetch']))    $dataPayload['speetch'] = (string)$payload['speetch'];
    if (!empty($payload['speak_text'])) $dataPayload['speak_text'] = (string)$payload['speak_text'];
    if (!empty($payload['target_url'])) $dataPayload['target_url'] = (string)$payload['target_url'];
    if (!empty($payload['web_url']))    $dataPayload['web_url'] = (string)$payload['web_url'];
    if (!empty($payload['image_url']))  $dataPayload['image_url'] = (string)$payload['image_url'];
    if (!empty($payload['audio_url']))  $dataPayload['audio_url'] = (string)$payload['audio_url'];
    
    $message = [
        'data' => $dataPayload,
        'android' => [
            'priority' => 'HIGH'
        ]
    ];
    
    // Target Specific Token or Topic
    if (!empty($payload['token'])) {
        $message['token'] = $payload['token'];
    } else {
        $message['topic'] = $payload['topic'] ?? 'all';
    }
    
    $ch = curl_init("https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send");
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer {$accessToken}",
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode(['message' => $message]));
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    return ['http_code' => $httpCode, 'response' => json_decode($result, true)];
}

// -------------------------------------------------------------
// EXAMPLE 1: Send Call Alert with Voice Announcement to 1 User
// -------------------------------------------------------------
/*
$res = sendEformxNotification(
    __DIR__ . '/service-account.json',
    'your-firebase-project-id',
    [
        'token'      => 'cdyelUpGTES0l2sAQsVOIp:APA91bHn1RvPxLzj...',
        'title'      => '📞 एडमिन लाइव कॉल अलर्ट',
        'body'       => 'एडमिन आपसे तुरंत बात करना चाहते हैं।',
        'speetch'    => 'एडमिन आपसे तुरंत बात करना चाहते हैं। कृपया ऐप खोलें।',
        'sound_type' => 'call',
        'target_url' => 'https://eformx.com/call/room123',
        'open_type'  => 'app_webview'
    ]
);
print_r($res);
*/

// -------------------------------------------------------------
// EXAMPLE 2: Broadcast Update with Hindi Voice to All Users
// -------------------------------------------------------------
/*
$res = sendEformxNotification(
    __DIR__ . '/service-account.json',
    'your-firebase-project-id',
    [
        'topic'      => 'all',
        'title'      => '📢 ज़रूरी सूचना',
        'body'       => 'नया सरकारी फॉर्म ऑनलाइन शुरू हो चुका है।',
        'speetch'    => 'नमस्ते, नया सरकारी फॉर्म ऑनलाइन शुरू हो चुका है। कृपया ई-फॉर्म-एक्स ऐप देखें।',
        'sound_type' => 'notification',
        'target_url' => 'https://eformx.com/new-forms',
        'open_type'  => 'app_webview'
    ]
);
print_r($res);
*/
```

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

## 🛡️ Anti-Reverse-Engineering & ProGuard / R8 Hardening

To protect backend APIs, secret endpoints, device tokens, and business logic from reverse-engineering tools (such as JADX, Ghidra, Apktool, DEX string inspectors):

1. **R8 Full Mode Aggressive Optimization (`android.enableR8.fullMode=true`):**
   - Automatically inlines methods, merges classes, and eliminates dead code.
   - 5 optimization passes (`-optimizationpasses 5`) with access modifications allowed.
2. **Class & Package Repackaging (`-repackageclasses 'eformx.app.o'`):**
   - Collapses and flattens all internal app classes into a single obfuscated subpackage. Attackers cannot deduce class responsibilities from directory structures.
   - Aggressive member overloading (`-overloadaggressively`) replaces method and field names with single characters (`a`, `b`, `c`).
3. **Stripping Debugging Metadata & Bytecode Line Numbers:**
   - Strips `SourceFile`, `LineNumberTable`, `LocalVariableTable`, and `LocalVariableTypeTable` (`-renamesourcefileattribute ""`).
   - Decompilers fail to reconstruct original line numbers, local variable names, or source filenames.
4. **Log & Trace Stripping (`-assumenosideeffects`):**
   - All `android.util.Log` calls (`v`, `d`, `i`, `w`, `e`) and `System.out.println` statements are completely removed from release bytecode, preventing runtime inspection of API responses and auth tokens.
5. **Secure String & Endpoint Vault (`SecureConfig.java`):**
   - In standard ProGuard, string literals remain visible in plain text inside `classes.dex`.
   - eFormX incorporates a dynamic XOR cipher mask (`SecureConfig.java`). Endpoints such as `https://api.eformx.in/?api=install/app`, `https://api.eformx.in/?api=FCM/store`, SharedPreferences keys, and redirect URLs are stored exclusively as scrambled byte arrays.
   - Decompiling `classes.dex` reveals **zero plain-text API URLs or sensitive paths**.

---

## ⚙️ Configuration & Default Launch URL

The web portal URL is dynamically routed in `SplashActivity.java` and `MainActivity.java`:
- **Dynamic API Endpoint:** Managed securely via `SecureConfig.getInstallApiUrl()`
- **Dynamic Redirect URL:** Extracted from API (`data.redirect_url`) and cached securely in `SharedPreferences`
- **Default Fallback URL:** `https://eformx.com/` (used if offline or before initial API fix)
- **Deep Link Handling:** Verified App Links (`https://eformx.com`, `https://apply.eformx.com`) and custom scheme (`eformx://`) dynamically resolved with fallback support.

