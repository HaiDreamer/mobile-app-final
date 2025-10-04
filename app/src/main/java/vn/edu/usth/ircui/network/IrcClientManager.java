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


//create a client → connect to irc.libera.chat:6697 over TLS → join a channel → send/receive messages—not to host anything locally.
public class IrcClientManager {

    public interface MessageCallback {
        void onMessage(String username, String text, long ts, boolean mine);
        void onSystem(String text);
    }

    private final android.os.Handler main = new android.os.Handler(Looper.getMainLooper());

    private org.kitteh.irc.client.library.Client client;
    private String currentNick = "Guest";
    private String currentChannel = "#usth-ircui";
    private MessageCallback callback;

    public void setCallback(MessageCallback cb) { this.callback = cb; }

    public void connect(String nickname, String channel) {
        if (nickname != null && !nickname.isEmpty()) currentNick = nickname;
        if (channel != null && !channel.isEmpty())   currentChannel = channel;

        new Thread(() -> {
            try {
                client = org.kitteh.irc.client.library.Client.builder()
                        .nick(currentNick)
                        .realName("USTH IRC UI")
                        .server()
                        .host("irc.libera.chat")
                        .port(6697) // TLS ✅
                        .then()
                        .buildAndConnect();

                client.getEventManager().registerEventListener(new Object() {

                    // Ready to go (handshake/negotiation completed)
                    @net.engio.mbassy.listener.Handler
                    public void onReady(ClientNegotiationCompleteEvent e) {
                        client.addChannel(currentChannel);
                        postSystem("Connected. Joined " + currentChannel);
                    }

                    // Channel message
                    @net.engio.mbassy.listener.Handler
                    public void onMsg(ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        postMessage(from, msg, System.currentTimeMillis(), mine);
                    }

                    // Disconnected (covers closed/failed)
                    @net.engio.mbassy.listener.Handler
                    public void onDisconnect(ClientConnectionEndedEvent e) {
                        String why = e.getCause().map(Throwable::getMessage)
                                .orElse("connection ended");
                        postSystem("Disconnected: " + why);
                    }
                });

            } catch (Exception ex) {
                postSystem("Connect error: " + ex.getMessage());
            }
        }, "kicl-connect").start();
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

    public void disconnect() {
        try {
            if (client != null) {
                client.shutdown("Bye");
                client = null;
            }
        } catch (Exception ignored) {}
    }

    // ----- helpers -----
    private void postMessage(String u, String t, long ts, boolean mine) {
        if (callback == null) return;
        main.post(() -> callback.onMessage(u, t, ts, mine));
    }
    private void postSystem(String t) {
        if (callback == null) return;
        main.post(() -> callback.onSystem(t));
    }

    // in IrcClientManager.java
    public void connectWithSasl(String nickname, String channel,
                                @Nullable String saslUser, @Nullable String saslPass,
                                boolean useExternal) {
        if (nickname != null && !nickname.isEmpty()) currentNick = nickname;
        if (channel != null && !channel.isEmpty())   currentChannel = channel;

        new Thread(() -> {
            try {
                client = Client.builder()
                        .nick(currentNick)
                        .realName("USTH IRC UI")
                        .server()
                        .host("irc.libera.chat")
                        .port(6697) // TLS
                        .then()
                        .build();

                // --- SASL setup ---
                if (useExternal) {
                    // EXTERNAL: use client TLS certificate (you must install/provide it in your SSLSocketFactory)
                    client.getAuthManager().addProtocol(
                            new SaslExternal(client));
                } else if (saslUser != null && !saslUser.isEmpty() && saslPass != null) {
                    client.getAuthManager().addProtocol(
                            new SaslPlain(client, saslUser, saslPass));
                }
                // Request the 'sasl' CAP so AUTHENTICATE can happen during negotiation
                client.getServerInfo().getISupportParameter("sasl"); // harmless hint
                // --- end SASL ---

                client.connect(); // start connection/negotiation (non-blocking)

                client.getEventManager().registerEventListener(new Object() {
                    @Handler
                    public void onReady(
                            ClientNegotiationCompleteEvent e) {
                        client.addChannel(currentChannel);
                        postSystem("Connected. Joined " + currentChannel);
                    }

                    @Handler
                    public void onMsg(
                            ChannelMessageEvent e) {
                        String from = e.getActor().getNick();
                        String msg  = e.getMessage();
                        boolean mine = from.equalsIgnoreCase(currentNick);
                        postMessage(from, msg, System.currentTimeMillis(), mine);
                    }

                    @Handler
                    public void onDisconnect(
                            ClientConnectionEndedEvent e) {
                        String why = e.getCause().map(Throwable::getMessage).orElse("connection ended");
                        postSystem("Disconnected: " + why);
                    }
                });

            } catch (Exception ex) {
                postSystem("Connect error: " + ex.getMessage());
            }
        }, "kicl-connect").start();
    }

}
