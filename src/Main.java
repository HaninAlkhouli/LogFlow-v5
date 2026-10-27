public class Main {
    public static void main(String[] args) {
        try {
            run(CliOptions.parse(args));
        } catch (PipelineException | IllegalArgumentException error) {
            System.err.println("Error: " + error.getMessage());
            System.err.println(CliOptions.usage());
        }
    }

    static void run(CliOptions options) {
        LoadedPipeline loaded = PipelineLoader.standard().load(options.config());
        System.out.println("Pipeline loaded: " + loaded.name());
        System.out.println("Execution plan: " + loaded.executionPlan());
        RunSummary summary = loaded.run();
        System.out.println("Pipeline completed: " + loaded.name());
        System.out.println("Output written to: " + loaded.output().toAbsolutePath());
        if (loaded.deadLetterOutput() != null) {
            System.out.println("Dead letter written to: "
                    + loaded.deadLetterOutput().toAbsolutePath());
        }
        System.out.println(summary.format());
    }
}
