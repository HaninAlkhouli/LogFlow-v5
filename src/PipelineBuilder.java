import java.util.Objects;

/** Fluent, type-safe builder for Source -> Stage(s) -> Sink pipelines. */
public final class PipelineBuilder<T> {
    private final PipelineNode<T> output;
    private final RunSummary summary;

    private PipelineBuilder(PipelineNode<T> output, RunSummary summary) {
        this.output = output;
        this.summary = summary;
    }

    public static <T> PipelineBuilder<T> from(Source<T> source) {
        RunSummary summary = new RunSummary();
        return new PipelineBuilder<>(new SourceNode<>(Objects.requireNonNull(source), summary), summary);
    }

    public <R> PipelineBuilder<R> via(Stage<T, R> stage) {
        Stage<T, R> value = Objects.requireNonNull(stage);
        String name = value.getClass().getSimpleName();
        summary.registerStage(name);
        return new PipelineBuilder<>(new StageNode<>(output, value, name,
                ErrorPolicy.failFast(), summary, null), summary);
    }

    public Pipeline<T> to(Sink<T> sink) {
        return new Pipeline<>(output, Objects.requireNonNull(sink), null, summary);
    }
}
