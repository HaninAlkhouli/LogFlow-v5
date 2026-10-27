import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small validation helpers for values read from YAML. */
final class ConfigValues {
    private ConfigValues() {
    }

    static Map<String, Object> map(Object value, String path) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new ConfigException(path + " must be a mapping");
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (!(key instanceof String name)) {
                throw new ConfigException(path + " contains a non-text key");
            }
            result.put(name, item);
        });
        return result;
    }

    static Map<String, Object> requiredMap(Map<String, Object> owner, String key, String path) {
        if (!owner.containsKey(key)) {
            throw new ConfigException(path + " is required");
        }
        return map(owner.get(key), path);
    }

    static Map<String, Object> optionalMap(Map<String, Object> owner, String key, String path) {
        return owner.containsKey(key) ? map(owner.get(key), path) : Map.of();
    }

    static List<Object> requiredList(Map<String, Object> owner, String key, String path) {
        if (!owner.containsKey(key)) {
            throw new ConfigException(path + " is required");
        }
        Object value = owner.get(key);
        if (!(value instanceof List<?> values)) {
            throw new ConfigException(path + " must be a list");
        }
        return new ArrayList<>(values);
    }

    static String requiredString(Map<String, Object> owner, String key, String path) {
        if (!owner.containsKey(key)) {
            throw new ConfigException(path + " is required");
        }
        Object value = owner.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ConfigException(path + " must be a non-empty string");
        }
        return text;
    }

    static String optionalString(
            Map<String, Object> owner, String key, String path, String defaultValue) {
        if (!owner.containsKey(key)) return defaultValue;
        return requiredString(owner, key, path);
    }

    static int requiredInt(Map<String, Object> owner, String key, String path) {
        if (!owner.containsKey(key)) {
            throw new ConfigException(path + " is required");
        }
        Object value = owner.get(key);
        if (!(value instanceof Integer number)) {
            throw new ConfigException(path + " must be an integer");
        }
        return number;
    }

    static Integer optionalInt(
            Map<String, Object> owner, String key, String path, Integer defaultValue) {
        if (!owner.containsKey(key)) return defaultValue;
        Object value = owner.get(key);
        if (!(value instanceof Integer number)) {
            throw new ConfigException(path + " must be an integer");
        }
        return number;
    }

    static Long optionalLong(
            Map<String, Object> owner, String key, String path, Long defaultValue) {
        if (!owner.containsKey(key)) return defaultValue;
        Object value = owner.get(key);
        if (!(value instanceof Number number)) {
            throw new ConfigException(path + " must be an integer");
        }
        return number.longValue();
    }

    static List<String> requiredStringList(
            Map<String, Object> owner, String key, String path) {
        List<Object> values = requiredList(owner, key, path);
        if (values.isEmpty()) {
            throw new ConfigException(path + " must not be empty");
        }
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new ConfigException(path + "[" + i + "] must be a non-empty string");
            }
            strings.add(text);
        }
        if (new LinkedHashSet<>(strings).size() != strings.size()) {
            throw new ConfigException(path + " must not contain duplicates");
        }
        return List.copyOf(strings);
    }

    static List<String> optionalStringList(
            Map<String, Object> owner, String key, String path) {
        if (!owner.containsKey(key)) return List.of();
        List<Object> values = requiredList(owner, key, path);
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new ConfigException(path + "[" + i + "] must be a non-empty string");
            }
            strings.add(text);
        }
        if (new LinkedHashSet<>(strings).size() != strings.size()) {
            throw new ConfigException(path + " must not contain duplicates");
        }
        return List.copyOf(strings);
    }

    static void allowOnly(Map<String, Object> values, String path, String... allowedKeys) {
        Set<String> allowed = Set.of(allowedKeys);
        values.keySet().stream()
                .filter(key -> !allowed.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new ConfigException(path + " contains unknown key '" + key + "'");
                });
    }
}
