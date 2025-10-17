package vn.edu.usth.ircui.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
/**See network connectivity changes*/
public class NetworkMonitor {
    public interface Callback {
        void onUp(); void onDown();
    }

    private final ConnectivityManager cm;
    private final ConnectivityManager.NetworkCallback cb;

    public NetworkMonitor(Context ctx, Callback callback) {
        cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        cb = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network network) { callback.onUp(); }
            @Override public void onLost(@NonNull Network network) { callback.onDown(); }
        };
    }
    public void start() {
        cm.registerDefaultNetworkCallback(cb);
    }
    public void stop()  {
        cm.unregisterNetworkCallback(cb);
    }
}
