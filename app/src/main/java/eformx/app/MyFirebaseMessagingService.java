package eformx.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String CHANNEL_ID = "eformx_notification_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // WhatsApp jaisa Delivery Receipt: Message aate hi server ko confirm karta hai ki DATA ON hai
        sendDeliveryAck(this, remoteMessage);

        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wakeLock = pm.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "eformx:fcm_call_wakelock"
                );
                wakeLock.acquire(15000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring WakeLock: " + e.getMessage());
        }

        String title = "Notification";
        String messageBody = "You have a new message.";
        String imageUrl = null;

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                messageBody = remoteMessage.getNotification().getBody();
            }
            if (remoteMessage.getNotification().getImageUrl() != null) {
                imageUrl = remoteMessage.getNotification().getImageUrl().toString();
            }
        }

        String targetUrl = null;
        String openType = "app_webview";
        String soundType = "notification";
        String speakText = null;
        String callId = null;
        String callbackUrl = null;

        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            if (data.containsKey("title")) {
                title = data.get("title");
            }
            if (data.containsKey("message")) {
                messageBody = data.get("message");
            } else if (data.containsKey("body")) {
                messageBody = data.get("body");
            }

            if (data.containsKey("call_id")) {
                callId = data.get("call_id");
            } else if (data.containsKey("callId")) {
                callId = data.get("callId");
            } else if (data.containsKey("room_id")) {
                callId = data.get("room_id");
            }

            if (data.containsKey("callback_url")) {
                callbackUrl = data.get("callback_url");
            } else if (data.containsKey("callbackUrl")) {
                callbackUrl = data.get("callbackUrl");
            } else if (data.containsKey("decline_url")) {
                callbackUrl = data.get("decline_url");
            }

            if (data.containsKey("imageUrl")) {
                imageUrl = data.get("imageUrl");
            } else if (data.containsKey("image")) {
                imageUrl = data.get("image");
            } else if (data.containsKey("image_url")) {
                imageUrl = data.get("image_url");
            }

            if (data.containsKey("target_url")) {
                targetUrl = data.get("target_url");
            } else if (data.containsKey("url")) {
                targetUrl = data.get("url");
            } else if (data.containsKey("link")) {
                targetUrl = data.get("link");
            } else if (data.containsKey("web_url")) {
                targetUrl = data.get("web_url");
            }

            if (data.containsKey("open_type")) {
                openType = data.get("open_type");
            }

            if (data.containsKey("notification_type")) {
                soundType = data.get("notification_type");
            } else if (data.containsKey("sound_type")) {
                soundType = data.get("sound_type");
            } else if (data.containsKey("sound")) {
                soundType = data.get("sound");
            }

            if (data.containsKey("speetch")) {
                speakText = data.get("speetch");
            } else if (data.containsKey("speech")) {
                speakText = data.get("speech");
            } else if (data.containsKey("speak_text")) {
                speakText = data.get("speak_text");
            } else if (data.containsKey("tts_text")) {
                speakText = data.get("tts_text");
            } else if ("true".equalsIgnoreCase(data.get("tts")) || "true".equalsIgnoreCase(data.get("speak"))) {
                speakText = messageBody;
            }
        }

        // Agar server ne sirf check karne ke liye 'ping' bheja hai toh UI alert mat dikhao, delivery receipt already chali gayi hai!
        if ("ping".equalsIgnoreCase(soundType) || "silent_ping".equalsIgnoreCase(soundType) || "silent".equalsIgnoreCase(soundType)) {
            Log.d(TAG, "Silent ping received - Delivery receipt sent, skipping UI.");
            return;
        }

        boolean isCallAlert = "ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType);

        // Agar speetch / speech text maujood hai, toh ringtone band rahegi aur keval voice bolegi!
        if (isCallAlert && (speakText == null || speakText.trim().isEmpty())) {
            playCallRingtone(getApplicationContext(), null);
        }

        if (speakText != null && !speakText.trim().isEmpty()) {
            speakOutText(getApplicationContext(), speakText, isCallAlert);
        }

        sendNotification(title, messageBody, targetUrl, openType, imageUrl, soundType, speakText, callId, callbackUrl);
    }

    @Override
    public void onNewToken(@NonNull String token) {

        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        try {
            getSharedPreferences("eformx_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply();
        } catch (Exception ignored) {}
        registerFcmTokenOnServer(this, token);
        
    }

 

    public static void registerFcmTokenOnServer(Context context, String token) {
        if (context == null || token == null || token.trim().isEmpty()) return;
        new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                String deviceId = android.provider.Settings.Secure.getString(
                        context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                if (deviceId == null) deviceId = "";

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("device_id", deviceId);
                payload.put("fcm_token", token);

                byte[] postData = payload.toString().getBytes("UTF-8");
                java.net.URL url = new java.net.URL(SecureConfig.getFcmStoreApiUrl());
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "FCM token registration response code: " + responseCode);
                if (responseCode >= 200 && responseCode < 300) {
                    context.getSharedPreferences("eformx_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("last_registered_fcm_token", token)
                            .apply();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to register FCM token on server: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void sendNotification(String title, String messageBody, String targetUrl, String openType, String imageUrl, String soundType, String speakText, String callId, String callbackUrl) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (targetUrl != null && !targetUrl.isEmpty()) {
            intent.putExtra("target_url", targetUrl);
            intent.putExtra("open_type", openType);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String channelId = "eformx_voice_channel_v2";
        String category = NotificationCompat.CATEGORY_MESSAGE;
        Uri soundUri = null;

        boolean isCallNotification = "ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType);
        boolean isAlarmNotification = "alarm".equalsIgnoreCase(soundType);
        boolean isVoiceNotification = speakText != null && !speakText.trim().isEmpty();

        if (isCallNotification) {
            channelId = "eformx_call_voice_channel_v3";
            category = NotificationCompat.CATEGORY_CALL;
            soundUri = null; // Channel sound null taaki double ringtone na baje
        } else if (isAlarmNotification) {
            channelId = "eformx_alarm_channel_v2";
            category = NotificationCompat.CATEGORY_ALARM;
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        } else if (isVoiceNotification) {
            channelId = "eformx_voice_speech_channel_v4";
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            channelId = "eformx_standard_channel_v3";
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        Intent deleteIntent = new Intent(this, NotificationDismissReceiver.class);
        deleteIntent.setAction(NotificationDismissReceiver.ACTION_DISMISS);
        if (callId != null && !callId.isEmpty()) {
            deleteIntent.putExtra("call_id", callId);
        }
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            deleteIntent.putExtra("callback_url", callbackUrl);
        }
        deleteIntent.putExtra("status", "dismissed");
        deleteIntent.putExtra("action", "call_cut");
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                this, (int) System.currentTimeMillis() + 1, deleteIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Bitmap appLogoBitmap = getAppIconBitmap();

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification_small)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 500, 200, 500})
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(category)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setFullScreenIntent(pendingIntent, true)
                        .setContentIntent(pendingIntent)
                        .setDeleteIntent(deletePendingIntent);

        if (appLogoBitmap != null) {
            notificationBuilder.setLargeIcon(appLogoBitmap);
        }

        if (isCallNotification) {
            Intent declineIntent = new Intent(this, NotificationDismissReceiver.class);
            declineIntent.setAction(NotificationDismissReceiver.ACTION_DISMISS);
            if (callId != null && !callId.isEmpty()) {
                declineIntent.putExtra("call_id", callId);
            }
            if (callbackUrl != null && !callbackUrl.isEmpty()) {
                declineIntent.putExtra("callback_url", callbackUrl);
            }
            declineIntent.putExtra("status", "declined");
            declineIntent.putExtra("action", "call_cut");
            PendingIntent declinePendingIntent = PendingIntent.getBroadcast(
                    this, (int) System.currentTimeMillis() + 2, declineIntent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            Intent answerIntent = new Intent(this, MainActivity.class);
            answerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (targetUrl != null && !targetUrl.isEmpty()) {
                answerIntent.putExtra("target_url", targetUrl);
                answerIntent.putExtra("open_type", openType);
            }
            PendingIntent answerPendingIntent = PendingIntent.getActivity(
                    this, (int) System.currentTimeMillis() + 3, answerIntent,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            notificationBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent);
            notificationBuilder.addAction(android.R.drawable.ic_menu_call, "Answer", answerPendingIntent);
        }

        if (soundUri != null) {
            notificationBuilder.setSound(soundUri);
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl);
            if (bitmap != null) {
                notificationBuilder.setStyle(
                        new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon((Bitmap) null)
                );
                notificationBuilder.setLargeIcon(bitmap);
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "eFormX General Notifications";

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );

            if ("eformx_call_voice_channel_v3".equals(channelId)) {
                channelName = "eFormX Incoming Call Alert";
                channel.setName(channelName);
                channel.setSound(null, null);
            } else if ("eformx_alarm_channel_v2".equals(channelId)) {
                channelName = "eFormX Emergency Alarm Alerts";
                channel.setName(channelName);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            } else if ("eformx_voice_speech_channel_v4".equals(channelId)) {
                channelName = "eFormX Voice Speech Notifications";
                channel.setName(channelName);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            } else {
                channelName = "eFormX Standard Notifications";
                channel.setName(channelName);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            }

            channel.setDescription("High priority channel for eFormX Notifications");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (notificationManager != null) {
            Notification notification = notificationBuilder.build();
            if ("ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType) || "alarm".equalsIgnoreCase(soundType)) {
                notification.flags |= Notification.FLAG_INSISTENT;
            }
            notificationManager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    private static TextToSpeech textToSpeech;
    private static Ringtone activeCallRingtone;
    private static MediaPlayer callMediaPlayer;
    private static boolean isTtsLooping = false;
    private static String currentSpeakingText = "";
    private static android.content.BroadcastReceiver dynamicVolumeReceiver = null;

    private static synchronized void registerDynamicVolumeReceiver(Context context) {
        if (dynamicVolumeReceiver == null && context != null) {
            try {
                dynamicVolumeReceiver = new android.content.BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        Log.d(TAG, "Volume button pressed - Muting call sound and speech immediately");
                        muteSoundOnly(ctx);
                    }
                };
                android.content.IntentFilter filter = new android.content.IntentFilter();
                filter.addAction("android.media.VOLUME_CHANGED_ACTION");
                context.getApplicationContext().registerReceiver(dynamicVolumeReceiver, filter);
            } catch (Exception e) {
                Log.w(TAG, "Failed to register dynamic volume receiver: " + e.getMessage());
            }
        }
    }

    private static synchronized void unregisterDynamicVolumeReceiver(Context context) {
        if (dynamicVolumeReceiver != null && context != null) {
            try {
                context.getApplicationContext().unregisterReceiver(dynamicVolumeReceiver);
            } catch (Exception ignored) {}
            dynamicVolumeReceiver = null;
        }
    }

    public static void muteSoundOnly(Context context) {
        isTtsLooping = false;
        unregisterDynamicVolumeReceiver(context);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (activeCallRingtone != null) {
                    if (activeCallRingtone.isPlaying()) activeCallRingtone.stop();
                    activeCallRingtone = null;
                }
            } catch (Exception ignored) {}

            try {
                if (callMediaPlayer != null) {
                    if (callMediaPlayer.isPlaying()) callMediaPlayer.stop();
                    callMediaPlayer.release();
                    callMediaPlayer = null;
                }
            } catch (Exception ignored) {}

            try {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    textToSpeech.shutdown();
                    textToSpeech = null;
                }
            } catch (Exception ignored) {}
        });
    }

    public static void stopAllMediaAndTTS(Context context) {
        muteSoundOnly(context);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (context != null) {
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.cancelAll();
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static void playCallRingtone(Context context, Uri ringtoneUri) {
        if (context == null) return;

        android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode == android.media.AudioManager.RINGER_MODE_SILENT || ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE) {
                Log.d(TAG, "Phone is on silent or vibrate. Skipping audio ringtone playback.");
                return;
            }
        }

        registerDynamicVolumeReceiver(context);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (callMediaPlayer != null) {
                    try {
                        if (callMediaPlayer.isPlaying()) callMediaPlayer.stop();
                        callMediaPlayer.release();
                    } catch (Exception ignored) {}
                    callMediaPlayer = null;
                }

                if (activeCallRingtone != null) {
                    try {
                        if (activeCallRingtone.isPlaying()) activeCallRingtone.stop();
                    } catch (Exception ignored) {}
                    activeCallRingtone = null;
                }

                Uri uri = ringtoneUri;
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                }
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }

                activeCallRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), uri);
                if (activeCallRingtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        activeCallRingtone.setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                        .build()
                        );
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        activeCallRingtone.setLooping(true);
                    }
                    activeCallRingtone.play();
                } else {
                    callMediaPlayer = new MediaPlayer();
                    callMediaPlayer.setDataSource(context, uri);
                    callMediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .build()
                    );
                    callMediaPlayer.setLooping(true);
                    callMediaPlayer.prepare();
                    callMediaPlayer.start();
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    muteSoundOnly(context);
                }, 30000);

            } catch (Exception e) {
                Log.e(TAG, "Error playing continuous call ringtone: " + e.getMessage());
            }
        });
    }

    private void speakOutText(Context context, String textToSpeak, boolean loop) {
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) return;
        currentSpeakingText = textToSpeak;
        isTtsLooping = loop;
        registerDynamicVolumeReceiver(context);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (textToSpeech == null) {
                textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        configureNaturalTtsVoiceAndSpeak(context, currentSpeakingText);
                    }
                });
            } else {
                configureNaturalTtsVoiceAndSpeak(context, currentSpeakingText);
            }
        });
    }

    private void configureNaturalTtsVoiceAndSpeak(Context context, String textToSpeak) {
        try {
            int result = textToSpeech.setLanguage(new Locale("hi", "IN"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }

            textToSpeech.setPitch(0.95f);
            textToSpeech.setSpeechRate(0.92f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Set<Voice> voices = textToSpeech.getVoices();
                if (voices != null) {
                    for (Voice voice : voices) {
                        if (voice.getName() != null && voice.getName().toLowerCase().contains("hi-in") && !voice.isNetworkConnectionRequired()) {
                            textToSpeech.setVoice(voice);
                            break;
                        }
                    }
                }
            }

            textToSpeech.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    if (isTtsLooping) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isTtsLooping && textToSpeech != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    android.os.Bundle params = new android.os.Bundle();
                                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM);
                                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "FCM_TTS_LOOP_" + System.currentTimeMillis());
                                } else {
                                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        }, 1000);
                    }
                }

                @Override
                public void onError(String utteranceId) {}
            });

            // Silent mode bypass: Ensure ALARM stream has audible volume
            try {
                android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    int alarmVol = am.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
                    int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                    if (alarmVol <= 1 && maxVol > 0) {
                        am.setStreamVolume(android.media.AudioManager.STREAM_ALARM, (int)(maxVol * 0.85f), 0);
                    }
                }
            } catch (Exception ignored) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                );
                android.os.Bundle params = new android.os.Bundle();
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM);
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "FCM_TTS_LOOP_" + System.currentTimeMillis());
            } else {
                java.util.HashMap<String, String> params = new java.util.HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(android.media.AudioManager.STREAM_ALARM));
                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS Speak error: " + e.getMessage());
        }
    }

    private Bitmap getBitmapFromUrl(String urlString) {
        if (urlString == null || urlString.isEmpty()) return null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error downloading notification image: " + e.getMessage());
            return null;
        }
    }

    private Bitmap getAppIconBitmap() {
        Bitmap rawBitmap = null;
        try {
            rawBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        } catch (Exception ignored) {}

        if (rawBitmap == null) {
            try {
                rawBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.official_favicon);
            } catch (Exception ignored) {}
        }

        if (rawBitmap == null) {
            try {
                Drawable drawable = ContextCompat.getDrawable(this, R.mipmap.ic_launcher);
                if (drawable != null) {
                    int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 128;
                    int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 128;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    rawBitmap = bitmap;
                }
            } catch (Exception ignored) {}
        }

        return getCircularBitmap(rawBitmap);
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int size = Math.min(width, height);

            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);

            Rect srcRect = new Rect((width - size) / 2, (height - size) / 2, (width + size) / 2, (height + size) / 2);
            Rect destRect = new Rect(0, 0, size, size);

            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, srcRect, destRect, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static String getDeviceIpAddress() {
        try {
            java.util.List<java.net.NetworkInterface> interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
            for (java.net.NetworkInterface intf : interfaces) {
                java.util.List<java.net.InetAddress> addrs = java.util.Collections.list(intf.getInetAddresses());
                for (java.net.InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    public static String getNetworkTypeName(Context context) {
        if (context == null) return "OFFLINE";
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.net.Network activeNetwork = cm.getActiveNetwork();
                    if (activeNetwork != null) {
                        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                        if (caps != null) {
                            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return "WIFI";
                            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) return "MOBILE_DATA";
                            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return "ETHERNET";
                        }
                    }
                } else {
                    android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                    if (info != null && info.isConnected()) {
                        if (info.getType() == android.net.ConnectivityManager.TYPE_WIFI) return "WIFI";
                        if (info.getType() == android.net.ConnectivityManager.TYPE_MOBILE) return "MOBILE_DATA";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "ONLINE";
    }

    public static void sendNetworkPresencePing(Context context, String eventType) {
        if (context == null) return;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String deviceId = android.provider.Settings.Secure.getString(
                        context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                if (deviceId == null) deviceId = "";

                String token = context.getSharedPreferences("eformx_prefs", Context.MODE_PRIVATE)
                        .getString("fcm_token", "");

                String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
                String ipAddress = getDeviceIpAddress();
                String networkType = getNetworkTypeName(context);

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("device_id", deviceId);
                payload.put("fcm_token", token);
                payload.put("device_name", deviceName);
                payload.put("ip_address", ipAddress);
                payload.put("network_type", networkType);
                payload.put("status", "ONLINE");
                payload.put("event", eventType != null ? eventType : "network_connected");
                payload.put("timestamp", System.currentTimeMillis());

                byte[] postData = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                URL url = new URL("https://api.eformx.in/?api=FCM/delivery");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Network presence ping [" + eventType + "] sent. Response: " + responseCode);
            } catch (Exception e) {
                Log.w(TAG, "Failed to send network presence ping: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void sendDeliveryAck(Context context, RemoteMessage remoteMessage) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String deviceId = "";
                if (context != null) {
                    deviceId = android.provider.Settings.Secure.getString(
                            context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                }
                if (deviceId == null) deviceId = "";

                String token = context != null ? context.getSharedPreferences("eformx_prefs", Context.MODE_PRIVATE)
                        .getString("fcm_token", "") : "";

                String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
                String ipAddress = getDeviceIpAddress();
                String networkType = getNetworkTypeName(context);

                String messageId = remoteMessage != null ? remoteMessage.getMessageId() : "";
                Map<String, String> data = remoteMessage != null ? remoteMessage.getData() : null;
                String callId = (data != null && data.containsKey("call_id")) ? data.get("call_id") : "";

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("device_id", deviceId);
                payload.put("fcm_token", token);
                payload.put("device_name", deviceName);
                payload.put("ip_address", ipAddress);
                payload.put("network_type", networkType);
                payload.put("status", "delivered");
                payload.put("network", "DATA_ON");
                payload.put("message_id", messageId);
                if (!callId.isEmpty()) {
                    payload.put("call_id", callId);
                }
                payload.put("timestamp", System.currentTimeMillis());

                byte[] postData = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                URL url = new URL("https://api.eformx.in/?api=FCM/delivery");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Delivery acknowledgement (Data ON / Double Tick) sent. Response: " + responseCode);
            } catch (Exception e) {
                Log.w(TAG, "Failed to send delivery acknowledgement: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
