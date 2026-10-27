import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProjectionStageTest {
    @Test
    void keepsSelectedFieldsInRequestedOrder() {
        ProjectionStage projection = new ProjectionStage("status", "clientIp", "path");
        List<ProjectedRecord> output = new ArrayList<>();

        projection.process(record(), output::add);

        assertEquals(List.of("status", "clientIp", "path"),
                new ArrayList<>(output.get(0).fields().keySet()));
        assertEquals("status=404 clientIp=127.0.0.1 path=/api/users", output.get(0).toLine());
    }

    @Test
    void rejectsUnknownFields() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectionStage("unknown"));
    }

    private static LogRecord record() {
        return new LogRecord(Instant.parse("2025-01-02T00:00:00Z"), "127.0.0.1", "GET",
                "/api/users", 404, 12, "test", Map.of(), "raw");
    }
}
