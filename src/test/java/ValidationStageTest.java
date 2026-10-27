import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ValidationStageTest {
    private final ValidationStage validator = new ValidationStage(
            List.of("clientIp", "method", "path"), 0L, null, 100, 599,
            Instant.parse("2020-01-01T00:00:00Z"),
            Instant.parse("2030-12-31T23:59:59Z"), true);

    @Test
    void emitsValidRecord() throws StageException {
        List<LogRecord> output = new ArrayList<>();
        validator.process(record(12, "127.0.0.1", Instant.parse("2025-01-01T00:00:00Z")),
                output::add);
        assertEquals(1, output.size());
    }

    @Test
    void rejectsNegativeBytesOldTimestampAndBadIp() {
        assertThrows(StageException.class, () -> validator.process(
                record(-1, "127.0.0.1", Instant.parse("2025-01-01T00:00:00Z")), ignored -> { }));
        assertThrows(StageException.class, () -> validator.process(
                record(1, "127.0.0.1", Instant.parse("1970-01-01T00:00:00Z")), ignored -> { }));
        assertThrows(StageException.class, () -> validator.process(
                record(1, "999.1.1.1", Instant.parse("2025-01-01T00:00:00Z")), ignored -> { }));
    }

    private static LogRecord record(long bytes, String ip, Instant time) {
        return new LogRecord(time, ip, "GET", "/", 200, bytes, "", Map.of(), "raw");
    }
}
