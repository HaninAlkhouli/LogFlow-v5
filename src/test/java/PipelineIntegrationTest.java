import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void runsParserFilterAndProjectionIntoFile() throws Exception {
        Path input = directory.resolve("access.log");
        Path output = directory.resolve("errors.log");
        Files.write(input, List.of(line("/api/ok", 200), line("/api/missing", 404)));

        PipelineBuilder.from(new FileLineSource(input))
                .via(new ParserStage())
                .via(new PredicateFilterStage(record -> record.status() >= 400))
                .via(new ProjectionStage("clientIp", "status", "path"))
                .to(new FileSink(output))
                .run();

        assertEquals(List.of("clientIp=127.0.0.1 status=404 path=/api/missing"),
                Files.readAllLines(output));
    }

    private static String line(String path, int status) {
        return "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET " + path
                + " HTTP/1.0\" " + status + " 123 \"-\" \"Test Agent\"";
    }
}
