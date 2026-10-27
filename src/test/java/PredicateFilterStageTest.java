import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PredicateFilterStageTest {
    @Test
    void emitsOnlyMatchingRecords() {
        PredicateFilterStage filter = new PredicateFilterStage(record -> record.status() >= 400);
        List<LogRecord> output = new ArrayList<>();

        filter.process(record(200, "/api/ok"), output::add);
        filter.process(record(404, "/api/missing"), output::add);

        assertEquals(List.of(record(404, "/api/missing")), output);
    }

    @Test
    void supportsPathAndTimestampPredicates() {
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        PredicateFilterStage filter = new PredicateFilterStage(record ->
                record.path().startsWith("/api") && !record.timestamp().isBefore(start));
        List<LogRecord> output = new ArrayList<>();

        filter.process(record(200, "/public"), output::add);
        filter.process(record(200, "/api/users"), output::add);

        assertEquals(1, output.size());
        assertEquals("/api/users", output.get(0).path());
    }

    private static LogRecord record(int status, String path) {
        return new LogRecord(Instant.parse("2025-01-02T00:00:00Z"), "127.0.0.1", "GET",
                path, status, 12, "test", Map.of(), "raw");
    }
}
