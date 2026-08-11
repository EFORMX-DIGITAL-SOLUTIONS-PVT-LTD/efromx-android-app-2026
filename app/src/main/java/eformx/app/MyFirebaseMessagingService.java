package eformx.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        String audioUrl = null;

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

            if (data.containsKey("imageUrl")) {
                imageUrl = data.get("imageUrl");
            } else if (data.containsKey("image")) {
                imageUrl = data.get("image");
            } else if (data.containsKey("image_url")) {
                imageUrl = data.get("image_url");
            }

            if (data.containsKey("audio_url")) {
                audioUrl = data.get("audio_url");
            } else if (data.containsKey("audio")) {
                audioUrl = data.get("audio");
            } else if (data.containsKey("mp3_url")) {
                audioUrl = data.get("mp3_url");
            }

            if (data.containsKey("target_url")) {
                targetUrl = data.get("target_url");
            } else if (data.containsKey("url")) {
                targetUrl = data.get("url");
            } else if (data.containsKey("link")) {
                targetUrl = data.get("link");
            }

            if (data.containsKey("open_type")) {
                openType = data.get("open_type");
            }

            if (data.containsKey("sound_type")) {
                soundType = data.get("sound_type");
            } else if (data.containsKey("sound")) {
                soundType = data.get("sound");
            }

            if (data.containsKey("speak_text")) {
                speakText = data.get("speak_text");
            } else if (data.containsKey("tts_text")) {
                speakText = data.get("tts_text");
            } else if ("true".equalsIgnoreCase(data.get("tts")) || "true".equalsIgnoreCase(data.get("speak"))) {
                speakText = messageBody;
            }
        }

        boolean isCallAlert = "ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType);

        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            playAudioUrl(getApplicationContext(), audioUrl);
        } else {
            if (speakText != null && !speakText.trim().isEmpty()) {
                speakOutText(getApplicationContext(), speakText, isCallAlert);
            }
        }

        sendNotification(title, messageBody, targetUrl, openType, imageUrl, soundType, speakText, audioUrl);
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
    }

    private void sendNotification(String title, String messageBody, String targetUrl, String openType, String imageUrl, String soundType, String speakText, String audioUrl) {
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
        boolean isAudioNotification = audioUrl != null && !audioUrl.trim().isEmpty();
        boolean isVoiceNotification = speakText != null && !speakText.trim().isEmpty();

        if (isCallNotification) {
            channelId = "eformx_call_voice_channel_v1";
            category = NotificationCompat.CATEGORY_CALL;
            soundUri = null;
        } else if (isAlarmNotification) {
            channelId = "eformx_alarm_voice_channel_v1";
            category = NotificationCompat.CATEGORY_ALARM;
            soundUri = null;
        } else if (isAudioNotification || isVoiceNotification) {
            channelId = "eformx_voice_speech_channel_v3";
            soundUri = null;
        } else {
            channelId = "eformx_standard_channel_v3";
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        Intent deleteIntent = new Intent(this, NotificationDismissReceiver.class);
        deleteIntent.setAction(NotificationDismissReceiver.ACTION_DISMISS);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                this, (int) System.currentTimeMillis() + 1, deleteIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Bitmap appLogoBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(appLogoBitmap)
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

            if ("eformx_call_voice_channel_v1".equals(channelId)) {
                channelName = "eFormX Incoming Call Voice";
                channel.setName(channelName);
                channel.setSound(null, null);
            } else if ("eformx_alarm_voice_channel_v1".equals(channelId)) {
                channelName = "eFormX Alarm Voice Notifications";
                channel.setName(channelName);
                channel.setSound(null, null);
            } else if ("eformx_voice_speech_channel_v3".equals(channelId)) {
                channelName = "eFormX Voice Speech Notifications";
                channel.setName(channelName);
                channel.setSound(null, null);
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
            if ("ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType)) {
                notification.flags |= Notification.FLAG_INSISTENT;
            }
            notificationManager.notify((int) System.currentTimeMillis(), notification);
        }
    }

    private static TextToSpeech textToSpeech;
    private static MediaPlayer mediaPlayer;
    private static MediaPlayer callMediaPlayer;
    private static boolean isTtsLooping = false;
    private static String currentSpeakingText = "";

    public static void stopAllMediaAndTTS(Context context) {
        isTtsLooping = false;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (callMediaPlayer != null) {
                    if (callMediaPlayer.isPlaying()) callMediaPlayer.stop();
                    callMediaPlayer.release();
                    callMediaPlayer = null;
                }
            } catch (Exception ignored) {}

            try {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            } catch (Exception ignored) {}

            try {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    textToSpeech.shutdown();
                    textToSpeech = null;
                }
            } catch (Exception ignored) {}

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
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (callMediaPlayer != null) {
                    try {
                        if (callMediaPlayer.isPlaying()) callMediaPlayer.stop();
                        callMediaPlayer.release();
                    } catch (Exception ignored) {}
                    callMediaPlayer = null;
                }

                Uri uri = ringtoneUri;
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
                if (uri == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                }

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

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (callMediaPlayer != null) {
                            if (callMediaPlayer.isPlaying()) callMediaPlayer.stop();
                            callMediaPlayer.release();
                            callMediaPlayer = null;
                        }
                    } catch (Exception ignored) {}
                }, 30000);

            } catch (Exception e) {
                Log.e(TAG, "Error playing continuous call ringtone: " + e.getMessage());
            }
        });
    }

    private void playAudioUrl(Context context, String audioUrl) {
        if (audioUrl == null || audioUrl.trim().isEmpty()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                        mediaPlayer.release();
                    } catch (Exception ignored) {}
                }
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .build()
                );
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.setOnPreparedListener(mp -> {
                    try {
                        mp.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting custom audio: " + e.getMessage());
                    }
                });
                mediaPlayer.setOnCompletionListener(mp -> {
                    try { mp.release(); mediaPlayer = null; } catch (Exception ignored) {}
                });
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e(TAG, "Error playing custom audio URL: " + e.getMessage());
            }
        });
    }

    private void speakOutText(Context context, String textToSpeak, boolean loop) {
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) return;
        currentSpeakingText = textToSpeak;
        isTtsLooping = loop;
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
                                textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "FCM_TTS_LOOP_" + System.currentTimeMillis());
                            }
                        }, 1000);
                    }
                }

                @Override
                public void onError(String utteranceId) {}
            });

            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "FCM_TTS_LOOP_" + System.currentTimeMillis());
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
}
