import java.nio.file.Path;
import java.util.List;

/** A validated pipeline that is ready to execute. */
public record LoadedPipeline(
        String name,
        String sourceType,
        List<String> stageTypes,
        String sinkType,
        Pipeline<?> pipeline,
        Path output,
        Path deadLetterOutput) {
    public LoadedPipeline {
        stageTypes = List.copyOf(stageTypes);
    }

    public String executionPlan() {
        return sourceType + " -> " + String.join(" -> ", stageTypes) + " -> " + sinkType;
    }

    public RunSummary run() {
        return pipeline.run();
    }
}
