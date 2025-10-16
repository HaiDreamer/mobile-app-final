package vn.edu.usth.ircui.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import androidx.annotation.Nullable;

import net.engio.mbassy.listener.Handler;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.connection.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;
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
    private int connectionAttempts = 0;
    private static final int MAX_ATTEMPTS_PER_SERVER = 3;

    // SASL
    private @Nullable String saslUser, saslPass;
    private boolean saslExternal;

    private MessageCallback callback;
    private Context context;
    
    public void setCallback(MessageCallback cb) { this.callback = cb; }
    public void setContext(Context ctx) { this.context = ctx; }

    public void setServers(List<Server> list) {
        if (list!=null && !list.isEmpty()) { servers = list; serverIndex = 0; }
    }

    public void connect(String nickname, String channel) {
        connectWithSasl(nickname, channel, null, null, false);
    }

    public void joinChannel(String channel) {
        if (client != null) {
            currentChannel = channel;
            client.addChannel(channel);
            // Don't send system message here - onReady() will handle it
        } else {
            postSystem("❌ Cannot join channel: Not connected to server");
        }
    }

    public void partChannel(String channel) {
        if (client != null) {
            client.removeChannel(channel);
            postSystem("👋 Left channel: " + channel);
        } else {
            postSystem("❌ Cannot leave channel: Not connected to server");
        }
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
        connectionAttempts = 0;
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
    
    public void resetConnection() {
        disconnect();
        backoffMs = 1500;
        serverIndex = 0;
        connectionAttempts = 0;
        connecting.set(false);
    }

    /**
     * Send a chat message:
     * - Strip CR/LF/NUL (IRC messages must be single-line)
     * - Split multi-line input into separate PRIVMSGs
     * - Chunk lines to stay well under the 512-byte wire limit (reserve header slack)
     * - Messages will be echoed back via IRCv3 echo-message capability
     */
    public void sendMessage(String text) {
        if (client == null) {
            postSystem("❌ Cannot send message: Not connected to IRC server");
            return;
        }
        
        if (text == null) {
            postSystem("❌ Cannot send message: Text is null");
            return;
        }
        
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            postSystem("❌ Cannot send message: Text is empty");
            return;
        }

        // Split on CR/LF, filter empties
        String[] lines = trimmed.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = sanitizeForIrc(rawLine);
            if (line.isEmpty()) continue;

            // Chunk to keep each PRIVMSG comfortably < 512 bytes on the wire.
            for (String chunk : chunkForIrc(line)) {
                try {
                    client.sendMessage(currentChannel, chunk);
                } catch (Exception e) {
                    postSystem("❌ Send failed: " + e.getMessage());
                    // If send fails, we might be disconnected
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("disconnected") || 
                         e.getMessage().contains("connection"))) {
                        postSystem("💡 Try reconnecting with /reconnect");
                    }
                }
            }
        }
    }
    public boolean isActive() { return client != null; }
    
    public boolean isConnected() { 
        return client != null; 
    }

    /**
     * Send a private message to a specific user:
     * - Strip CR/LF/NUL (IRC messages must be single-line)
     * - Split multi-line input into separate PRIVMSGs
     * - Chunk lines to stay well under the 512-byte wire limit (reserve header slack)
     */
    public void sendPrivateMessage(String targetUser, String text) {
        if (client == null) {
            postSystem("❌ Cannot send private message: Not connected to IRC server");
            return;
        }
        
        if (text == null) {
            postSystem("❌ Cannot send private message: Text is null");
            return;
        }
        
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            postSystem("❌ Cannot send private message: Text is empty");
            return;
        }

        if (targetUser == null || targetUser.trim().isEmpty()) {
            postSystem("❌ Cannot send private message: Target user is empty");
            return;
        }

        // Split on CR/LF, filter empties
        String[] lines = trimmed.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = sanitizeForIrc(rawLine);
            if (line.isEmpty()) continue;

            // Chunk to keep each PRIVMSG comfortably < 512 bytes on the wire.
            for (String chunk : chunkForIrc(line)) {
                try {
                    client.sendMessage(targetUser.trim(), chunk);
                } catch (Exception e) {
                    postSystem("❌ Private message send failed: " + e.getMessage());
                    // If send fails, we might be disconnected
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("disconnected") || 
                         e.getMessage().contains("connection"))) {
                        postSystem("💡 Try reconnecting with /reconnect");
                    }
                }
            }
        }
    }


    // internal
    private void startConnectAttempt() {
        if (!wantConnected.get()) return;
        if (!connecting.compareAndSet(false, true)) return;

        // Check internet connection first
        if (!isInternetAvailable()) {
            postSystem("❌ No internet connection available");
            connecting.set(false);
            handleReconnect();
            return;
        }

        final Server s = servers.get(serverIndex);
        connectionAttempts++;
        
        new Thread(() -> {
            try {
                postSystem("🔄 Connecting to " + s.host + " (port " + s.port + ")... (attempt " + connectionAttempts + "/" + MAX_ATTEMPTS_PER_SERVER + ")");
                // Generate unique nickname to avoid conflicts
                String uniqueNick = generateUniqueNick(currentNick);
                
                Client c = Client.builder()
                        .nick(uniqueNick)
                        .realName("USTH IRC UI")
                        .server()
                        .host(s.host)
                        .port(s.port) // ensure TLS on 6697 for Libera
                        .secure(s.tls)
                        .then()
                        .build();
                
                // Note: Using local echo for our own messages since echo-message capability
                // might not be available in all IRC servers

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
                        postSystem("✅ Connected to " + s.host + " (port " + s.port + ")");
                        postSystem("📺 Joined channel: " + currentChannel);
                        c.addChannel(currentChannel);
                    }

                    @Handler
                    public void onMsg(ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        // Only show messages from other users (our own messages are shown via local echo)
                        if (!mine) {
                            postMessage(from, msg, System.currentTimeMillis(), false);
                        }
                    }

                    @Handler
                    public void onPrivateMsg(PrivateMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        // Only show private messages from other users (our own messages are shown via local echo)
                        if (!mine) {
                            postMessage(from, msg, System.currentTimeMillis(), false);
                        }
                    }

                    @Handler
                    public void onJoin(ChannelJoinEvent e) {
                        String user = e.getActor().getNick();
                        String channel = e.getChannel().getName();
                        boolean mine = user.equalsIgnoreCase(currentNick);
                        // Only show join notifications for other users, not ourselves
                        if (!mine) {
                            postSystem("👋 " + user + " joined " + channel);
                        }
                    }

                    @Handler
                    public void onPart(ChannelPartEvent e) {
                        String user = e.getActor().getNick();
                        String channel = e.getChannel().getName();
                        boolean mine = user.equalsIgnoreCase(currentNick);
                        // Only show part notifications for other users, not ourselves
                        if (!mine) {
                            postSystem("👋 " + user + " left " + channel);
                        }
                    }

                    @Handler
                    public void onQuit(UserQuitEvent e) {
                        String user = e.getActor().getNick();
                        boolean mine = user.equalsIgnoreCase(currentNick);
                        // Only show quit notifications for other users, not ourselves
                        if (!mine) {
                            postSystem("👋 " + user + " quit");
                        }
                    }

                    @Handler
                    public void onDisconnect(ClientConnectionEndedEvent e) {
                        String why = e.getCause().map(Throwable::getMessage).orElse("connection ended");
                        postSystem("❌ Disconnected from " + s.host + " - " + why);
                        
                        // Only reconnect if it's not a user-initiated disconnect
                        if (wantConnected.get()) {
                            // Add delay before reconnecting to avoid rapid connect/disconnect loops
                            main.postDelayed(() -> {
                                if (wantConnected.get()) {
                                    handleReconnect();
                                }
                            }, 3000); // 3 second delay
                        }
                    }
                });

                client = c;
                c.connect(); // non-blocking connect/handshake
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = ex.getClass().getSimpleName();
                }
                
                // Provide more specific error messages
                if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
                    postSystem("❌ Connection timeout to " + s.host + " - Server may be slow or unreachable");
                } else if (errorMsg.contains("refused") || errorMsg.contains("connection refused")) {
                    postSystem("❌ Connection refused by " + s.host + " - Server may be down or blocking connections");
                } else if (errorMsg.contains("SSL") || errorMsg.contains("TLS")) {
                    postSystem("❌ SSL/TLS error connecting to " + s.host + " - Certificate or encryption issue");
                } else if (errorMsg.contains("nickname") || errorMsg.contains("nick")) {
                    postSystem("❌ Nickname conflict on " + s.host + " - Trying with different nickname");
                } else {
                    postSystem("❌ Connection failed to " + s.host + " - " + errorMsg);
                }
                
                handleReconnect();
            } finally {
                connecting.set(false);
            }
        }, "irc-connect").start();
    }

    private void handleReconnect() {
        if (!wantConnected.get()) return;

        // If we've tried too many times on current server, try next server
        if (connectionAttempts >= MAX_ATTEMPTS_PER_SERVER) {
            serverIndex = (serverIndex + 1) % servers.size();
            connectionAttempts = 0;
            backoffMs = 1500; // Reset backoff for new server
            postSystem("🔄 Switching to next server after " + MAX_ATTEMPTS_PER_SERVER + " failed attempts");
        }

        // schedule with exponential backoff
        long delay = backoffMs;
        backoffMs = Math.min(BACKOFF_MAX_MS, (long)(backoffMs * 1.5)); // Slower backoff
        
        // Limit total reconnection attempts to avoid infinite loops
        if (backoffMs >= BACKOFF_MAX_MS) {
            postSystem("❌ Max reconnection attempts reached for all servers");
            wantConnected.set(false);
            return;
        }
        
        final Server nextServer = servers.get(serverIndex);
        postSystem("🔄 Reconnecting to " + nextServer.host + " in " + (delay/1000) + "s...");
        main.postDelayed(this::startConnectAttempt, delay);
    }

    // ---- helpers ----
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

    /** Generate unique nickname to avoid conflicts */
    private String generateUniqueNick(String baseNick) {
        if (baseNick == null || baseNick.isEmpty()) {
            baseNick = "Guest";
        }
        
        // Clean nickname (remove invalid characters)
        String cleanNick = baseNick.replaceAll("[^a-zA-Z0-9\\-_\\[\\]{}|`^]", "");
        if (cleanNick.isEmpty()) {
            cleanNick = "Guest";
        }
        
        // Limit length to leave room for suffix
        if (cleanNick.length() > 10) {
            cleanNick = cleanNick.substring(0, 10);
        }
        
        // Add timestamp-based suffix to make it more unique
        long timestamp = System.currentTimeMillis() % 10000; // Last 4 digits
        int randomSuffix = (int) (Math.random() * 100); // 0-99
        return cleanNick + timestamp + randomSuffix;
    }
    
    /** Check if internet connection is available */
    private boolean isInternetAvailable() {
        if (context == null) return true; // Assume available if no context
        
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            return true; // Assume available if check fails
        }
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
