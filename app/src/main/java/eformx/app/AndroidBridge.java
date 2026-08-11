package eformx.app;

import android.app.Activity;
import android.os.Build;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;

import androidx.activity.result.ActivityResultLauncher;

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

            json.put("language",
                    Locale.getDefault().getLanguage());

            json.put("country",
                    Locale.getDefault().getCountry());

            json.put("timezone",
                    TimeZone.getDefault().getID());

            json.put("screen_width",
                    activity.getResources()
                            .getDisplayMetrics().widthPixels);

            json.put("screen_height",
                    activity.getResources()
                            .getDisplayMetrics().heightPixels);

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

    public void cleanup() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
