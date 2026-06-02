package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;
import pt.lourenco.optimization.utils.TextListParsingUtils;

import java.util.Map;
import java.util.Set;

public final class RequestedRoomCharacteristicsUtils {

    private RequestedRoomCharacteristicsUtils() {
    }

    public static Set<String> extractRequestedCharacteristics(
            Map<String, Object> classRow,
            Map<String, Object> mappingData
    ) {
        String rawValue = ScheduleMappingUtils.getRequestedRoomCharacteristics(classRow, mappingData);
        String separator = extractConfiguredSeparator(mappingData);

        return TextListParsingUtils.splitToNormalizedSet(rawValue, separator);
    }

    private static String extractConfiguredSeparator(Map<String, Object> mappingData) {
        Map<String, Object> mapping = NestedMapUtils.getMap(mappingData, "mapping");
        String separator = NestedMapUtils.getString(mapping, "caracteristicas_pedidas_separator");

        if (NestedMapUtils.hasText(separator)) {
            return separator;
        }

        return null;
    }
}