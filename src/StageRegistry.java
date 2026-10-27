import java.time.DateTimeException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Data-driven registry that maps stage names to typed factory functions. */
public final class StageRegistry {
    private final Map<String, Registration> registrations = new LinkedHashMap<>();

    public StageRegistry register(
            String type, Class<?> inputType, Class<?> outputType, StageFactory factory) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Stage type is required");
        Registration registration = new Registration(inputType, outputType, factory);
        if (registrations.putIfAbsent(type, registration) != null) {
            throw new IllegalArgumentException("Stage type is already registered: " + type);
        }
        return this;
    }

    public Registration require(String type) {
        Registration registration = registrations.get(type);
        if (registration == null) {
            throw new ConfigException("Unknown stage type '" + type
                    + "'. Registered stages: " + String.join(", ", registrations.keySet()));
        }
        return registration;
    }

    public static StageRegistry standard() {
        return new StageRegistry()
                .register("parse", RawLine.class, LogRecord.class, StageRegistry::parser)
                .register("validate", LogRecord.class, LogRecord.class, StageRegistry::validate)
                .register("filter", LogRecord.class, LogRecord.class, StageRegistry::filter)
                .register("project", LogRecord.class, ProjectedRecord.class, StageRegistry::project);
    }

    private static Stage<?, ?> parser(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "parse params", "format", "maxLineBytes");
        String format = ConfigValues.requiredString(params, "format", "parse params.format");
        if (!"clf".equals(format)) {
            throw new ConfigException("parse params.format must be 'clf'");
        }
        int maxLineBytes = ConfigValues.optionalInt(
                params, "maxLineBytes", "parse params.maxLineBytes", 65_536);
        return new ParserStage(maxLineBytes);
    }

    private static Stage<?, ?> validate(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "validate params", "requiredFields", "bytesRange",
                "statusRange", "timestampRange", "ipFormat");
        List<String> required = ConfigValues.optionalStringList(
                params, "requiredFields", "validate params.requiredFields");

        Map<String, Object> bytes = ConfigValues.optionalMap(
                params, "bytesRange", "validate params.bytesRange");
        ConfigValues.allowOnly(bytes, "validate params.bytesRange", "min", "max");
        Long minBytes = ConfigValues.optionalLong(
                bytes, "min", "validate params.bytesRange.min", null);
        Long maxBytes = ConfigValues.optionalLong(
                bytes, "max", "validate params.bytesRange.max", null);

        Map<String, Object> status = ConfigValues.optionalMap(
                params, "statusRange", "validate params.statusRange");
        ConfigValues.allowOnly(status, "validate params.statusRange", "min", "max");
        Integer minStatus = ConfigValues.optionalInt(
                status, "min", "validate params.statusRange.min", null);
        Integer maxStatus = ConfigValues.optionalInt(
                status, "max", "validate params.statusRange.max", null);

        Map<String, Object> timestamp = ConfigValues.optionalMap(
                params, "timestampRange", "validate params.timestampRange");
        ConfigValues.allowOnly(timestamp, "validate params.timestampRange", "min", "max");
        Instant minTimestamp = instant(timestamp, "min", "validate params.timestampRange.min");
        Instant maxTimestamp = instant(timestamp, "max", "validate params.timestampRange.max");

        String ipFormat = ConfigValues.optionalString(
                params, "ipFormat", "validate params.ipFormat", "none");
        if (!ipFormat.equals("none") && !ipFormat.equals("ipv4")) {
            throw new ConfigException("validate params.ipFormat must be 'none' or 'ipv4'");
        }
        return new ValidationStage(required, minBytes, maxBytes, minStatus, maxStatus,
                minTimestamp, maxTimestamp, ipFormat.equals("ipv4"));
    }

    private static Instant instant(Map<String, Object> values, String key, String path) {
        if (!values.containsKey(key)) return null;
        String value = ConfigValues.requiredString(values, key, path);
        try {
            return Instant.parse(value);
        } catch (DateTimeException error) {
            throw new ConfigException(path + " must be an ISO-8601 UTC timestamp", error);
        }
    }

    private static Stage<?, ?> filter(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "filter params", "field", "op", "value");
        String field = ConfigValues.requiredString(params, "field", "filter params.field");
        String operator = ConfigValues.requiredString(params, "op", "filter params.op");

        Predicate<LogRecord> predicate = switch (field) {
            case "status" -> statusPredicate(operator,
                    ConfigValues.requiredInt(params, "value", "filter params.value"));
            case "path" -> pathPredicate(operator,
                    ConfigValues.requiredString(params, "value", "filter params.value"));
            default -> throw new ConfigException(
                    "filter params.field must be 'status' or 'path'");
        };
        return new PredicateFilterStage(predicate);
    }

    private static Predicate<LogRecord> statusPredicate(String operator, int value) {
        if (value < 100 || value > 599) {
            throw new ConfigException("filter params.value for status must be between 100 and 599");
        }
        return switch (operator) {
            case ">=" -> record -> record.status() >= value;
            case ">" -> record -> record.status() > value;
            case "==" -> record -> record.status() == value;
            case "<=" -> record -> record.status() <= value;
            case "<" -> record -> record.status() < value;
            default -> throw new ConfigException(
                    "filter params.op for status must be one of: >=, >, ==, <=, <");
        };
    }

    private static Predicate<LogRecord> pathPredicate(String operator, String value) {
        return switch (operator) {
            case "startsWith" -> record -> record.path().startsWith(value);
            case "==" -> record -> record.path().equals(value);
            default -> throw new ConfigException(
                    "filter params.op for path must be 'startsWith' or '=='");
        };
    }

    private static Stage<?, ?> project(Map<String, Object> params) {
        ConfigValues.allowOnly(params, "project params", "fields");
        List<String> fields = ConfigValues.requiredStringList(
                params, "fields", "project params.fields");
        return new ProjectionStage(fields);
    }

    public record Registration(
            Class<?> inputType, Class<?> outputType, StageFactory factory) {
        public Registration {
            if (inputType == null || outputType == null || factory == null) {
                throw new IllegalArgumentException("Stage registration values must not be null");
            }
        }

        public Stage<?, ?> create(Map<String, Object> params) {
            return factory.create(params);
        }
    }
}
