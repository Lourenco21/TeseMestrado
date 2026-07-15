package pt.lourenco.optimization.utils;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class TextListParsingUtils {

    private static final String DEFAULT_MULTI_DELIMITER_REGEX = "[,;]+";
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NON_ALNUM_UNDERSCORE_PATTERN = Pattern.compile("[^a-z0-9_]+");
    private static final Pattern MULTIPLE_UNDERSCORES_PATTERN = Pattern.compile("_+");

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

        String text = value.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return "";
        }

        text = Normalizer.normalize(text, Normalizer.Form.NFKD);
        text = DIACRITICS_PATTERN.matcher(text).replaceAll("");
        text = text.replace("-", "_").replace("/", "_");
        text = WHITESPACE_PATTERN.matcher(text).replaceAll("_");
        text = NON_ALNUM_UNDERSCORE_PATTERN.matcher(text).replaceAll("");
        text = MULTIPLE_UNDERSCORES_PATTERN.matcher(text).replaceAll("_");
        text = text.replaceAll("^_+|_+$", "");

        return text;
    }
}