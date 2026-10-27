import java.util.Map;

/** Creates one configured stage. */
@FunctionalInterface
public interface StageFactory {
    Stage<?, ?> create(Map<String, Object> params);
}
