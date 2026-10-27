import java.util.Objects;

/** A fully assembled, type-safe Source -> Stage(s) -> Sink pipeline. */
public final class Pipeline<T> {
    private final PipelineNode<T> output;
    private final Sink<T> sink;
    private final Sink<FailedRecord> deadLetter;
    private final RunSummary summary;

    Pipeline(PipelineNode<T> output, Sink<T> sink,
            Sink<FailedRecord> deadLetter, RunSummary summary) {
        this.output = Objects.requireNonNull(output);
        this.sink = Objects.requireNonNull(sink);
        this.deadLetter = deadLetter;
        this.summary = Objects.requireNonNull(summary);
    }

    public RunSummary run() {
        boolean outputOpened = false;
        boolean sinkOpened = false;
        boolean deadLetterOpened = false;
        summary.start();
        try {
            output.open();
            outputOpened = true;
            sink.open();
            sinkOpened = true;
            if (deadLetter != null) {
                deadLetter.open();
                deadLetterOpened = true;
            }
            output.produce(item -> {
                sink.consume(item);
                summary.recordOutput();
            });
        } catch (PipelineException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new PipelineException("Pipeline execution failed", error);
        } finally {
            try {
                try {
                    if (deadLetterOpened) deadLetter.close();
                } finally {
                    if (sinkOpened) sink.close();
                }
            } finally {
                try {
                    if (outputOpened) output.close();
                } finally {
                    summary.finish();
                }
            }
        }
        return summary;
    }
}
