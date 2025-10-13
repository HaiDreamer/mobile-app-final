package vn.edu.usth.ircui.feature_chat.data;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import vn.edu.usth.ircui.ChatActivity;
import vn.edu.usth.ircui.R;

public class MessageNotification {
    private static final String CHANNEL_ID = "chat_message_channel";
    private static volatile boolean channelCreated = false;

    /** Returns true if the app can legally post notifications right now. */
    public static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // Older versions don’t have the runtime permission; respect user/block setting:
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** Safe wrapper: only posts if allowed; never throws. */
    public static void showMsgNotification(Context context, String sender, String message) {
        if (!canPostNotifications(context)) {
            // Optionally log or surface a toast from the calling Activity/Fragment instead.
            return;
        }

        // Create channel once (API 26+)
        if (!channelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Chat Message", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
            channelCreated = true;
        }

        // Tap opens ChatActivity
        Intent intent = new Intent(context, ChatActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                // FLAG_IMMUTABLE is required for targetSdk 31+ if you don't mutate extras.
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New message from " + sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context)
                    .notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException se) {
            // If permissions were revoked between check & post, swallow safely or log.
        }
    }
}
