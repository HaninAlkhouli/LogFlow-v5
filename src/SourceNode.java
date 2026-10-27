import java.util.Objects;

final class SourceNode<T> implements PipelineNode<T> {
    private final Source<T> source;
    private final RunSummary summary;

    SourceNode(Source<T> source, RunSummary summary) {
        this.source = Objects.requireNonNull(source);
        this.summary = Objects.requireNonNull(summary);
    }

    @Override
    public void open() {
    }

    @Override
    public void produce(Emitter<T> out) {
        source.produce(item -> {
            summary.recordInput();
            out.emit(item);
        });
    }

    @Override
    public void close() {
    }
}
