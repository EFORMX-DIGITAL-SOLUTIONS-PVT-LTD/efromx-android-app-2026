package eformx.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class VolumeButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if ("android.media.VOLUME_CHANGED_ACTION".equals(action) || Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                Log.d("VolumeReceiver", "Volume key pressed. Muting notification speech & audio.");
                MyFirebaseMessagingService.stopAllMediaAndTTS(context);
            }
        }
    }
}
