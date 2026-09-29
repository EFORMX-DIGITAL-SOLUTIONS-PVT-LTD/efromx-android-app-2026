package eformx.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AndroidBridge {

    private static final String TAG = "AndroidBridge";
    private final Activity activity;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher;
    private TextToSpeech textToSpeech;
    private Location latestBridgeLocation = null;

    public AndroidBridge(Activity activity, ActivityResultLauncher<String[]> locationPermissionLauncher) {
        this.activity = activity;
        this.locationPermissionLauncher = locationPermissionLauncher;
        initLocationListener();
    }

    public void initLocationListener() {
        if (activity == null) return;
        try {
            if (hasLocationPermission()) {
                LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    LocationListener listener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            if (loc != null) latestBridgeLocation = loc;
                        }
                        @Override public void onStatusChanged(String s, int i, Bundle b) {}
                        @Override public void onProviderEnabled(String s) {}
                        @Override public void onProviderDisabled(String s) {}
                    };
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper());
                    }
                    if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper());
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void speak(String text) {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (text == null || text.trim().isEmpty()) return;
            if (textToSpeech == null) {
                textToSpeech = new TextToSpeech(activity.getApplicationContext(), status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech.setLanguage(new Locale("hi", "IN"));
                        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
                    }
                });
            } else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
            }
        });
    }

    @JavascriptInterface
    public void openBrowser(String url) {
        if (activity == null || url == null || url.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            try {
                String targetUrl = url.trim();
                if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                    targetUrl = "https://" + targetUrl;
                }
                Uri uri = Uri.parse(targetUrl);
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.launchUrl(activity, uri);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(activity, "Browser open karne me error aaya", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @JavascriptInterface
    public void openApp(String url) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).openAppUrl(url);
        }
    }

    @JavascriptInterface
    public void openApp() {
        openApp("");
    }

    @JavascriptInterface
    public void SafeScreen(boolean enable) {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            try {
                if (enable) {
                    activity.getWindow().setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                    );
                } else {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            } catch (Exception e) {
                Log.e(TAG, "SafeScreen error: " + e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void vibrate(long milliseconds) {
        if (activity == null) return;
        try {
            long duration = (milliseconds <= 0) ? 50 : Math.min(milliseconds, 5000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vm.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } else {
                Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(duration);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void vibrate() {
        vibrate(50);
    }

    @JavascriptInterface
    public void copyToClipboard(String text) {
        if (activity == null || text == null) return;
        activity.runOnUiThread(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("eFormX Data", text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
            } catch (Exception ignored) {}
        });
    }

    @JavascriptInterface
    public String getFromClipboard() {
        if (activity == null) return "";
        try {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    return text != null ? text.toString() : "";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    @JavascriptInterface
    public void showToast(String message) {
        if (activity == null || message == null || message.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            try {
                Toast.makeText(activity, message.trim(), Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        });
    }

    @JavascriptInterface
    public void saveBase64File(String base64Data, String mimeType, String fileName) {
        if (activity == null || base64Data == null || base64Data.trim().isEmpty()) return;
        activity.runOnUiThread(() -> {
            try {
                String cleanBase64 = base64Data;
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }
                byte[] fileBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);

                String name = (fileName != null && !fileName.trim().isEmpty()) ? fileName.trim() : ("eFormX_Download_" + System.currentTimeMillis() + ".pdf");
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File file = new File(downloadsDir, name);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(fileBytes);
                fos.flush();
                fos.close();

                android.media.MediaScannerConnection.scanFile(
                        activity,
                        new String[]{file.getAbsolutePath()},
                        new String[]{mimeType != null ? mimeType : "application/pdf"},
                        null
                );

                Toast.makeText(activity, "File Saved in Downloads: " + name, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "saveBase64File error: " + e.getMessage());
                Toast.makeText(activity, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public void checkForAppUpdate() {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).checkGooglePlayAppUpdate();
        }
    }

    @JavascriptInterface
    public void showProcessLoader(String page) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showProcessLoader(page);
        }
    }

    @JavascriptInterface
    public void openLocationPermission() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (locationPermissionLauncher != null) {
                locationPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
    }

    @JavascriptInterface
    public void openLocationSettings() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            try {
                activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception ignored) {}
        });
    }

    @JavascriptInterface
    public boolean hasLocationPermission() {
        if (activity == null) return false;
        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public boolean hasNotificationPermission() {
        if (activity == null) return true;
        return NotificationManagerCompat.from(activity).areNotificationsEnabled();
    }

    @JavascriptInterface
    public void openNotificationPermission() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).requestNotificationPermissionExplicit();
            }
        });
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        openNotificationPermission();
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openNotificationSettings();
            }
        });
    }

    @JavascriptInterface
    public void requestAllPermissions() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).checkAndRequestAllPermissions();
            }
        });
    }

    @JavascriptInterface
    public void exitApp() {
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).showExitConfirmationDialog();
            }
        });
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        if (activity == null) return "{\"error\":true}";
        try {
            JSONObject json = new JSONObject();

            json.put("android_id",
                    Settings.Secure.getString(
                            activity.getContentResolver(),
                            Settings.Secure.ANDROID_ID
                    ));

            json.put("manufacturer", Build.MANUFACTURER);
            json.put("brand", Build.BRAND);
            json.put("model", Build.MODEL);
            json.put("device", Build.DEVICE);
            json.put("product", Build.PRODUCT);

            json.put("android_version", Build.VERSION.RELEASE);
            json.put("sdk_version", Build.VERSION.SDK_INT);

            json.put("language", Locale.getDefault().getLanguage());
            json.put("country", Locale.getDefault().getCountry());
            json.put("timezone", TimeZone.getDefault().getID());

            json.put("screen_width",
                    activity.getResources().getDisplayMetrics().widthPixels);

            json.put("screen_height",
                    activity.getResources().getDisplayMetrics().heightPixels);

            json.put("app_version", getAppVersion());
            json.put("package_name", getPackageName());
            json.put("is_network_available", isNetworkAvailable());
            json.put("ip_address", getIpAddress());
            json.put("network_type", getNetworkType());
            json.put("network_operator", getNetworkOperator());
            json.put("fcm_token", getFcmToken());

            return json.toString();

        } catch (Exception e) {
            return "{\"error\":true}";
        }
    }

    @JavascriptInterface
    public String getDeviceId() {
        if (activity == null) return "";
        try {
            String id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id != null ? id : "";
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public String getFcmToken() {
        if (activity == null) return "";
        try {
            SharedPreferences prefs = activity.getSharedPreferences("eformx_prefs", Context.MODE_PRIVATE);
            String savedToken = prefs.getString("fcm_token", "");
            if (savedToken != null && !savedToken.isEmpty()) {
                return savedToken;
            }

            Task<String> task = FirebaseMessaging.getInstance().getToken();
            String token = Tasks.await(task, 3, TimeUnit.SECONDS);
            if (token != null && !token.isEmpty()) {
                prefs.edit().putString("fcm_token", token).apply();
                return token;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching FCM token: " + e.getMessage());
        }
        return "";
    }

    @JavascriptInterface
    public String getAppVersion() {
        if (activity == null) return "";
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.9";
        } catch (Exception e) {
            return "1.9";
        }
    }

    @JavascriptInterface
    public String getPackageName() {
        if (activity == null) return "eformx.app";
        return activity.getPackageName();
    }

    @JavascriptInterface
    public boolean isNetworkAvailable() {
        if (activity == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception ignored) {}
        return false;
    }

    @JavascriptInterface
    public String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // 1. Priority 1: Search for IPv6 address
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') >= 0) {
                            int delim = sAddr.indexOf('%');
                            return delim < 0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
            // 2. Fallback: If IPv6 is not available, return IPv4
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "::1";
    }

    @JavascriptInterface
    public String getNetworkType() {
        if (activity == null) return "OFFLINE";
        try {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        return "WIFI";
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        return "CELLULAR_MOBILE";
                    } else {
                        return "CONNECTED";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "OFFLINE";
    }

    @JavascriptInterface
    public String getNetworkOperator() {
        if (activity == null) return "";
        try {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String op = tm.getNetworkOperatorName();
                return op != null ? op : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    @JavascriptInterface
    private String buildLocationJson(Location loc) {
        try {
            JSONObject json = new JSONObject();
            json.put("latitude", loc.getLatitude());
            json.put("longitude", loc.getLongitude());
            json.put("accuracy", loc.getAccuracy());
            json.put("altitude", loc.getAltitude());
            json.put("speed", loc.getSpeed());
            json.put("time", loc.getTime());
            json.put("error", false);
            return json.toString();
        } catch (Exception e) {
            return "{\"error\":true,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    @JavascriptInterface
    public String getLocation() {
        if (activity == null) return "{\"error\":true,\"message\":\"Activity null\"}";
        try {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                openLocationPermission();
                return "{\"error\":true,\"message\":\"Location permission required. Please grant permission.\"}";
            }

            LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return "{\"error\":true,\"message\":\"LocationManager null\"}";

            boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gpsEnabled && !netEnabled) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "कृपया फोन की लोकेशन (GPS) ऑन करें", Toast.LENGTH_LONG).show();
                    openLocationSettings();
                });
                return "{\"error\":true,\"message\":\"Device GPS is turned OFF in phone settings\"}";
            }

            // 1. Fix from active bridge listener
            if (latestBridgeLocation != null) {
                return buildLocationJson(latestBridgeLocation);
            }

            // 3. Cached last known location
            Location gpsLoc = null;
            Location netLoc = null;
            if (gpsEnabled) {
                gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (netEnabled) {
                netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            Location bestLoc = gpsLoc;
            if (bestLoc == null || (netLoc != null && netLoc.getTime() > bestLoc.getTime())) {
                bestLoc = netLoc;
            }

            if (bestLoc != null) {
                latestBridgeLocation = bestLoc;
                return buildLocationJson(bestLoc);
            }

            // 4. Trigger active single / continuous fix on main thread
            activity.runOnUiThread(() -> {
                try {
                    LocationListener quickListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location loc) {
                            if (loc != null) latestBridgeLocation = loc;
                        }
                        @Override public void onStatusChanged(String s, int i, Bundle b) {}
                        @Override public void onProviderEnabled(String s) {}
                        @Override public void onProviderDisabled(String s) {}
                    };
                    if (gpsEnabled) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, quickListener, Looper.getMainLooper());
                    }
                    if (netEnabled) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, quickListener, Looper.getMainLooper());
                    }
                } catch (Exception ignored) {}
            });

            return "{\"error\":true,\"message\":\"Acquiring satellite fix... Please tap again in 2 seconds.\"}";

        } catch (Exception e) {
            return "{\"error\":true,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    public void cleanup() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
