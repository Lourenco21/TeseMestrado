package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;
import pt.lourenco.optimization.utils.TextListParsingUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoomsMappingUtils {

    private static final String FORMAT_SINGLE_COLUMN_LIST = "single_column_list";
    private static final String FORMAT_MULTIPLE_COLUMNS = "multiple_columns";
    private static final String FORMAT_RANGE_COLUMNS = "range_columns";

    private RoomsMappingUtils() {
    }

    public static String getFieldMapping(Map<String, Object> roomsMappingData, String canonicalField) {
        Map<String, Object> fieldMappings = NestedMapUtils.getMap(roomsMappingData, "field_mappings");
        return NestedMapUtils.getString(fieldMappings, canonicalField);
    }

    public static String getRoomValue(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData,
            String canonicalField
    ) {
        String columnName = getFieldMapping(roomsMappingData, canonicalField);

        if (!NestedMapUtils.hasText(columnName) || roomRow == null) {
            return null;
        }

        Object value = roomRow.get(columnName);
        return value == null ? null : String.valueOf(value);
    }

    public static String getRoomName(Map<String, Object> roomRow, Map<String, Object> roomsMappingData) {
        return getRoomValue(roomRow, roomsMappingData, "room_name");
    }

    public static String getBuilding(Map<String, Object> roomRow, Map<String, Object> roomsMappingData) {
        return getRoomValue(roomRow, roomsMappingData, "building");
    }

    public static String getCapacity(Map<String, Object> roomRow, Map<String, Object> roomsMappingData) {
        return getRoomValue(roomRow, roomsMappingData, "capacity");
    }

    public static String getRoomsFileRoomColumn(Map<String, Object> roomsMappingData) {
        Map<String, Object> linking = NestedMapUtils.getMap(roomsMappingData, "linking");
        return NestedMapUtils.getString(linking, "rooms_file_room_column");
    }

    public static String getScheduleRoomColumn(Map<String, Object> roomsMappingData) {
        Map<String, Object> linking = NestedMapUtils.getMap(roomsMappingData, "linking");
        return NestedMapUtils.getString(linking, "schedule_room_column");
    }

    public static String getCharacteristicsFormat(Map<String, Object> roomsMappingData) {
        Map<String, Object> characteristics = NestedMapUtils.getMap(roomsMappingData, "characteristics");
        return NestedMapUtils.getString(characteristics, "format");
    }

    public static Set<String> extractRoomCharacteristics(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        String format = getCharacteristicsFormat(roomsMappingData);

        if (FORMAT_SINGLE_COLUMN_LIST.equalsIgnoreCase(format)) {
            return extractSingleColumnListCharacteristics(roomRow, roomsMappingData);
        }

        if (FORMAT_MULTIPLE_COLUMNS.equalsIgnoreCase(format)) {
            return extractMultipleColumnsCharacteristics(roomRow, roomsMappingData);
        }

        if (FORMAT_RANGE_COLUMNS.equalsIgnoreCase(format)) {
            return extractRangeColumnsCharacteristics(roomRow, roomsMappingData);
        }

        return Set.of();
    }

    private static Set<String> extractSingleColumnListCharacteristics(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        Map<String, Object> config = getCharacteristicsConfig(roomsMappingData);

        String sourceColumn = NestedMapUtils.getString(config, "source_column");
        String separator = NestedMapUtils.getString(config, "separator");

        if (!NestedMapUtils.hasText(sourceColumn) || roomRow == null) {
            return Set.of();
        }

        Object rawValue = roomRow.get(sourceColumn);
        if (rawValue == null) {
            return Set.of();
        }

        return TextListParsingUtils.splitToNormalizedSet(String.valueOf(rawValue), separator);
    }

    private static Set<String> extractMultipleColumnsCharacteristics(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        Map<String, Object> config = getCharacteristicsConfig(roomsMappingData);
        List<String> selectedColumns = NestedMapUtils.getStringList(config, "selected_columns");
        List<String> selectedValues = NestedMapUtils.getStringList(config, "selected_values");

        if (roomRow == null || selectedColumns.isEmpty() || selectedValues.isEmpty()) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>();

        for (String columnName : selectedColumns) {
            Object rawValue = roomRow.get(columnName);
            String cellValue = rawValue == null ? null : String.valueOf(rawValue);

            if (matchesSelectedValue(cellValue, selectedValues)) {
                String normalized = TextListParsingUtils.normalizeToken(columnName);
                if (NestedMapUtils.hasText(normalized)) {
                    result.add(normalized);
                }
            }
        }

        return result;
    }

    private static Set<String> extractRangeColumnsCharacteristics(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        Map<String, Object> config = getCharacteristicsConfig(roomsMappingData);

        String startColumn = NestedMapUtils.getString(config, "start_column");
        String endColumn = NestedMapUtils.getString(config, "end_column");
        List<String> selectedValues = NestedMapUtils.getStringList(config, "selected_values");

        if (!NestedMapUtils.hasText(startColumn) || !NestedMapUtils.hasText(endColumn)) {
            return Set.of();
        }

        if (roomRow == null || selectedValues.isEmpty()) {
            return Set.of();
        }

        Set<String> result = new LinkedHashSet<>();
        boolean insideRange = false;

        for (Map.Entry<String, Object> entry : roomRow.entrySet()) {
            String columnName = entry.getKey();

            if (columnName.equals(startColumn)) {
                insideRange = true;
            }

            if (insideRange) {
                String cellValue = entry.getValue() == null ? null : String.valueOf(entry.getValue());
                if (matchesSelectedValue(cellValue, selectedValues)) {
                    String normalized = TextListParsingUtils.normalizeToken(columnName);
                    if (NestedMapUtils.hasText(normalized)) {
                        result.add(normalized);
                    }
                }
            }

            if (columnName.equals(endColumn)) {
                break;
            }
        }

        return result;
    }

    private static Map<String, Object> getCharacteristicsConfig(Map<String, Object> roomsMappingData) {
        Map<String, Object> characteristics = NestedMapUtils.getMap(roomsMappingData, "characteristics");
        return NestedMapUtils.getMap(characteristics, "config");
    }

    private static boolean matchesSelectedValue(String cellValue, List<String> selectedValues) {
        if (cellValue == null || selectedValues == null || selectedValues.isEmpty()) {
            return false;
        }

        String normalizedCellValue = cellValue.trim();

        for (String selectedValue : selectedValues) {
            if (selectedValue != null && normalizedCellValue.equalsIgnoreCase(selectedValue.trim())) {
                return true;
            }
        }

        return false;
    }
}