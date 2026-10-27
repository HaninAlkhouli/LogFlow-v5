import java.time.Instant;

/** Diagnostic record written to the independent dead-letter channel. */
public record FailedRecord(
        String rawInput,
        String stageName,
        String exception,
        Instant timestamp) {

    public FailedRecord {
        if (rawInput == null || stageName == null || exception == null || timestamp == null) {
            throw new IllegalArgumentException("FailedRecord fields must not be null");
        }
    }

    public static FailedRecord from(Object input, String stageName, StageException error) {
        return new FailedRecord(
                rawInputOf(input),
                stageName,
                error.getClass().getSimpleName() + ": " + error.getMessage(),
                Instant.now());
    }

    public String toLine() {
        return "timestamp=" + timestamp
                + " stage=" + escape(stageName)
                + " exception=\"" + escape(exception) + "\""
                + " raw=\"" + escape(rawInput) + "\"";
    }

    private static String rawInputOf(Object input) {
        if (input instanceof RawLine line) return line.rawInput();
        if (input instanceof LogRecord record) return record.raw();
        if (input instanceof ProjectedRecord record) return record.toLine();
        return String.valueOf(input);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }
}
