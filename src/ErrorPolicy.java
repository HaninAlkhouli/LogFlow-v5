import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recovery action applied when one record fails inside a stage. */
public record ErrorPolicy(Action action, int retries) {
    private static final Pattern RETRY = Pattern.compile("RETRY\\((\\d+)\\)");

    public enum Action {
        FAIL_FAST,
        SKIP,
        DEAD_LETTER,
        RETRY
    }

    public ErrorPolicy {
        if (action == null) throw new IllegalArgumentException("Error policy action is required");
        if (action == Action.RETRY && retries < 1) {
            throw new IllegalArgumentException("RETRY count must be at least 1");
        }
        if (action != Action.RETRY && retries != 0) {
            throw new IllegalArgumentException("Only RETRY may define a retry count");
        }
    }

    public static ErrorPolicy failFast() {
        return new ErrorPolicy(Action.FAIL_FAST, 0);
    }

    public static ErrorPolicy parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigException("Error policy must be a non-empty string");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        Matcher retry = RETRY.matcher(normalized);
        if (retry.matches()) {
            try {
                return new ErrorPolicy(Action.RETRY, Integer.parseInt(retry.group(1)));
            } catch (IllegalArgumentException error) {
                throw new ConfigException("Invalid RETRY policy: " + value, error);
            }
        }
        try {
            return new ErrorPolicy(Action.valueOf(normalized), 0);
        } catch (IllegalArgumentException error) {
            throw new ConfigException(
                    "Error policy must be FAIL_FAST, SKIP, DEAD_LETTER or RETRY(n)", error);
        }
    }

    @Override
    public String toString() {
        return action == Action.RETRY ? "RETRY(" + retries + ")" : action.name();
    }
}
