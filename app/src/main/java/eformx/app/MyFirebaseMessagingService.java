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
import android.os.Handler;
import android.os.Looper;
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

        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            playAudioUrl(getApplicationContext(), audioUrl);
        } else if (speakText != null && !speakText.trim().isEmpty()) {
            speakOutText(getApplicationContext(), speakText);
        }

        sendNotification(title, messageBody, targetUrl, openType, imageUrl, soundType, speakText);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
    }

    private void sendNotification(String title, String messageBody, String targetUrl, String openType, String imageUrl, String soundType, String speakText) {
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

        String channelId = "eformx_notification_channel_v3";
        String category = NotificationCompat.CATEGORY_MESSAGE;
        Uri soundUri = null;

        boolean isVoiceNotification = (speakText != null && !speakText.trim().isEmpty()) || "none".equalsIgnoreCase(soundType) || "voice".equalsIgnoreCase(soundType) || "silent".equalsIgnoreCase(soundType);

        if (isVoiceNotification) {
            channelId = "eformx_silent_channel_v1";
            soundUri = null;
        } else if ("ringtone".equalsIgnoreCase(soundType) || "call".equalsIgnoreCase(soundType)) {
            channelId = "eformx_call_channel_v3";
            category = NotificationCompat.CATEGORY_CALL;
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        } else if ("alarm".equalsIgnoreCase(soundType)) {
            channelId = "eformx_alarm_channel_v3";
            category = NotificationCompat.CATEGORY_ALARM;
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri == null) {
                soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            }
        }

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
                        .setContentIntent(pendingIntent);

        if (soundUri != null) {
            notificationBuilder.setSound(soundUri);
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
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
            int usage = AudioAttributes.USAGE_NOTIFICATION;

            if ("eformx_call_channel_v3".equals(channelId)) {
                channelName = "eFormX Incoming Call Ringtone";
                usage = AudioAttributes.USAGE_NOTIFICATION_RINGTONE;
            } else if ("eformx_alarm_channel_v3".equals(channelId)) {
                channelName = "eFormX Alarm Notifications";
                usage = AudioAttributes.USAGE_ALARM;
            }

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High priority channel for eFormX Notifications");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            if ("eformx_silent_channel_v1".equals(channelId)) {
                channelName = "eFormX Voice Speech Notifications";
                channel.setSound(null, null);
            } else {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(usage)
                        .build();
                channel.setSound(soundUri, audioAttributes);
            }

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
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                mediaPlayer.setOnCompletionListener(mp -> {
                    try { mp.release(); } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                Log.e(TAG, "Error playing custom audio URL: " + e.getMessage());
            }
        });
    }

    private void speakOutText(Context context, String textToSpeak) {
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (textToSpeech == null) {
                textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        configureNaturalTtsVoiceAndSpeak(textToSpeak);
                    }
                });
            } else {
                configureNaturalTtsVoiceAndSpeak(textToSpeak);
            }
        });
    }

    private void configureNaturalTtsVoiceAndSpeak(String textToSpeak) {
        try {
            int result = textToSpeech.setLanguage(new Locale("hi", "IN"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }

            textToSpeech.setPitch(0.95f);      // Soft Natural Pitch
            textToSpeech.setSpeechRate(0.92f); // Realistic Pace

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

            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "FCM_TTS_" + System.currentTimeMillis());
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
