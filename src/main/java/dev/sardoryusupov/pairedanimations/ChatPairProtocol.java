package dev.sardoryusupov.pairedanimations;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PM wire format as displayed by MigosMC:
 * [~Player -> я] PA1:I:123456abcdef
 *
 * This parser ignores everything except a full private-message envelope
 * containing a strict PairedAnimations packet. In particular plain chat
 * and outgoing message echoes are never treated as incoming invitations.
 */
public final class ChatPairProtocol {
    private static final Pattern PRIVATE_MESSAGE = Pattern.compile(
        "^\\s*\\[\\s*(.+?)\\s*(?:->|→)\\s*(.+?)\\s*]\\s*"
        + "(PA1:[IASDC]:[0-9a-fA-F]{12}(?::[0-9]{13})?)\\s*$"
    );
    private static final Pattern USERNAME = Pattern.compile("([A-Za-z0-9_]{3,16})\\s*$");
    private static final Pattern NONCE = Pattern.compile("[0-9a-f]{12}");

    private ChatPairProtocol() {}

    public static Parsed parse(String renderedMessage, String localName) {
        if (renderedMessage == null || localName == null) return null;
        // Minecraft's Component#getString usually strips styling, but servers
        // may embed legacy color codes in the text itself.
        String text = renderedMessage.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
        Matcher match = PRIVATE_MESSAGE.matcher(text);
        if (!match.matches()) return null;

        String to = match.group(2).trim();
        if (!(to.equalsIgnoreCase("я") || to.equalsIgnoreCase("me")
            || to.equalsIgnoreCase(localName))) return null;
        String sender = normalizedName(match.group(1));
        if (sender == null || sender.equalsIgnoreCase(localName)) return null;

        String[] parts = match.group(3).split(":");
        if (parts.length != 3 && parts.length != 4) return null;
        char operation = parts[1].charAt(0);
        if (!NONCE.matcher(parts[2].toLowerCase(Locale.ROOT)).matches()) return null;
        if (operation == 'S' ? parts.length != 4 : parts.length != 3) return null;
        long startEpochMillis = 0;
        if (operation == 'S') {
            try {
                startEpochMillis = Long.parseLong(parts[3]);
            } catch (NumberFormatException invalid) {
                return null;
            }
        }
        return new Parsed(sender, operation, parts[2].toLowerCase(Locale.ROOT), startEpochMillis);
    }

    public static String encode(char operation, String nonce, long startEpochMillis) {
        if ("IASDC".indexOf(operation) < 0 || nonce == null || !NONCE.matcher(nonce).matches())
            throw new IllegalArgumentException("Invalid packet");
        if (operation == 'S') {
            if (startEpochMillis <= 0) throw new IllegalArgumentException("Missing start time");
            return "PA1:S:" + nonce + ":" + startEpochMillis;
        }
        return "PA1:" + operation + ":" + nonce;
    }

    static String normalizedName(String source) {
        if (source == null) return null;
        Matcher matcher = USERNAME.matcher(source.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    public record Parsed(String senderName, char operation, String nonce, long startEpochMillis) {}
}
