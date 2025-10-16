package vn.edu.usth.ircui.network;

import android.content.Context;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton IRC client that can be shared across all fragments
 * This ensures only one IRC connection per server and synchronized messaging
 */
public class SharedIrcClient {
    
    private static SharedIrcClient instance;
    private IrcClientManager ircClient;
    private final List<MessageCallback> callbacks = new CopyOnWriteArrayList<>();
    private final List<SystemMessageCallback> systemCallbacks = new CopyOnWriteArrayList<>();
    private String currentServer;
    private String currentUsername;
    private String currentChannel;
    
    private SharedIrcClient() {
        // Private constructor for singleton
    }
    
    public static synchronized SharedIrcClient getInstance() {
        if (instance == null) {
            instance = new SharedIrcClient();
        }
        return instance;
    }
    
    /**
     * Connect to IRC server with shared connection
     */
    public void connect(String serverHost, String username, String channel, Context context) {
        // If already connected to the same server, just update channel
        if (ircClient != null && ircClient.isConnected() && 
            currentServer != null && currentServer.equals(serverHost)) {
            
            // Update current info
            currentUsername = username;
            currentChannel = channel;
            
            // Join new channel if different
            if (!channel.equals(currentChannel)) {
                ircClient.joinChannel(channel);
                // Only notify when actually joining a new channel
                notifySystem("📺 Joined channel: " + channel);
            }
            // Don't send "Already connected" message to avoid duplicates
            return;
        }
        
        // Disconnect existing connection if different server
        if (ircClient != null) {
            ircClient.disconnect();
        }
        
        // Create new connection
        currentServer = serverHost;
        currentUsername = username;
        currentChannel = channel;
        
        ircClient = new IrcClientManager();
        ircClient.setContext(context);
        ircClient.setCallback(new IrcClientManager.MessageCallback() {
            @Override
            public void onMessage(String user, String text, long timestamp, boolean isMine) {
                // Forward to all registered callbacks
                notifyMessage(user, text, timestamp, isMine);
            }

            @Override
            public void onSystem(String text) {
                // Forward to all registered callbacks
                notifySystem(text);
            }
        });

        // Configure server list with Libera priority
        List<IrcClientManager.Server> servers = new ArrayList<>();
        servers.add(new IrcClientManager.Server(serverHost, 6697, true));
        servers.add(new IrcClientManager.Server("irc.libera.chat", 6697, true));      // Primary Libera
        servers.add(new IrcClientManager.Server("libera.chat", 6697, true));          // Alternative Libera hostname
        servers.add(new IrcClientManager.Server("irc.libera.chat", 6697, true));      // Backup Libera
        servers.add(new IrcClientManager.Server("irc.oftc.net", 6697, true));         // OFTC fallback
        servers.add(new IrcClientManager.Server("irc.rizon.net", 6697, true));        // Rizon fallback
        
        ircClient.setServers(servers);
        ircClient.connect(username, channel);
        
        notifySystem("🔄 Connecting to " + serverHost + "...");
    }
    
    /**
     * Register a callback to receive messages
     */
    public void registerCallback(MessageCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    /**
     * Unregister a callback
     */
    public void unregisterCallback(MessageCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * Register a callback to receive system messages only
     */
    public void registerSystemCallback(SystemMessageCallback callback) {
        if (!systemCallbacks.contains(callback)) {
            systemCallbacks.add(callback);
        }
    }
    
    /**
     * Unregister a system callback
     */
    public void unregisterSystemCallback(SystemMessageCallback callback) {
        systemCallbacks.remove(callback);
    }
    
    /**
     * Send a channel message
     */
    public void sendMessage(String text) {
        if (ircClient != null && ircClient.isConnected()) {
            ircClient.sendMessage(text);
        }
    }
    
    /**
     * Send a private message
     */
    public void sendPrivateMessage(String targetUser, String text) {
        if (ircClient != null && ircClient.isConnected()) {
            ircClient.sendPrivateMessage(targetUser, text);
        }
    }
    
    /**
     * Join a channel
     */
    public void joinChannel(String channel) {
        if (ircClient != null && ircClient.isConnected()) {
            currentChannel = channel;
            ircClient.joinChannel(channel);
        }
    }
    
    /**
     * Leave a channel
     */
    public void partChannel(String channel) {
        if (ircClient != null && ircClient.isConnected()) {
            ircClient.partChannel(channel);
        }
    }
    
    /**
     * Check if connected
     */
    public boolean isConnected() {
        return ircClient != null && ircClient.isConnected();
    }
    
    /**
     * Get current server
     */
    public String getCurrentServer() {
        return currentServer;
    }
    
    /**
     * Get current username
     */
    public String getCurrentUsername() {
        return currentUsername;
    }
    
    /**
     * Get current channel
     */
    public String getCurrentChannel() {
        return currentChannel;
    }
    
    /**
     * Disconnect
     */
    public void disconnect() {
        if (ircClient != null) {
            ircClient.disconnect();
            ircClient = null;
        }
        callbacks.clear();
        systemCallbacks.clear();
        currentServer = null;
        currentUsername = null;
        currentChannel = null;
    }
    
    /**
     * Reset connection (for reconnection)
     */
    public void resetConnection() {
        if (ircClient != null) {
            ircClient.resetConnection();
        }
    }
    
    /**
     * Manually trigger reconnection - useful for testing or manual retry
     */
    public void forceReconnect() {
        if (ircClient != null) {
            ircClient.forceReconnect();
        }
    }
    
    // Helper methods to notify all callbacks
    private void notifyMessage(String user, String text, long timestamp, boolean isMine) {
        for (MessageCallback callback : callbacks) {
            try {
                callback.onMessage(user, text, timestamp, isMine);
            } catch (Exception e) {
                // Remove faulty callback
                callbacks.remove(callback);
            }
        }
    }
    
    private void notifySystem(String text) {
        // Notify system message callbacks only
        for (SystemMessageCallback callback : systemCallbacks) {
            try {
                callback.onSystem(text);
            } catch (Exception e) {
                // Remove faulty callback
                systemCallbacks.remove(callback);
            }
        }
    }
    
    /**
     * Message callback interface
     */
    public interface MessageCallback {
        void onMessage(String username, String text, long timestamp, boolean isMine);
    }
    
    /**
     * System message callback interface
     */
    public interface SystemMessageCallback {
        void onSystem(String text);
    }
}
