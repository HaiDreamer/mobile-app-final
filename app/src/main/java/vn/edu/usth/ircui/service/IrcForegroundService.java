package vn.edu.usth.ircui.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import vn.edu.usth.ircui.R;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.network.NetworkMonitor;

public class IrcForegroundService extends Service {

    public static final String EXTRA_NICK = "nick";
    public static final String EXTRA_CHANNEL = "channel";
    public static final String EXTRA_SASL_USER = "sasl_user";
    public static final String EXTRA_SASL_PASS = "sasl_pass";
    public static final String EXTRA_SASL_EXTERNAL = "sasl_external"; // boolean
    public vn.edu.usth.ircui.network.NetworkMonitor monitor;
    private String lastNick, lastChannel, lastUser, lastPass; private boolean lastExt;

    private static final String CHANNEL_ID = "irc_fg";
    private static final int NOTIF_ID = 42;

    private final IrcClientManager irc = new IrcClientManager();

    @Override public void onCreate() {
        super.onCreate();
        createNotifChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String nick    = intent.getStringExtra(EXTRA_NICK);
        String channel = intent.getStringExtra(EXTRA_CHANNEL);
        String user    = intent.getStringExtra(EXTRA_SASL_USER);
        String pass    = intent.getStringExtra(EXTRA_SASL_PASS);
        boolean useExternal = intent.getBooleanExtra(EXTRA_SASL_EXTERNAL, false);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_irc)
                .setContentTitle("IRC connected")
                .setContentText("Nick " + nick + " · " + channel)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, n);

        irc.setCallback(new IrcClientManager.MessageCallback() {
            @Override public void onMessage(String u, String t, long ts, boolean mine) { /* no-op here */ }
            @Override public void onSystem(String t) { /* could update notification */ }
        });

        // connect with optional SASL (we’ll implement in section 2)
        irc.connectWithSasl(nick, channel, user, pass, useExternal);
        lastNick = nick; lastChannel = channel; lastUser = user; lastPass = pass; lastExt = useExternal;

        monitor = new vn.edu.usth.ircui.network.NetworkMonitor(getApplicationContext(), new vn.edu.usth.ircui.network.NetworkMonitor.Callback() {
            @Override public void onUp() {
                // if not connected, try again
                irc.disconnect();
                irc.connectWithSasl(lastNick, lastChannel, lastUser, lastPass, lastExt);
            }
            @Override public void onDown() {
                // optional: show “offline” in the notification
            }
        });
        monitor.start();

        return START_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        irc.disconnect();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "IRC Connection", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }
}
