import java.nio.file.Path;

/** Validated command-line configuration for LogFlow. */
public record CliOptions(Path config) {
    public static CliOptions parse(String[] args) {
        if (args.length != 2 || !"--config".equals(args[0])) {
            throw new IllegalArgumentException("A YAML configuration file is required");
        }
        if (args[1].isBlank()) {
            throw new IllegalArgumentException("Configuration path must not be blank");
        }
        return new CliOptions(Path.of(args[1]));
    }

    public static String usage() {
        return "Usage: java -jar target/logflow-5.0.0.jar --config <file.yaml>";
    }
}
