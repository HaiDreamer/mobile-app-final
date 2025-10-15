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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IRC manager:
 * - TLS on 6697 (true)
 * - Multi-server fallback
 * - Exponential backoff reconnect
 * - Optional SASL (PLAIN/EXTERNAL)
 * - No duplicate self-messages (use IRCv3 echo-message)
 * - Sanitizes/splits outbound text to avoid CR/LF/NUL & 512-byte limit
 */
public class IrcClientManager {

    public interface MessageCallback {
        void onMessage(String username, String text, long ts, boolean mine);
        void onSystem(String text);
    }

    private final android.os.Handler main = new android.os.Handler(Looper.getMainLooper());
    private volatile Client client;

    // user/channel
    private String currentNick = "Guest";
    private String currentChannel = "#usth-ircui";

    // servers
    public static class Server {
        public final String host; public final int port; public final boolean tls;
        public Server(String host, int port, boolean tls){ this.host=host; this.port=port; this.tls=tls; }
    }
    // presets: Libera, OFTC, Rizon (TLS 6697)
    private List<Server> servers = Arrays.asList(
            new Server("irc.libera.chat", 6697, true),
            new Server("irc.oftc.net",   6697, true),
            new Server("irc.rizon.net",  6697, true)
    );
    private int serverIndex = 0;

    // reconnection
    private long backoffMs = 1500;
    private static final long BACKOFF_MAX_MS = 60_000;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean wantConnected = new AtomicBoolean(false);

    // SASL
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

    /**
     * Send a chat message
     */
    public void sendMessage(String text) {
        if (client == null || text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        // Split on CR/LF, filter empties
        String[] lines = trimmed.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = sanitizeForIrc(rawLine);
            if (line.isEmpty()) continue;

            // Chunk to keep each PRIVMSG comfortably < 512 bytes on the wire.
            for (String chunk : chunkForIrc(line)) {
                try {
                    client.sendMessage(currentChannel, chunk);
                    // debugging, and that is the REASON why double message
                    postMessage(currentNick, text, System.currentTimeMillis(), true);
                } catch (Exception e) {
                    postSystem("Send failed: " + e.getMessage());
                }
            }
        }
    }

    // internal
    private void startConnectAttempt() {
        if (!wantConnected.get()) return;
        if (!connecting.compareAndSet(false, true)) return;

        final Server s = servers.get(serverIndex);
        new Thread(() -> {
            try {
                postSystem("Connecting to " + s.host + ":" + s.port + (s.tls ? " (TLS)" : ""));
                Client c = Client.builder()
                        .nick(currentNick)
                        .realName("USTH IRC UI")
                        .server()
                        .host(s.host)
                        .port(s.port) // ensure TLS on 6697 for Libera
                        .then()
                        .build();

                // SASL per KICL (PLAIN/EXTERNAL)
                if (saslExternal) {
                    c.getAuthManager().addProtocol(new SaslExternal(c));
                } else if (saslUser != null && !saslUser.isEmpty() && saslPass != null) {
                    c.getAuthManager().addProtocol(new SaslPlain(c, saslUser, saslPass));
                }
                // (Capability negotiation handles 'sasl' automatically when a protocol is added.)

                // Register listeners BEFORE connect so early events are caught
                c.getEventManager().registerEventListener(new Object() {

                    @Handler
                    public void onReady(ClientNegotiationCompleteEvent e) {
                        // reset backoff on success
                        backoffMs = 1500;
                        postSystem("Connected (" + (s.tls ? "TLS " : "plain ")
                                + s.host + ":" + s.port + "). Joining " + currentChannel + "…");
                        c.addChannel(currentChannel);
                    }

                    @Handler
                    public void onMsg(ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        // Single source of truth: render on server echo (mine==true) and everyone else's messages.
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
        backoffMs = Math.min(BACKOFF_MAX_MS, (long)(backoffMs * 1.7));
        postSystem("Reconnecting in " + (delay/1000) + "s… (server #" + (serverIndex+1) + ")");
        main.postDelayed(this::startConnectAttempt, delay);
    }

    // helpers
    private void postMessage(String u, String t, long ts, boolean mine) {
        if (callback == null) return;
        main.post(() -> callback.onMessage(u, t, ts, mine));
    }
    private void postSystem(String t) {
        if (callback == null) return;
        main.post(() -> callback.onSystem(t));
    }

    /** Strip forbidden control chars (CR/LF/NUL) and trim. IRC messages must be single-line. */
    private static String sanitizeForIrc(String s) {
        if (s == null) return "";
        return s.replace("\r", " ")
                .replace("\n", " ")
                .replace("\0", " ")
                .trim();
    }

    /** Chunk a UTF-8 string to keep each PRIVMSG well under the 512-byte limit (header + tags eat bytes). */
    private static List<String> chunkForIrc(String text) {
        // Very conservative: split by characters; servers enforce ~512 bytes including CRLF & prefix. :contentReference[oaicite:5]{index=5}
        List<String> out = new ArrayList<>();
        if (text.isEmpty()) return out;

        int i = 0;
        while (i < text.length()) {
            int end = Math.min(text.length(), i + 400);

            // try not to cut in the middle of a surrogate pair
            if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
                end--;
            }

            String chunk = text.substring(i, end);

            // (Optional) if you want to be byte-precise, you could shrink until UTF-8 bytes <= ~400:
            // while (chunk.getBytes(StandardCharsets.UTF_8).length > 400 && end > i) { end--; chunk = text.substring(i, end); }

            out.add(chunk);
            i = end;
        }
        return out;
    }
}
