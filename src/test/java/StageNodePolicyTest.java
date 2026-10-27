import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class StageNodePolicyTest {
    @Test
    void skipCountsDroppedRecord() {
        RunSummary summary = summary();
        StageNode<String, String> node = node(alwaysFails(),
                ErrorPolicy.parse("SKIP"), summary, null);

        node.produce(ignored -> { });

        assertEquals(1, summary.droppedByStage().get("test"));
    }

    @Test
    void deadLetterKeepsFailedRecord() {
        RunSummary summary = summary();
        List<FailedRecord> failures = new ArrayList<>();
        StageNode<String, String> node = node(alwaysFails(),
                ErrorPolicy.parse("DEAD_LETTER"), summary, failures::add);

        node.produce(ignored -> { });

        assertEquals(1, failures.size());
        assertEquals("test", failures.get(0).stageName());
        assertEquals(1, summary.deadLetterRecords());
    }

    @Test
    void retrySucceedsOnThirdAttempt() {
        RunSummary summary = summary();
        AtomicInteger attempts = new AtomicInteger();
        Stage<String, String> stage = (input, out) -> {
            if (attempts.incrementAndGet() < 3) throw new StageException("temporary");
            out.emit(input);
        };
        List<String> output = new ArrayList<>();

        node(stage, ErrorPolicy.parse("RETRY(2)"), summary, null).produce(output::add);

        assertEquals(3, attempts.get());
        assertEquals(List.of("input"), output);
    }

    @Test
    void failFastBecomesPipelineException() {
        StageNode<String, String> node = node(alwaysFails(),
                ErrorPolicy.parse("FAIL_FAST"), summary(), null);
        assertThrows(PipelineException.class, () -> node.produce(ignored -> { }));
    }

    private static Stage<String, String> alwaysFails() {
        return (input, out) -> { throw new StageException("bad record"); };
    }

    private static RunSummary summary() {
        RunSummary summary = new RunSummary();
        summary.registerStage("test");
        return summary;
    }

    private static StageNode<String, String> node(Stage<String, String> stage,
            ErrorPolicy policy, RunSummary summary, Sink<FailedRecord> deadLetter) {
        SourceNode<String> source = new SourceNode<>(out -> out.emit("input"), summary);
        return new StageNode<>(source, stage, "test", policy, summary, deadLetter);
    }
}
