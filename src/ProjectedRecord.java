import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Immutable ordered subset of a LogRecord. */
public record ProjectedRecord(Map<String, Object> fields) implements Record {
    public ProjectedRecord {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public String toLine() {
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return toLine();
    }
}
