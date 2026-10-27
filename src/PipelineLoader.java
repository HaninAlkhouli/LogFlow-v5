import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** Reads, validates and assembles a complete pipeline from YAML. */
public final class PipelineLoader {
    private final StageRegistry stages;
    private final Map<String, SourceRegistration> sources = new LinkedHashMap<>();
    private final Map<String, SinkRegistration> sinks = new LinkedHashMap<>();

    public PipelineLoader(StageRegistry stages) {
        this.stages = stages;
        sources.put("file", new SourceRegistration(RawLine.class, PipelineLoader::fileSource));
        sinks.put("file", new SinkRegistration(ProjectedRecord.class, PipelineLoader::fileSink));
    }

    public static PipelineLoader standard() {
        return new PipelineLoader(StageRegistry.standard());
    }

    public LoadedPipeline load(Path configFile) {
        Map<String, Object> root = readYaml(configFile);
        ConfigValues.allowOnly(root, "configuration",
                "pipeline", "source", "stages", "sink", "deadLetter");

        Map<String, Object> pipelineConfig =
                ConfigValues.requiredMap(root, "pipeline", "pipeline");
        ConfigValues.allowOnly(pipelineConfig, "pipeline", "name");
        String name = ConfigValues.requiredString(pipelineConfig, "name", "pipeline.name");

        RunSummary summary = new RunSummary();
        DeadLetterComponent deadLetter = deadLetter(root);
        Component source = source(root);
        PipelineNode<?> node = sourceNode(source.instance(), summary);
        Class<?> currentType = source.dataType();
        List<String> stageTypes = new ArrayList<>();

        List<Object> stageConfigs = ConfigValues.requiredList(root, "stages", "stages");
        if (stageConfigs.isEmpty()) {
            throw new ConfigException("stages must contain at least one stage");
        }

        for (int index = 0; index < stageConfigs.size(); index++) {
            String location = "stages[" + index + "]";
            Map<String, Object> stageConfig = ConfigValues.map(stageConfigs.get(index), location);
            ConfigValues.allowOnly(stageConfig, location, "type", "params", "onError");
            String type = ConfigValues.requiredString(stageConfig, "type", location + ".type");
            Map<String, Object> params =
                    ConfigValues.optionalMap(stageConfig, "params", location + ".params");
            ErrorPolicy policy = ErrorPolicy.parse(ConfigValues.optionalString(
                    stageConfig, "onError", location + ".onError", "FAIL_FAST"));
            if (policy.action() == ErrorPolicy.Action.DEAD_LETTER && deadLetter == null) {
                throw new ConfigException(location
                        + " uses DEAD_LETTER but configuration.deadLetter is missing");
            }

            StageRegistry.Registration registration = stages.require(type);
            if (!registration.inputType().isAssignableFrom(currentType)) {
                throw new ConfigException(location + " ('" + type + "') expects "
                        + registration.inputType().getSimpleName() + " but receives "
                        + currentType.getSimpleName());
            }

            Stage<?, ?> stage;
            try {
                stage = registration.create(params);
            } catch (ConfigException | IllegalArgumentException error) {
                throw new ConfigException(location + " ('" + type + "'): "
                        + error.getMessage(), error);
            }
            summary.registerStage(type);
            node = append(node, stage, type, policy, summary,
                    deadLetter == null ? null : deadLetter.instance());
            currentType = registration.outputType();
            stageTypes.add(type);
        }

        SinkComponent sink = sink(root);
        if (!sink.inputType().isAssignableFrom(currentType)) {
            throw new ConfigException("sink ('" + sink.type() + "') expects "
                    + sink.inputType().getSimpleName() + " but receives "
                    + currentType.getSimpleName());
        }

        return new LoadedPipeline(name, source.type(), stageTypes, sink.type(),
                pipeline(node, sink.instance(),
                        deadLetter == null ? null : deadLetter.instance(), summary),
                sink.output(), deadLetter == null ? null : deadLetter.output());
    }

    private Component source(Map<String, Object> root) {
        Map<String, Object> config = ConfigValues.requiredMap(root, "source", "source");
        ConfigValues.allowOnly(config, "source", "type", "params");
        String type = ConfigValues.requiredString(config, "type", "source.type");
        Map<String, Object> params = ConfigValues.optionalMap(config, "params", "source.params");
        SourceRegistration registration = sources.get(type);
        if (registration == null) throw new ConfigException("Unknown source type '" + type + "'");
        try {
            return new Component(type, registration.outputType(), registration.factory().apply(params));
        } catch (ConfigException error) {
            throw new ConfigException("source ('" + type + "'): " + error.getMessage(), error);
        }
    }

    private SinkComponent sink(Map<String, Object> root) {
        Map<String, Object> config = ConfigValues.requiredMap(root, "sink", "sink");
        ConfigValues.allowOnly(config, "sink", "type", "params");
        String type = ConfigValues.requiredString(config, "type", "sink.type");
        Map<String, Object> params = ConfigValues.optionalMap(config, "params", "sink.params");
        SinkRegistration registration = sinks.get(type);
        if (registration == null) throw new ConfigException("Unknown sink type '" + type + "'");
        try {
            FileSink sink = registration.factory().apply(params);
            return new SinkComponent(type, registration.inputType(), sink, sink.output());
        } catch (ConfigException error) {
            throw new ConfigException("sink ('" + type + "'): " + error.getMessage(), error);
        }
    }

    private DeadLetterComponent deadLetter(Map<String, Object> root) {
        if (!root.containsKey("deadLetter")) return null;
        Map<String, Object> config = ConfigValues.requiredMap(root, "deadLetter", "deadLetter");
        ConfigValues.allowOnly(config, "deadLetter", "type", "params");
        String type = ConfigValues.requiredString(config, "type", "deadLetter.type");
        if (!type.equals("file")) throw new ConfigException("Unknown deadLetter type '" + type + "'");
        Map<String, Object> params = ConfigValues.optionalMap(
                config, "params", "deadLetter.params");
        ConfigValues.allowOnly(params, "file deadLetter params", "path");
        Path output = Path.of(ConfigValues.requiredString(
                params, "path", "file deadLetter params.path"));
        return new DeadLetterComponent(new DeadLetterFileSink(output), output);
    }

    private static Source<?> fileSource(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "file source params", "path");
        return new FileLineSource(ConfigValues.requiredString(
                params, "path", "file source params.path"));
    }

    private static FileSink fileSink(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "file sink params", "path");
        return new FileSink(Path.of(ConfigValues.requiredString(
                params, "path", "file sink params.path")));
    }

    private static Map<String, Object> readYaml(Path file) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (InputStream input = Files.newInputStream(file)) {
            Object document = yaml.load(input);
            if (document == null) throw new ConfigException("Configuration file is empty");
            return ConfigValues.map(document, "configuration");
        } catch (IOException error) {
            throw new ConfigException("Could not read configuration file: " + file, error);
        } catch (YAMLException error) {
            throw new ConfigException("Invalid YAML in " + file + ": " + error.getMessage(), error);
        }
    }

    private static PipelineNode<?> sourceNode(Source<?> source, RunSummary summary) {
        return new SourceNode<>(source, summary);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PipelineNode<?> append(PipelineNode<?> previous, Stage<?, ?> stage,
            String name, ErrorPolicy policy, RunSummary summary, Sink<FailedRecord> deadLetter) {
        return new StageNode((PipelineNode) previous, (Stage) stage,
                name, policy, summary, deadLetter);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Pipeline<?> pipeline(PipelineNode<?> output, Sink<?> sink,
            Sink<FailedRecord> deadLetter, RunSummary summary) {
        return new Pipeline((PipelineNode) output, (Sink) sink, deadLetter, summary);
    }

    private record Component(String type, Class<?> dataType, Source<?> instance) {
    }

    private record SourceRegistration(
            Class<?> outputType, Function<Map<String, Object>, Source<?>> factory) {
    }

    private record SinkRegistration(
            Class<?> inputType, Function<Map<String, Object>, FileSink> factory) {
    }

    private record SinkComponent(
            String type, Class<?> inputType, FileSink instance, Path output) {
    }

    private record DeadLetterComponent(DeadLetterFileSink instance, Path output) {
    }
}
