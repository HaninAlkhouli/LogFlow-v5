import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Keeps only the requested LogRecord fields, in the requested order. */
public final class ProjectionStage implements Stage<LogRecord, ProjectedRecord> {
    private static final Set<String> SUPPORTED = Set.of(
            "timestamp", "clientIp", "method", "path", "status",
            "bytes", "userAgent", "attributes", "raw");
    private final List<String> fields;

    public ProjectionStage(String... fields) {
        this(List.of(fields));
    }

    public ProjectionStage(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("At least one field is required");
        }
        List<String> copy = List.copyOf(fields);
        copy.forEach(field -> {
            if (!SUPPORTED.contains(field)) {
                throw new IllegalArgumentException("Unknown field: " + field);
            }
        });
        this.fields = copy;
    }

    @Override
    public void process(LogRecord record, Emitter<ProjectedRecord> out) {
        LinkedHashMap<String, Object> selected = new LinkedHashMap<>();
        fields.forEach(field -> selected.put(field, valueOf(record, field)));
        out.emit(new ProjectedRecord(selected));
    }

    private static Object valueOf(LogRecord record, String field) {
        return switch (field) {
            case "timestamp" -> record.timestamp(); case "clientIp" -> record.clientIp();
            case "method" -> record.method(); case "path" -> record.path();
            case "status" -> record.status(); case "bytes" -> record.bytes();
            case "userAgent" -> record.userAgent(); case "attributes" -> record.attributes();
            case "raw" -> record.raw(); default -> throw new IllegalStateException(field);
        };
    }
}
