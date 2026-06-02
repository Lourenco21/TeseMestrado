package pt.lourenco.optimization.utils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
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
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static String buildSplitRegex(String configuredSeparator) {
        if (NestedMapUtils.hasText(configuredSeparator)) {
            return Pattern.quote(configuredSeparator);
        }

        return DEFAULT_MULTI_DELIMITER_REGEX;
    }
}
