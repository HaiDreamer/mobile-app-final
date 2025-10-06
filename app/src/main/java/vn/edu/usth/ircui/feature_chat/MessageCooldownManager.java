package vn.edu.usth.ircui.feature_chat;

public class MessageCooldownManager {

    private static final long COOLDOWN_PERIOD_MS = 2000;
    private long lastMessageTimestamp = 0;
    private static final MessageCooldownManager instance = new MessageCooldownManager();

    private MessageCooldownManager() {}

    public static MessageCooldownManager getInstance() {
        return instance;
    }

    public boolean isCooldownActive() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastMessageTimestamp) < COOLDOWN_PERIOD_MS;
    }

    public void recordMessageSent() {
        this.lastMessageTimestamp = System.currentTimeMillis();
    }
}