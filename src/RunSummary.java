import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable counters for one pipeline run, exposed as an immutable snapshot after completion. */
public final class RunSummary {
    private final LinkedHashMap<String, Long> droppedByStage = new LinkedHashMap<>();
    private long inputRecords;
    private long outputRecords;
    private long deadLetterRecords;
    private long startedNanos;
    private long elapsedNanos;

    public void registerStage(String stageName) {
        droppedByStage.putIfAbsent(stageName, 0L);
    }

    void start() {
        startedNanos = System.nanoTime();
    }

    void finish() {
        elapsedNanos = System.nanoTime() - startedNanos;
    }

    void recordInput() {
        inputRecords++;
    }

    void recordOutput() {
        outputRecords++;
    }

    void recordDropped(String stageName) {
        droppedByStage.merge(stageName, 1L, Long::sum);
    }

    void recordDeadLetter() {
        deadLetterRecords++;
    }

    public long inputRecords() {
        return inputRecords;
    }

    public long outputRecords() {
        return outputRecords;
    }

    public long deadLetterRecords() {
        return deadLetterRecords;
    }

    public Map<String, Long> droppedByStage() {
        return Collections.unmodifiableMap(droppedByStage);
    }

    public long totalDropped() {
        return droppedByStage.values().stream().mapToLong(Long::longValue).sum();
    }

    public Duration elapsed() {
        return Duration.ofNanos(elapsedNanos);
    }

    public boolean reconciles() {
        return inputRecords == outputRecords + totalDropped() + deadLetterRecords;
    }

    public String format() {
        StringBuilder result = new StringBuilder("Run summary:\n")
                .append("  input records: ").append(inputRecords).append('\n')
                .append("  output records: ").append(outputRecords).append('\n');
        droppedByStage.forEach((stage, count) -> result
                .append("  dropped at ").append(stage).append(": ").append(count).append('\n'));
        return result.append("  dead-letter records: ").append(deadLetterRecords).append('\n')
                .append("  elapsed: ").append(elapsed().toMillis()).append(" ms\n")
                .append("  counts reconcile: ").append(reconciles())
                .toString();
    }
}
