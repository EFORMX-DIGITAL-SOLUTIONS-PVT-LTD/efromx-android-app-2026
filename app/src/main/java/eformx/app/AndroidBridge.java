package eformx.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.Locale;
import java.util.TimeZone;

public class AndroidBridge {

    private final Activity activity;
    private final ActivityResultLauncher<String[]> locationPermissionLauncher;
    private TextToSpeech textToSpeech;

    public AndroidBridge(Activity activity, ActivityResultLauncher<String[]> locationPermissionLauncher) {
        this.activity = activity;
        this.locationPermissionLauncher = locationPermissionLauncher;
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
    public String getAppVersion() {
        if (activity == null) return "";
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0";
        } catch (Exception e) {
            return "1.0";
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
    public String getLocation() {
        if (activity == null) return "{\"error\":true,\"message\":\"Activity null\"}";
        try {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return "{\"error\":true,\"message\":\"Permission not granted\"}";
            }

            LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return "{\"error\":true,\"message\":\"LocationManager null\"}";

            Location gpsLoc = null;
            Location netLoc = null;

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            Location bestLoc = gpsLoc;
            if (bestLoc == null || (netLoc != null && netLoc.getTime() > bestLoc.getTime())) {
                bestLoc = netLoc;
            }

            if (bestLoc != null) {
                JSONObject json = new JSONObject();
                json.put("latitude", bestLoc.getLatitude());
                json.put("longitude", bestLoc.getLongitude());
                json.put("accuracy", bestLoc.getAccuracy());
                json.put("altitude", bestLoc.getAltitude());
                json.put("speed", bestLoc.getSpeed());
                json.put("time", bestLoc.getTime());
                json.put("error", false);
                return json.toString();
            } else {
                return "{\"error\":true,\"message\":\"No location fix available\"}";
            }

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
