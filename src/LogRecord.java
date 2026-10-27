import java.time.Instant;
import java.util.Map;

/** Immutable domain record produced by the log parser. */
public record LogRecord(
        Instant timestamp,
        String clientIp,
        String method,
        String path,
        int status,
        long bytes,
        String userAgent,
        Map<String, String> attributes,
        String raw) implements Record {

    public LogRecord {
        if (timestamp == null || clientIp == null || method == null || path == null
                || userAgent == null || attributes == null || raw == null) {
            throw new IllegalArgumentException("LogRecord fields must not be null");
        }
        attributes = Map.copyOf(attributes);
    }
}
