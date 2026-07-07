package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;
import pt.lourenco.optimization.utils.TextListParsingUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResolvedRoomFeatureUtils {

    private static final String RESOLUTION_MAP_TO_ROOM_FEATURE = "map_to_room_feature";
    private static final String RESOLUTION_NONE_REQUIRED = "none_required";

    private ResolvedRoomFeatureUtils() {
    }

    public static List<Set<String>> extractResolvedRequestedCharacteristicGroups(
            Map<String, Object> classRow,
            Map<String, Object> mappingData,
            Map<String, Object> roomFeatureResolution
    ) {
        String rawValue = ScheduleMappingUtils.getRequestedRoomCharacteristics(classRow, mappingData);
        String separator = extractConfiguredSeparator(mappingData);
        List<String> requestedTokens = TextListParsingUtils.splitToNormalizedList(rawValue, separator);

        if (requestedTokens.isEmpty()) {
            return List.of();
        }

        Map<String, Map<String, Object>> resolutionLookup = buildResolutionLookup(roomFeatureResolution);
        List<Set<String>> groups = new ArrayList<>();

        for (String requestedToken : requestedTokens) {
            if (!NestedMapUtils.hasText(requestedToken)) {
                continue;
            }

            Map<String, Object> resolutionItem = resolutionLookup.get(requestedToken);

            if (resolutionItem == null) {
                groups.add(Set.of(requestedToken));
                continue;
            }

            String resolutionType = NestedMapUtils.getString(resolutionItem, "resolution_type");

            if (RESOLUTION_NONE_REQUIRED.equalsIgnoreCase(resolutionType)) {
                continue;
            }

            if (RESOLUTION_MAP_TO_ROOM_FEATURE.equalsIgnoreCase(resolutionType)) {
                List<String> targetValues = NestedMapUtils.getStringList(resolutionItem, "target_values");
                Set<String> normalizedTargets = new LinkedHashSet<>();

                for (String target : targetValues) {
                    if (NestedMapUtils.hasText(target)) {
                        normalizedTargets.add(normalize(target));
                    }
                }

                if (!normalizedTargets.isEmpty()) {
                    groups.add(normalizedTargets);
                    continue;
                }
            }

            groups.add(Set.of(requestedToken));
        }

        return List.copyOf(groups);
    }

    public static Set<String> flattenResolvedGroups(List<Set<String>> groups) {
        if (groups == null || groups.isEmpty()) {
            return Set.of();
        }

        Set<String> flattened = new LinkedHashSet<>();
        for (Set<String> group : groups) {
            if (group != null) {
                flattened.addAll(group);
            }
        }

        return Set.copyOf(flattened);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> buildResolutionLookup(
            Map<String, Object> roomFeatureResolution
    ) {
        Object rawRequestedValues = roomFeatureResolution == null
                ? null
                : roomFeatureResolution.get("requested_values");

        if (!(rawRequestedValues instanceof List<?> items)) {
            return Map.of();
        }

        Map<String, Map<String, Object>> lookup = new LinkedHashMap<>();

        for (Object item : items) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> resolutionItem = (Map<String, Object>) rawMap;
            String sourceValue = NestedMapUtils.getString(resolutionItem, "source_value");

            if (NestedMapUtils.hasText(sourceValue)) {
                lookup.put(normalize(sourceValue), resolutionItem);
            }
        }

        return lookup;
    }

    private static String extractConfiguredSeparator(Map<String, Object> mappingData) {
        Map<String, Object> mapping = NestedMapUtils.getMap(mappingData, "mapping");
        String separator = NestedMapUtils.getString(mapping, "caracteristicas_pedidas_separator");

        if (NestedMapUtils.hasText(separator)) {
            return separator;
        }

        return null;
    }

    private static String normalize(String value) {
        return TextListParsingUtils.normalizeToken(value);
    }
}