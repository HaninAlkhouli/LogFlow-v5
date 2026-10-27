import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PipelineLoaderTest {
    @TempDir
    Path directory;

    private final PipelineLoader loader = PipelineLoader.standard();

    @Test
    void loadsValidConfiguration() throws IOException {
        Path config = write("""
                pipeline: { name: valid }
                source: { type: file, params: { path: input.log } }
                stages:
                  - { type: parse, params: { format: clf } }
                  - { type: project, params: { fields: [status, path] } }
                sink: { type: file, params: { path: output.log } }
                """);

        LoadedPipeline loaded = assertDoesNotThrow(() -> loader.load(config));

        assertEquals("file -> parse -> project -> file", loaded.executionPlan());
        assertFalse(loaded.stageTypes().contains("filter"));
    }

    @Test
    void rejectsMissingStagesSection() throws IOException {
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: missing-stages }
                source: { type: file, params: { path: input.log } }
                sink: { type: file, params: { path: output.log } }
                """)));

        assertTrue(error.getMessage().contains("stages is required"));
    }

    @Test
    void rejectsUnknownStageType() throws IOException {
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: unknown }
                source: { type: file, params: { path: input.log } }
                stages:
                  - { type: missing, params: {} }
                sink: { type: file, params: { path: output.log } }
                """)));

        assertTrue(error.getMessage().contains("Unknown stage type 'missing'"));
    }

    @Test
    void rejectsMissingParameterBeforeCreatingOutput() throws IOException {
        Path output = directory.resolve("must-not-exist.log");
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: missing-parameter }
                source: { type: file, params: { path: input.log } }
                stages:
                  - { type: parse, params: {} }
                sink: { type: file, params: { path: %s } }
                """.formatted(output.toString().replace('\\', '/')))));

        assertTrue(error.getMessage().contains("format"));
        assertFalse(Files.exists(output));
    }

    @Test
    void rejectsParameterWithWrongType() throws IOException {
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: wrong-type }
                source: { type: file, params: { path: input.log } }
                stages:
                  - { type: parse, params: { format: clf } }
                  - type: filter
                    params: { field: status, op: ">=", value: four-hundred }
                sink: { type: file, params: { path: output.log } }
                """)));

        assertTrue(error.getMessage().contains("must be an integer"));
    }

    @Test
    void rejectsEmptyPipeline() throws IOException {
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: empty }
                source: { type: file, params: { path: input.log } }
                stages: []
                sink: { type: file, params: { path: output.log } }
                """)));

        assertTrue(error.getMessage().contains("at least one stage"));
    }

    @Test
    void rejectsDeadLetterPolicyWithoutDeadLetterSink() throws IOException {
        ConfigException error = assertThrows(ConfigException.class, () -> loader.load(write("""
                pipeline: { name: missing-dead-letter }
                source: { type: file, params: { path: input.log } }
                stages:
                  - { type: parse, onError: DEAD_LETTER, params: { format: clf } }
                  - { type: project, params: { fields: [status] } }
                sink: { type: file, params: { path: output.log } }
                """)));

        assertTrue(error.getMessage().contains("deadLetter is missing"));
    }

    private Path write(String yaml) throws IOException {
        Path file = directory.resolve("pipeline-" + System.nanoTime() + ".yaml");
        Files.writeString(file, yaml);
        return file;
    }
}
