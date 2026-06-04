package pt.lourenco.optimization.utils;

public final class NumericParsingUtils {

    private NumericParsingUtils() {
    }

    public static Integer parseIntegerSafely(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim()
                .replace(" ", "")
                .replace(",", ".");

        if (!normalized.matches("[-+]?\\d+(\\.\\d+)?")) {
            return null;
        }

        try {
            return (int) Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
