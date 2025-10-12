package vn.edu.usth.ircui;

/** Simple model for a conversation row used by ConversationAdapter. */
public class Conversation {
    public final String title;  // e.g., peer or channel name
    public final String last;   // last message preview
    public final String time;   // relative/absolute time string (e.g., "2m", "09:14")

    public Conversation(String title, String last, String time) {
        this.title = title;
        this.last  = last;
        this.time  = time;
    }
}
