package pt.lourenco.optimization.utils;

import java.util.*;
import java.util.regex.Pattern;

public final class TextListParsingUtils {

    private static final String DEFAULT_MULTI_DELIMITER_REGEX = "[,;|/]+";

    private TextListParsingUtils() {
    }

    public static Set<String> splitToNormalizedSet(String rawValue, String configuredSeparator) {
        if (!NestedMapUtils.hasText(rawValue)) {
            return Set.of();
        }

        String regex = buildSplitRegex(configuredSeparator);

        return Arrays.stream(rawValue.split(regex))
                .map(TextListParsingUtils::normalizeToken)
                .filter(NestedMapUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public static List<String> splitToNormalizedList(String rawValue, String configuredSeparator) {
        if (!NestedMapUtils.hasText(rawValue)) {
            return List.of();
        }

        String regex = buildSplitRegex(configuredSeparator);

        return Arrays.stream(rawValue.split(regex))
                .map(TextListParsingUtils::normalizeToken)
                .filter(NestedMapUtils::hasText)
                .toList();
    }

    private static String buildSplitRegex(String configuredSeparator) {
        if (NestedMapUtils.hasText(configuredSeparator)) {
            return Pattern.quote(configuredSeparator);
        }

        return DEFAULT_MULTI_DELIMITER_REGEX;
    }

    public static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }
}