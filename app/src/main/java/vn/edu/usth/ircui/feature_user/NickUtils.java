package vn.edu.usth.ircui.feature_user;

import java.util.regex.Pattern;

/**Check if nick is valid or not
 * BUT NO USAGE*/

public final class NickUtils {
    private NickUtils() {}

    // Allowed characters per RFC 2812 §2.3.1: letters, digits (not first), and specials []\`_^{}|
    // Hyphen '-' is widely allowed in the "rest" on modern nets.
    private static final String FIRST_CLASS = "A-Za-z\\[\\]\\\\`_^\\{\\}\\|";
    private static final String REST_CLASS  = FIRST_CLASS + "0-9\\-";

    private static final Pattern VALID_FIRST = Pattern.compile("^[" + FIRST_CLASS + "]");
    private static final Pattern VALID_FULL  = Pattern.compile("^[" + FIRST_CLASS + "][" + REST_CLASS + "]*$");

    /**
     * Sanitize an arbitrary string into a likely-acceptable IRC nick.
     * @param raw    user input (can be anything)
     * @param maxLen maximum length to enforce (e.g., 9 for strict RFC, 32 for many networks)
     * @return sanitized nickname; guaranteed non-empty
     */
    public static String sanitize(String raw, int maxLen) {
        if (maxLen <= 0) maxLen = 32;
        String s = (raw == null ? "" : raw).trim();

        // 1) Replace any whitespace with underscores (spaces are illegal in nicks).
        s = s.replaceAll("\\s+", "_");

        // 2) Strip any chars not in the allowed sets.
        s = s.replaceAll("[^" + REST_CLASS + "]", "");

        // 3) Ensure first char is legal (letter or one of []\`_^{}|).
        if (s.isEmpty() || !VALID_FIRST.matcher(s.substring(0, 1)).find()) {
            // Prefix with underscore if it starts with a digit/hyphen; or generate a fallback.
            if (!s.isEmpty()) {
                s = "_" + s;
            } else {
                s = "Guest" + (int)(Math.random() * 9000 + 1000);
            }
        }

        // 4) Enforce length (many networks allow >9; 30–32 is common).
        if (s.length() > maxLen) s = s.substring(0, maxLen);

        // 5) Empty safety (shouldn’t happen after step 3, but just in case)
        if (s.isEmpty()) s = "Guest" + (int)(Math.random() * 9000 + 1000);

        return s;
    }

    /** Convenience overload with a reasonable default (32). */
    public static String sanitize(String raw) {
        return sanitize(raw, 32);
    }

    /**
     * Validate without mutating.
     * @param nick   proposed nick
     * @param maxLen max length policy for your target network
     */
    public static boolean isValid(String nick, int maxLen) {
        if (nick == null) return false;
        if (maxLen <= 0) maxLen = 32;
        if (nick.length() == 0 || nick.length() > maxLen) return false;
        return VALID_FULL.matcher(nick).matches();
    }

    /** Validate with default max length (32). */
    public static boolean isValid(String nick) {
        return isValid(nick, 32);
    }
}
