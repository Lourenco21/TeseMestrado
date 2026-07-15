package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;
import pt.lourenco.optimization.utils.TextListParsingUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoomFeatureBitSetUtils {

    private static final String RESOLUTION_MAP_TO_ROOM_FEATURE = "map_to_room_feature";
    private static final String RESOLUTION_NONE_REQUIRED = "none_required";

    private RoomFeatureBitSetUtils() {
    }

    public static Map<String, Integer> buildFeatureIndex(
            List<Map<String, Object>> rawRooms,
            Map<String, Object> roomsMappingData,
            Map<String, Object> roomFeatureResolution
    ) {
        Set<String> allFeatures = new LinkedHashSet<>();

        if (rawRooms != null) {
            for (Map<String, Object> roomRow : rawRooms) {
                allFeatures.addAll(RoomsMappingUtils.extractRoomCharacteristics(roomRow, roomsMappingData));
            }
        }

        Object requestedValuesObject = roomFeatureResolution == null
                ? null
                : roomFeatureResolution.get("requested_values");

        if (requestedValuesObject instanceof List<?> requestedValues) {
            for (Object item : requestedValues) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> resolutionItem = (Map<String, Object>) rawMap;
                List<String> targetValues = NestedMapUtils.getStringList(resolutionItem, "target_values");

                for (String target : targetValues) {
                    String normalized = TextListParsingUtils.normalizeToken(target);
                    if (NestedMapUtils.hasText(normalized)) {
                        allFeatures.add(normalized);
                    }
                }
            }
        }

        Map<String, Integer> index = new LinkedHashMap<>();
        int current = 0;
        for (String feature : allFeatures) {
            index.put(feature, current++);
        }

        return Map.copyOf(index);
    }

    public static BitSet toBitSet(Set<String> features, Map<String, Integer> featureIndex) {
        BitSet bitSet = new BitSet();
        if (features == null || features.isEmpty() || featureIndex == null || featureIndex.isEmpty()) {
            return bitSet;
        }

        for (String feature : features) {
            Integer idx = featureIndex.get(feature);
            if (idx != null) {
                bitSet.set(idx);
            }
        }

        return bitSet;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, BitSet> buildRequirementBitSetCache(
            Map<String, Object> roomFeatureResolution,
            Map<String, Integer> featureIndex
    ) {
        Object requestedValuesObject = roomFeatureResolution == null
                ? null
                : roomFeatureResolution.get("requested_values");

        if (!(requestedValuesObject instanceof List<?> requestedValues)) {
            return Map.of();
        }

        Map<String, BitSet> cache = new LinkedHashMap<>();

        for (Object item : requestedValues) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> resolutionItem = (Map<String, Object>) rawMap;
            String sourceValue = NestedMapUtils.getString(resolutionItem, "source_value");
            String resolutionType = NestedMapUtils.getString(resolutionItem, "resolution_type");

            if (!NestedMapUtils.hasText(sourceValue)) {
                continue;
            }

            String normalizedSource = TextListParsingUtils.normalizeToken(sourceValue);

            if (RESOLUTION_NONE_REQUIRED.equalsIgnoreCase(resolutionType)) {
                cache.put(normalizedSource, new BitSet());
                continue;
            }

            BitSet requirementBitSet = new BitSet();

            if (RESOLUTION_MAP_TO_ROOM_FEATURE.equalsIgnoreCase(resolutionType)) {
                List<String> targetValues = NestedMapUtils.getStringList(resolutionItem, "target_values");
                for (String target : targetValues) {
                    String normalizedTarget = TextListParsingUtils.normalizeToken(target);
                    Integer idx = featureIndex.get(normalizedTarget);
                    if (idx != null) {
                        requirementBitSet.set(idx);
                    }
                }
            } else {
                Integer idx = featureIndex.get(normalizedSource);
                if (idx != null) {
                    requirementBitSet.set(idx);
                }
            }

            cache.put(normalizedSource, requirementBitSet);
        }

        return Map.copyOf(cache);
    }

    public static List<BitSet> extractRequestedRequirementBitSets(
            Map<String, Object> classRow,
            Map<String, Object> mappingData,
            Map<String, BitSet> requirementCache,
            Map<String, Integer> featureIndex
    ) {
        String rawValue = ScheduleMappingUtils.getRequestedRoomCharacteristics(classRow, mappingData);
        String separator = extractConfiguredSeparator(mappingData);
        List<String> requestedTokens = TextListParsingUtils.splitToNormalizedList(rawValue, separator);

        if (requestedTokens.isEmpty()) {
            return List.of();
        }

        List<BitSet> result = new ArrayList<>(requestedTokens.size());

        for (String token : requestedTokens) {
            if (!NestedMapUtils.hasText(token)) {
                continue;
            }

            BitSet cached = requirementCache.get(token);
            if (cached != null) {
                result.add((BitSet) cached.clone());
                continue;
            }

            Integer directIdx = featureIndex.get(token);
            if (directIdx != null) {
                BitSet direct = new BitSet();
                direct.set(directIdx);
                result.add(direct);
                continue;
            }

            System.out.println("WARN Unknown requested room characteristic token: " + token);
        }

        return List.copyOf(result);
    }

    public static Set<String> flattenBitSetRequirementsToDebugSet(
            List<BitSet> bitSets,
            Map<String, Integer> featureIndex
    ) {
        if (bitSets == null || bitSets.isEmpty()) {
            return Set.of();
        }

        Map<Integer, String> reverseIndex = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : featureIndex.entrySet()) {
            reverseIndex.put(entry.getValue(), entry.getKey());
        }

        Set<String> result = new LinkedHashSet<>();
        for (BitSet bitSet : bitSets) {
            if (bitSet == null) {
                continue;
            }

            for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                String feature = reverseIndex.get(i);
                if (feature != null) {
                    result.add(feature);
                }
            }
        }

        return Set.copyOf(result);
    }

    private static String extractConfiguredSeparator(Map<String, Object> mappingData) {
        Map<String, Object> mapping = NestedMapUtils.getMap(mappingData, "mapping");
        String separator = NestedMapUtils.getString(mapping, "caracteristicas_pedidas_separator");
        return NestedMapUtils.hasText(separator) ? separator : null;
    }
}