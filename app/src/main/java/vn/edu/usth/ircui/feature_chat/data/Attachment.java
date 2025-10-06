package vn.edu.usth.ircui.feature_chat.data;

import android.net.Uri;

public class Attachment {
    public enum Type { IMAGE, FILE }

    private final Type type;
    private final Uri uri;
    private final String displayName;
    private final long sizeBytes;

    public Attachment(Type type, Uri uri, String displayName, long sizeBytes) {
        this.type = type;
        this.uri = uri;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
    }

    public Type getType() { return type; }
    public Uri getUri() { return uri; }
    public String getDisplayName() { return displayName; }
    public long getSizeBytes() { return sizeBytes; }
}
