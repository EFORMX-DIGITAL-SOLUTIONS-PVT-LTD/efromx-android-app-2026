package eformx.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationDismissReceiver extends BroadcastReceiver {

    public static final String ACTION_DISMISS = "eformx.app.NOTIFICATION_DISMISSED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_DISMISS.equals(intent.getAction())) {
            Log.d("DismissReceiver", "Notification swiped away/dismissed. Stopping audio and TTS.");
            MyFirebaseMessagingService.stopAllMediaAndTTS(context);
        }
    }
}
