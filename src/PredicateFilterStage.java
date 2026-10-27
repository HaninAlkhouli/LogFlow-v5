import java.util.Objects;
import java.util.function.Predicate;

/** Passes only log records that satisfy the configured predicate. */
public final class PredicateFilterStage implements Stage<LogRecord, LogRecord> {
    private final Predicate<LogRecord> predicate;

    public PredicateFilterStage(Predicate<LogRecord> predicate) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    @Override
    public void process(LogRecord record, Emitter<LogRecord> out) {
        if (predicate.test(record)) {
            out.emit(record);
        }
    }
}
