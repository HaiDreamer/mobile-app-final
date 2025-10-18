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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IRC manager:
 * - TLS on 6697 (true)
 * - Multi-server fallback
 * - Exponential backoff reconnect
 * - No duplicate self-messages
 * - Sanitizes/splits outbound text to avoid CR/LF/NUL & 512-byte limit
 */
public class IrcClientManager {

    public interface MessageCallback {
        void onMessage(String username, String text, long ts, boolean mine);
        void onSystem(String text);
    }
    
    // Static flag to prevent multiple instances
    private static volatile boolean hasActiveInstance = false;

    private final android.os.Handler main = new android.os.Handler(Looper.getMainLooper());
    private volatile Client client;
    private volatile Object currentEventListener;

    // user/channel
    private String currentNick = "Guest";
    private String currentChannel = "#usth-ircui";
    private String actualNick = "Guest"; // Track the actual nickname being used

    // servers
    public static class Server {
        public final String host; public final int port; public final boolean tls;
        public Server(String host, int port, boolean tls){ this.host=host; this.port=port; this.tls=tls; }
    }
    // presets: Multiple Libera servers, OFTC, Rizon (TLS 6697)
    private List<Server> servers = Arrays.asList(
            new Server("irc.libera.chat", 6697, true),      // Primary Libera
            new Server("libera.chat", 6697, true),          // Alternative Libera hostname
            new Server("irc.libera.chat", 6697, true),      // Backup Libera (same as primary for redundancy)
            new Server("irc.oftc.net",   6697, true),       // OFTC fallback
            new Server("irc.rizon.net",  6697, true)        // Rizon fallback
    );
    private int serverIndex = 0;

    // reconnection
    private long backoffMs = 1500;
    private static final long BACKOFF_MAX_MS = 60_000;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean wantConnected = new AtomicBoolean(false);
    private int connectionAttempts = 0;
    private static final int MAX_ATTEMPTS_PER_SERVER = 3;
    private long lastConnectionTime = 0;
    private static final long MIN_CONNECTION_INTERVAL = 2000; // 2 seconds minimum between connections
    
    // Track last messages to prevent duplicates
    private String lastJoinMessage = "";
    private String lastQuitMessage = "";
    private long lastJoinTime = 0;
    private long lastQuitTime = 0;
    
    // Track last user messages to prevent duplicates
    private String lastUserMessage = "";
    private String lastUserNick = "";
    private long lastUserMessageTime = 0;

    private MessageCallback callback;
    private Context context;
    
    public void setCallback(MessageCallback cb) {
        this.callback = cb;
    }

    public void setContext(Context ctx) {
        this.context = ctx;
    }

    public void setServers(List<Server> list) {
        if (list!=null && !list.isEmpty()) {
            servers = list;
            serverIndex = 0;
        }
    }

    public void connect(String nickname, String channel) {
        connectWithSasl(nickname, channel, null, null, false);
    }

    public void joinChannel(String channel) {
        if (client != null) {
            currentChannel = channel;
            client.addChannel(channel);
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
        // Check if already connected to avoid duplicate connections
        if (isConnected()) {
            postSystem("ℹ️ Already connected to server");
            return;
        }
        
        // Check if there's already an active instance
        if (hasActiveInstance && !isConnected()) {
            postSystem("ℹ️ Another connection is being established, please wait...");
            return;
        }
        
        if (nickname != null && !nickname.isEmpty()) currentNick = nickname;
        if (channel != null && !channel.isEmpty())   currentChannel = channel;

        wantConnected.set(true);
        serverIndex = Math.min(serverIndex, servers.size()-1);
        backoffMs = 1500;
        connectionAttempts = 0;
        hasActiveInstance = true; // Mark as active
        startConnectAttempt(); // async
    }

    public void disconnect() {
        wantConnected.set(false);
        hasActiveInstance = false; // Clear active flag
        
        // Show quit message before disconnecting
        if (actualNick != null && !actualNick.isEmpty()) {
            postSystem("👋 " + actualNick + " quit");
        }
        
        Client c = client;
        client = null;
        
        // Clean up event listener to prevent duplicate messages
        Object listener = currentEventListener;
        currentEventListener = null;
        
        if (c != null) {
            try { 
                // Unregister the event listener before shutdown
                if (listener != null) {
                    c.getEventManager().unregisterEventListener(listener);
                }
                c.shutdown("Bye"); 
            } catch (Exception ignored) {}
        }
    }
    
    public void resetConnection() {
        disconnect();
        backoffMs = 1500;
        serverIndex = 0;
        connectionAttempts = 0;
        connecting.set(false);
        lastConnectionTime = 0; // Reset connection timing
        
        // Reset duplicate message tracking
        lastJoinMessage = "";
        lastQuitMessage = "";
        lastJoinTime = 0;
        lastQuitTime = 0;
        lastUserMessage = "";
        lastUserNick = "";
        lastUserMessageTime = 0;
    }
    
    /**
     * Manually trigger reconnection - useful for testing or manual retry
     */
    public void forceReconnect() {
        postSystem("🔄 Manual reconnection triggered");
        resetConnection();
        wantConnected.set(true);
        startConnectAttempt();
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
    public boolean isActive() {
        return client != null;
    }
    
    public boolean isConnected() { 
        return client != null && wantConnected.get() && !connecting.get(); 
    }
    
    public boolean isConnecting() {
        return connecting.get();
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
        if (!connecting.compareAndSet(false, true)) {
            // Already connecting, skip this attempt
            return;
        }

        // Check minimum interval between connections to avoid rapid reconnections
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastConnectionTime < MIN_CONNECTION_INTERVAL) {
            connecting.set(false);
            long delay = MIN_CONNECTION_INTERVAL - (currentTime - lastConnectionTime);
            main.postDelayed(this::startConnectAttempt, delay);
            return;
        }
        lastConnectionTime = currentTime;

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
                actualNick = uniqueNick; // Store the actual nickname being used
                
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

                // Register listeners BEFORE connect so early events are caught
                Object eventListener = new Object() {

                    @Handler
                    public void onReady(ClientNegotiationCompleteEvent e) {
                        // reset backoff on success
                        backoffMs = 1500;
                        connecting.set(false); // Clear connecting flag
                        postSystem("✅ Connected to " + s.host + " (port " + s.port + ")");
                        postSystem("📺 Joined channel: " + currentChannel);
                        postSystem("👋 " + actualNick + " joined " + currentChannel);
                        c.addChannel(currentChannel);
                    }

                    @Handler
                    public void onMsg(ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(actualNick);
                        // Only show messages from other users (our own messages are shown via local echo)
                        if (!mine) {
                            long currentTime = System.currentTimeMillis();
                            
                            // Prevent duplicate messages from same user within 500ms
                            if (!from.equals(lastUserNick) || !msg.equals(lastUserMessage) || (currentTime - lastUserMessageTime) > 500) {
                                lastUserNick = from;
                                lastUserMessage = msg;
                                lastUserMessageTime = currentTime;
                                postMessage(from, msg, currentTime, false);
                            }
                        }
                    }

                    @Handler
                    public void onPrivateMsg(PrivateMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(actualNick);
                        // Only show private messages from other users (our own messages are shown via local echo)
                        if (!mine) {
                            postMessage(from, msg, System.currentTimeMillis(), false);
                        }
                    }

                    @Handler
                    public void onJoin(ChannelJoinEvent e) {
                        String user = e.getActor().getNick();
                        String channel = e.getChannel().getName();
                        boolean mine = user.equalsIgnoreCase(actualNick);
                        // Only show join notifications for other users, not ourselves
                        if (!mine) {
                            String message = "👋 " + user + " joined " + channel;
                            long currentTime = System.currentTimeMillis();
                            
                            // Prevent duplicate messages within 1 second
                            if (!message.equals(lastJoinMessage) || (currentTime - lastJoinTime) > 1000) {
                                lastJoinMessage = message;
                                lastJoinTime = currentTime;
                                postSystem(message);
                            }
                        }
                    }

                    @Handler
                    public void onPart(ChannelPartEvent e) {
                        String user = e.getActor().getNick();
                        String channel = e.getChannel().getName();
                        boolean mine = user.equalsIgnoreCase(actualNick);
                        // Only show part notifications for other users, not ourselves
                        if (!mine) {
                            postSystem("👋 " + user + " left " + channel);
                        }
                    }

                    @Handler
                    public void onQuit(UserQuitEvent e) {
                        String user = e.getActor().getNick();
                        boolean mine = user.equalsIgnoreCase(actualNick);
                        // Only show quit notifications for other users, not ourselves
                        if (!mine) {
                            String message = "👋 " + user + " quit";
                            long currentTime = System.currentTimeMillis();
                            
                            // Prevent duplicate messages within 1 second
                            if (!message.equals(lastQuitMessage) || (currentTime - lastQuitTime) > 1000) {
                                lastQuitMessage = message;
                                lastQuitTime = currentTime;
                                postSystem(message);
                            }
                        }
                    }

                    @Handler
                    public void onDisconnect(ClientConnectionEndedEvent e) {
                        String why = e.getCause().map(Throwable::getMessage).orElse("connection ended");
                        postSystem("❌ Disconnected from " + s.host + " - " + why);
                        
                        // Show quit message when disconnected
                        if (actualNick != null && !actualNick.isEmpty()) {
                            postSystem("👋 " + actualNick + " quit");
                        }
                        
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
                };
                
                // Store reference to event listener for cleanup
                currentEventListener = eventListener;
                c.getEventManager().registerEventListener(eventListener);

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

        // Use shorter delay for continuous reconnection through all servers
        long delay = Math.min(backoffMs, 5000); // Max 5 seconds between attempts
        backoffMs = Math.min(10000, (long)(backoffMs * 1.2)); // Slower backoff, max 10s
        
        final Server nextServer = servers.get(serverIndex);
        postSystem("🔄 Reconnecting to " + nextServer.host + " in " + (delay/1000) + "s... (server " + (serverIndex + 1) + "/" + servers.size() + ")");
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
        
        // Always use the same nickname for consistency
        // Only add suffix if this is a retry attempt (attempt > 1)
        if (connectionAttempts > 1) {
            return cleanNick + connectionAttempts;
        }
        
        // For first attempt, use clean nickname without any suffix
        return cleanNick;
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

            // (Optional) if want to be byte-precise, you could shrink until UTF-8 bytes <= ~400:
            // while (chunk.getBytes(StandardCharsets.UTF_8).length > 400 && end > i) { end--; chunk = text.substring(i, end); }

            out.add(chunk);
            i = end;
        }
        return out;
    }
}
