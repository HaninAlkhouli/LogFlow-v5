/** Reports an invalid pipeline configuration before data processing begins. */
public final class ConfigException extends PipelineException {
    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
