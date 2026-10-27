import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Applies declarative field, range, timestamp and IPv4 validation rules. */
public final class ValidationStage implements Stage<LogRecord, LogRecord> {
    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "clientIp", "method", "path", "userAgent");
    private static final Pattern IPV4 = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)"
                    + "(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

    private final List<String> requiredFields;
    private final Long minimumBytes;
    private final Long maximumBytes;
    private final Integer minimumStatus;
    private final Integer maximumStatus;
    private final Instant minimumTimestamp;
    private final Instant maximumTimestamp;
    private final boolean validateIpv4;

    public ValidationStage(
            List<String> requiredFields,
            Long minimumBytes,
            Long maximumBytes,
            Integer minimumStatus,
            Integer maximumStatus,
            Instant minimumTimestamp,
            Instant maximumTimestamp,
            boolean validateIpv4) {
        this.requiredFields = List.copyOf(requiredFields);
        this.requiredFields.forEach(field -> {
            if (!REQUIRED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unsupported required field: " + field);
            }
        });
        requireOrdered(minimumBytes, maximumBytes, "bytes range");
        requireOrdered(minimumStatus, maximumStatus, "status range");
        if (minimumTimestamp != null && maximumTimestamp != null
                && minimumTimestamp.isAfter(maximumTimestamp)) {
            throw new IllegalArgumentException("timestamp range minimum must not exceed maximum");
        }
        this.minimumBytes = minimumBytes;
        this.maximumBytes = maximumBytes;
        this.minimumStatus = minimumStatus;
        this.maximumStatus = maximumStatus;
        this.minimumTimestamp = minimumTimestamp;
        this.maximumTimestamp = maximumTimestamp;
        this.validateIpv4 = validateIpv4;
    }

    @Override
    public void process(LogRecord record, Emitter<LogRecord> out) throws StageException {
        for (String field : requiredFields) {
            if (requiredValue(record, field).isBlank()) {
                throw new StageException("Required field is blank: " + field);
            }
        }
        if (minimumBytes != null && record.bytes() < minimumBytes) {
            throw new StageException("bytes is below minimum " + minimumBytes);
        }
        if (maximumBytes != null && record.bytes() > maximumBytes) {
            throw new StageException("bytes exceeds maximum " + maximumBytes);
        }
        if (minimumStatus != null && record.status() < minimumStatus) {
            throw new StageException("status is below minimum " + minimumStatus);
        }
        if (maximumStatus != null && record.status() > maximumStatus) {
            throw new StageException("status exceeds maximum " + maximumStatus);
        }
        if (minimumTimestamp != null && record.timestamp().isBefore(minimumTimestamp)) {
            throw new StageException("timestamp is before " + minimumTimestamp);
        }
        if (maximumTimestamp != null && record.timestamp().isAfter(maximumTimestamp)) {
            throw new StageException("timestamp is after " + maximumTimestamp);
        }
        if (validateIpv4 && !IPV4.matcher(record.clientIp()).matches()) {
            throw new StageException("clientIp is not a valid IPv4 address");
        }
        out.emit(record);
    }

    private static String requiredValue(LogRecord record, String field) {
        return switch (field) {
            case "clientIp" -> record.clientIp();
            case "method" -> record.method();
            case "path" -> record.path();
            case "userAgent" -> record.userAgent();
            default -> throw new IllegalStateException(field);
        };
    }

    private static <T extends Comparable<T>> void requireOrdered(T minimum, T maximum, String name) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(name + " minimum must not exceed maximum");
        }
    }
}
