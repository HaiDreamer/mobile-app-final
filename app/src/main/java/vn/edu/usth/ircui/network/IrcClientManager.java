package vn.edu.usth.ircui.network;

import android.os.Looper;
import androidx.annotation.Nullable;

import net.engio.mbassy.listener.Handler;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.feature.auth.SaslExternal;
import org.kitteh.irc.client.library.feature.auth.SaslPlain;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Robust IRC manager:
 * - TLS on 6697
 * - Multi-server fallback
 * - Exponential backoff reconnect
 * - Optional SASL (PLAIN/EXTERNAL)
 */
public class IrcClientManager {

    public interface MessageCallback {
        void onMessage(String username, String text, long ts, boolean mine);
        void onSystem(String text);
    }

    private final android.os.Handler main = new android.os.Handler(Looper.getMainLooper());
    private volatile Client client;

    // ---- user/channel ----
    private String currentNick = "Guest";
    private String currentChannel = "#usth-ircui";

    // ---- servers (host,port,tls) ----
    public static class Server {
        public final String host; public final int port; public final boolean tls;
        public Server(String host, int port, boolean tls){ this.host=host; this.port=port; this.tls=tls; }
    }
    // Presets: Libera, OFTC, Rizon (all TLS 6697)
    private List<Server> servers = Arrays.asList(
            new Server("irc.libera.chat", 6697, true),
            new Server("irc.oftc.net",   6697, true),
            new Server("irc.rizon.net",  6697, true)
    );
    private int serverIndex = 0;

    // ---- reconnection ----
    private long backoffMs = 1500;           // start at 1.5s
    private final long backoffMaxMs = 60_000;// cap at 60s
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean wantConnected = new AtomicBoolean(false);

    // ---- SASL ----
    private @Nullable String saslUser, saslPass;
    private boolean saslExternal;

    private MessageCallback callback;

    public void setCallback(MessageCallback cb) { this.callback = cb; }

    public void setServers(List<Server> list) {
        if (list!=null && !list.isEmpty()) { servers = list; serverIndex = 0; }
    }

    public void connect(String nickname, String channel) {
        connectWithSasl(nickname, channel, null, null, false);
    }

    public void connectWithSasl(String nickname, String channel,
                                @Nullable String saslUser, @Nullable String saslPass,
                                boolean useExternal) {
        if (nickname != null && !nickname.isEmpty()) currentNick = nickname;
        if (channel != null && !channel.isEmpty())   currentChannel = channel;
        this.saslUser = saslUser;
        this.saslPass = saslPass;
        this.saslExternal = useExternal;

        wantConnected.set(true);
        serverIndex = Math.min(serverIndex, servers.size()-1);
        backoffMs = 1500;
        startConnectAttempt(); // async
    }

    public void disconnect() {
        wantConnected.set(false);
        Client c = client;
        client = null;
        if (c != null) {
            try { c.shutdown("Bye"); } catch (Exception ignored) {}
        }
    }

    public void sendMessage(String text) {
        if (client == null || text == null || text.trim().isEmpty()) return;
        try {
            client.sendMessage(currentChannel, text);
            postMessage(currentNick, text, System.currentTimeMillis(), true);
        } catch (Exception e) {
            postSystem("Send failed: " + e.getMessage());
        }
    }

    // ---------------- internal ----------------
    private void startConnectAttempt() {
        if (!wantConnected.get()) return;
        if (!connecting.compareAndSet(false, true)) return;

        final Server s = servers.get(serverIndex);
        new Thread(() -> {
            try {
                postSystem("Connecting to " + s.host + ":" + s.port + (s.tls?" (TLS)":""));
                Client c = Client.builder()
                        .nick(currentNick)
                        .realName("USTH IRC UI")
                        .server()
                        .host(s.host)
                        .port(s.port)  // TLS on 6697 is the IRC TLS standard. :contentReference[oaicite:1]{index=1}
                        .then()
                        .build();

                // --- SASL per KICL docs (PLAIN or EXTERNAL) ---
                if (saslExternal) {
                    c.getAuthManager().addProtocol(new SaslExternal(c));
                } else if (saslUser != null && !saslUser.isEmpty() && saslPass != null) {
                    c.getAuthManager().addProtocol(new SaslPlain(c, saslUser, saslPass));
                }
                // KICL performs CAP negotiation; SASL requires the 'sasl' capability. :contentReference[oaicite:2]{index=2}

                // Register listeners BEFORE connect so early events are caught
                c.getEventManager().registerEventListener(new Object() {

                    @Handler
                    public void onReady(ClientNegotiationCompleteEvent e) {
                        // reset backoff on success
                        backoffMs = 1500;
                        postSystem("Connected ("
                                + (s.tls ? "TLS " : "plain ")
                                + s.host + ":" + s.port + "). Joining " + currentChannel + "…");
                        c.addChannel(currentChannel);
                    }

                    @Handler
                    public void onMsg(ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        postMessage(from, msg, System.currentTimeMillis(), mine);
                    }

                    @Handler
                    public void onDisconnect(ClientConnectionEndedEvent e) {
                        String why = e.getCause().map(Throwable::getMessage).orElse("connection ended");
                        postSystem("Disconnected: " + why);
                        // Try to reconnect if user still wants to be online
                        handleReconnect();
                    }
                });

                client = c;
                c.connect(); // non-blocking connect/handshake
            } catch (Exception ex) {
                postSystem("Connect error: " + ex.getMessage());
                handleReconnect();
            } finally {
                connecting.set(false);
            }
        }, "irc-connect").start();
    }

    private void handleReconnect() {
        if (!wantConnected.get()) return;

        // Try next server (round-robin) on each failure
        serverIndex = (serverIndex + 1) % servers.size();

        // schedule with exponential backoff
        long delay = backoffMs;
        backoffMs = Math.min(backoffMaxMs, (long)(backoffMs * 1.7));
        postSystem("Reconnecting in " + (delay/1000) + "s… (server #" + (serverIndex+1) + ")");
        main.postDelayed(this::startConnectAttempt, delay);
    }

    private void postMessage(String u, String t, long ts, boolean mine) {
        if (callback == null) return;
        main.post(() -> callback.onMessage(u, t, ts, mine));
    }
    private void postSystem(String t) {
        if (callback == null) return;
        main.post(() -> callback.onSystem(t));
    }
}
