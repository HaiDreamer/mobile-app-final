package vn.edu.usth.ircui.feature_chat.data;

public class Message {
    //New constants for UI adapters that use left/right/system types
    public static final int LEFT   = 0;
    public static final int RIGHT  = 1;
    public static final int SYSTEM = 2;

    private final String  username;
    private final String  content;
    private final long    timestamp;
    private final boolean mine;
    private final boolean codeBlock;

    // Maintain backward compatibility: derive "type" from "mine"
    private final int type;

    // Existing constructor used throughout the app
    public Message(String username, String content, boolean mine) {
        this.username  = username;
        this.content   = content;
        this.mine      = mine;
        this.timestamp = System.currentTimeMillis();
        this.codeBlock = looksLikeCode(content);
        this.type      = mine ? RIGHT : LEFT;           // map to new type for UI adapter
    }

    // Private constructor used by static factories below
    private Message(int type, String content) {
        this.username  = "";
        this.content   = content;
        this.mine      = (type == RIGHT);
        this.timestamp = System.currentTimeMillis();
        this.codeBlock = looksLikeCode(content);
        this.type      = type;
    }

    // ---- Static factories for feature_chat.ui.MessageAdapter ----
    public static Message left(String text)   { return new Message(LEFT,   text); }
    public static Message right(String text)  { return new Message(RIGHT,  text); }
    public static Message system(String text) { return new Message(SYSTEM, text); }

    // ---- Simple heuristics to detect code blocks (unchanged) ----
    private boolean looksLikeCode(String s) {
        String t = s == null ? "" : s.trim();
        if (t.startsWith("```") && t.endsWith("```")) return true;
        int symbols = 0;
        for (char c : t.toCharArray()) {
            if ("{}[]();<>#=/\\$\"'`".indexOf(c) >= 0) symbols++;
            if (symbols > 6) return true;
        }
        return false;
    }

    // ---- Existing getters (kept for older adapters) ----
    public String  getUsername()  { return username; }
    public String  getContent()   { return content; }
    public long    getTimestamp() { return timestamp; }
    public boolean isMine()       { return mine; }
    public boolean isCodeBlock()  { return codeBlock; }

    // ---- New getters used by feature_chat.ui.MessageAdapter ----
    /** Returns one of LEFT, RIGHT, SYSTEM. */
    public int getType() { return type; }

    /** Alias for getContent(); convenient for adapters that expect "text". */
    public String getText() { return content; }
}
