import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ChaosPipelineIntegrationTest {
    @Test
    void accountsForEveryDirtyFixtureRecord() throws Exception {
        LoadedPipeline pipeline = PipelineLoader.standard()
                .load(Path.of("configs/chaos-test.yaml"));

        RunSummary summary = pipeline.run();

        assertEquals(11, summary.inputRecords());
        assertEquals(3, summary.outputRecords());
        assertEquals(8, summary.deadLetterRecords());
        assertEquals(0, summary.totalDropped());
        assertTrue(summary.reconciles());
        assertEquals(3, Files.readAllLines(pipeline.output()).size());
        assertEquals(8, Files.readAllLines(pipeline.deadLetterOutput()).size());
    }

    @Test
    void handlesEmptyInput() throws Exception {
        LoadedPipeline pipeline = PipelineLoader.standard()
                .load(Path.of("configs/empty-input.yaml"));

        RunSummary summary = pipeline.run();

        assertEquals(0, summary.inputRecords());
        assertEquals(0, summary.outputRecords());
        assertEquals(0, summary.deadLetterRecords());
        assertTrue(summary.reconciles());
    }
}
