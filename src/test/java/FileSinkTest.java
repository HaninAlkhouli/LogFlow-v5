import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSinkTest {
    @TempDir
    Path directory;

    @Test
    void writesRecordsBetweenOpenAndClose() throws Exception {
        Path output = directory.resolve("result.log");
        FileSink sink = new FileSink(output);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        fields.put("status", 404);
        fields.put("path", "/api/missing");

        sink.open();
        sink.consume(new ProjectedRecord(fields));
        sink.close();

        assertEquals("status=404 path=/api/missing" + System.lineSeparator(),
                Files.readString(output));
    }

    @Test
    void rejectsWritesBeforeOpen() {
        FileSink sink = new FileSink(directory.resolve("result.log"));
        assertThrows(IllegalStateException.class,
                () -> sink.consume(new ProjectedRecord(new LinkedHashMap<>())));
    }
}
