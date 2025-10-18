package vn.edu.usth.ircui.feature_chat.data;

public class Message {
    private final String username;
    private final String content;
    private final long timestamp;
    private final boolean mine;
    private final boolean codeBlock;

    public Message(String username, String content, boolean mine) {
        this.username  = username;
        this.content   = content;
        this.mine      = mine;
        this.timestamp = System.currentTimeMillis();
        this.codeBlock = looksLikeCode(content);
    }

    private boolean looksLikeCode(String s) {
        // very simple detection: fenced code or many symbols
        String t = s.trim();
        if (t.startsWith("```") && t.endsWith("```")) return true;
        int symbols = 0;
        for (char c : t.toCharArray()) {
            if ("{}[]();<>#=/\\$\"'`".indexOf(c) >= 0) symbols++;
            if (symbols > 6) return true;
        }
        return false;
    }

    public String  getUsername()  { return username; }
    public String  getContent()   { return content; }
    public long    getTimestamp() { return timestamp; }
    public boolean isMine()       { return mine; }
    public boolean isCodeBlock()  { return codeBlock; }
}
