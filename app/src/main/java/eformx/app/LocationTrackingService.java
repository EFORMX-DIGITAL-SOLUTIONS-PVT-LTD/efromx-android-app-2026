package eformx.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LocationTrackingService extends Service implements LocationListener {

    private static final String TAG = "LocationTrackingService";
    public static final String ACTION_START_TRACKING = "eformx.app.ACTION_START_TRACKING";
    public static final String ACTION_STOP_TRACKING = "eformx.app.ACTION_STOP_TRACKING";
    public static final String EXTRA_API_URL = "tracking_api_url";
    private static final String CHANNEL_ID = "eformx_tracking_channel";
    private static final int NOTIFICATION_ID = 2002;

    private static volatile boolean isRunning = false;
    private static volatile Location latestLocation = null;

    private LocationManager locationManager;
    private String trackingApiUrl = null;
    private String deviceId = "";

    public static boolean isTrackingActive() {
        return isRunning;
    }

    public static Location getLatestLocation() {
        return latestLocation;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null) deviceId = "";
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_TRACKING.equals(action)) {
                stopTrackingService();
                return START_NOT_STICKY;
            }

            if (ACTION_START_TRACKING.equals(action)) {
                trackingApiUrl = intent.getStringExtra(EXTRA_API_URL);
                startForegroundWithNotification();
                startGpsUpdates();
                return START_STICKY;
            }
        }

        // Default start
        startForegroundWithNotification();
        startGpsUpdates();
        return START_STICKY;
    }

    private void startForegroundWithNotification() {
        createNotificationChannel();

        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingOpenIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, LocationTrackingService.class);
        stopIntent.setAction(ACTION_STOP_TRACKING);
        PendingIntent pendingStopIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("eFormX Live Tracking")
                .setContentText("Real-time GPS location tracking is active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingOpenIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", pendingStopIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        isRunning = true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "eFormX Live Tracking Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active status when real-time GPS tracking is in progress");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startGpsUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted for background tracking");
            return;
        }

        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return;

            // Seed initial location if available
            Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastGps != null) {
                latestLocation = lastGps;
            } else if (lastNet != null) {
                latestLocation = lastNet;
            }

            // Register live updates: 5000ms (5 seconds) interval, 3 meters minimum distance
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 3f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 3f, this);
            }

            Log.i(TAG, "Native GPS Hardware Listener registered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        latestLocation = location;

        // If an API endpoint is configured, post the coordinates asynchronously
        if (trackingApiUrl != null && !trackingApiUrl.trim().isEmpty()) {
            postLocationToServer(location, trackingApiUrl);
        }
    }

    private void postLocationToServer(Location loc, String targetUrl) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("device_id", deviceId);
                payload.put("latitude", loc.getLatitude());
                payload.put("longitude", loc.getLongitude());
                payload.put("accuracy", loc.getAccuracy());
                payload.put("altitude", loc.getAltitude());
                payload.put("speed", loc.getSpeed());
                payload.put("timestamp", loc.getTime());

                byte[] postData = payload.toString().getBytes("UTF-8");
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Location posted to server. HTTP Code: " + responseCode);
            } catch (Exception e) {
                Log.w(TAG, "Failed to post location to server: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private void stopTrackingService() {
        isRunning = false;
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (Exception ignored) {}

        stopForeground(true);
        stopSelf();
        Log.i(TAG, "Live tracking stopped and hardware released");
    }

    @Override
    public void onDestroy() {
        stopTrackingService();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
