package pt.lourenco.optimization.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class NestedMapUtils {

    private NestedMapUtils() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> source, String key) {
        if (source == null) {
            return Collections.emptyMap();
        }

        Object value = source.get(key);
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }

        return Collections.emptyMap();
    }

    public static String getString(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }

        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> source, String key) {
        if (source == null) {
            return List.of();
        }

        Object value = source.get(key);
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }

        return result;
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
