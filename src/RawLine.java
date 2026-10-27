import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** One physical input line with its original bytes and UTF-8 validity. */
public final class RawLine {
    private final byte[] bytes;
    private final String text;

    private RawLine(byte[] bytes, String text) {
        this.bytes = bytes;
        this.text = text;
    }

    public static RawLine fromBytes(byte[] value) {
        byte[] copy = value.clone();
        String decoded = null;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(copy))
                    .toString();
        } catch (CharacterCodingException ignored) {
            // The parser routes invalid UTF-8 through the configured error policy.
        }
        return new RawLine(copy, decoded);
    }

    public static RawLine fromUtf8(String text) {
        if (text == null) throw new IllegalArgumentException("Raw line must not be null");
        return fromBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validUtf8() {
        return text != null;
    }

    public int byteLength() {
        return bytes.length;
    }

    public String text() {
        if (text == null) throw new IllegalStateException("Raw line is not valid UTF-8");
        return text;
    }

    public String rawInput() {
        return text != null ? text : "base64:" + Base64.getEncoder().encodeToString(bytes);
    }
}
