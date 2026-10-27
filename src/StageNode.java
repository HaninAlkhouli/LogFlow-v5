import java.util.Objects;

final class StageNode<I, O> implements PipelineNode<O> {
    private final PipelineNode<I> previous;
    private final Stage<I, O> stage;
    private final String stageName;
    private final ErrorPolicy policy;
    private final RunSummary summary;
    private final Sink<FailedRecord> deadLetter;

    StageNode(PipelineNode<I> previous, Stage<I, O> stage, String stageName,
            ErrorPolicy policy, RunSummary summary, Sink<FailedRecord> deadLetter) {
        this.previous = Objects.requireNonNull(previous);
        this.stage = Objects.requireNonNull(stage);
        this.stageName = Objects.requireNonNull(stageName);
        this.policy = Objects.requireNonNull(policy);
        this.summary = Objects.requireNonNull(summary);
        this.deadLetter = deadLetter;
    }

    @Override
    public void open() {
        previous.open();
        try {
            stage.open();
        } catch (RuntimeException error) {
            previous.close();
            throw error;
        }
    }

    @Override
    public void produce(Emitter<O> out) {
        previous.produce(input -> process(input, out));
    }

    private void process(I input, Emitter<O> out) {
        int attempts = 0;
        while (true) {
            int[] emitted = {0};
            try {
                stage.process(input, item -> {
                    emitted[0]++;
                    out.emit(item);
                });
                if (emitted[0] == 0) summary.recordDropped(stageName);
                return;
            } catch (StageException error) {
                if (policy.action() == ErrorPolicy.Action.RETRY && attempts < policy.retries()) {
                    attempts++;
                    continue;
                }
                switch (policy.action()) {
                    case SKIP -> summary.recordDropped(stageName);
                    case DEAD_LETTER -> {
                        if (deadLetter == null) {
                            throw new PipelineException(
                                    "Stage '" + stageName + "' requires a dead-letter sink", error);
                        }
                        deadLetter.consume(FailedRecord.from(input, stageName, error));
                        summary.recordDeadLetter();
                    }
                    case FAIL_FAST, RETRY -> throw new PipelineException(
                            "Stage '" + stageName + "' failed: " + error.getMessage(), error);
                }
                return;
            }
        }
    }

    @Override
    public void close() {
        try {
            stage.close();
        } finally {
            previous.close();
        }
    }
}
