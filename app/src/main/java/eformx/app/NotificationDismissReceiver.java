package eformx.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationDismissReceiver extends BroadcastReceiver {

    public static final String ACTION_DISMISS = "eformx.app.NOTIFICATION_DISMISSED";
    private static final String TAG = "DismissReceiver";
    private static final String CALL_STATUS_API = "https://api.eformx.in/?api=FCM/call-status";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_DISMISS.equals(intent.getAction())) {
            Log.d(TAG, "Notification dismissed/cut. Stopping sound and speech.");
            MyFirebaseMessagingService.stopAllMediaAndTTS(context);

            int notifId = intent.getIntExtra("notification_id", -1);
            if (notifId != -1 && context != null) {
                try {
                    android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.cancel(notifId);
                    }
                } catch (Exception ignored) {}
            }

            try {
                if (context != null) {
                    android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        vibrator.cancel();
                    }
                }
            } catch (Exception ignored) {}

            String callId = intent.getStringExtra("call_id");
            String status = intent.getStringExtra("status");
            String callbackUrl = intent.getStringExtra("callback_url");

            if (status == null || status.isEmpty()) {
                status = "declined";
            }

            if (callId != null && !callId.trim().isEmpty()) {
                sendCallStatusCallback(context, callId, status, callbackUrl);
            }
        }
    }

    private void sendCallStatusCallback(Context context, String callId, String status, String customCallbackUrl) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String deviceId = "";
                if (context != null) {
                    deviceId = Settings.Secure.getString(
                            context.getContentResolver(), Settings.Secure.ANDROID_ID);
                }
                if (deviceId == null) deviceId = "";

                JSONObject payload = new JSONObject();
                payload.put("call_id", callId);
                payload.put("status", status);
                payload.put("action", "call_cut");
                payload.put("device_id", deviceId);

                byte[] postData = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                String targetUrlStr = (customCallbackUrl != null && !customCallbackUrl.trim().isEmpty())
                        ? customCallbackUrl.trim()
                        : CALL_STATUS_API;

                URL url = new URL(targetUrlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Call status callback sent to [" + targetUrlStr + "]. Response code: " + responseCode + " for call_id: " + callId);
            } catch (Exception e) {
                Log.w(TAG, "Failed to send call status callback: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
