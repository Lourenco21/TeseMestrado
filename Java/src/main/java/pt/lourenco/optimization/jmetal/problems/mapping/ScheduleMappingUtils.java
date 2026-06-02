package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;

import java.util.Map;

public final class ScheduleMappingUtils {

    private ScheduleMappingUtils() {
    }

    public static String getMappedColumn(Map<String, Object> mappingData, String canonicalField) {
        Map<String, Object> mapping = NestedMapUtils.getMap(mappingData, "mapping");
        return NestedMapUtils.getString(mapping, canonicalField);
    }

    public static String getClassValue(
            Map<String, Object> classRow,
            Map<String, Object> mappingData,
            String canonicalField
    ) {
        String columnName = getMappedColumn(mappingData, canonicalField);

        if (!NestedMapUtils.hasText(columnName) || classRow == null) {
            return null;
        }

        Object value = classRow.get(columnName);
        return value == null ? null : String.valueOf(value);
    }

    public static String getCourse(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "unidade_curricular");
    }

    public static String getClassType(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "tipo_aula");
    }

    public static String getTeacher(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "docente");
    }

    public static String getClassGroup(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "turma");
    }

    public static String getWeek(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "semana");
    }

    public static String getDay(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "dia");
    }

    public static String getStartTime(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "hora_inicio");
    }

    public static String getEndTime(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "hora_fim");
    }

    public static String getStudents(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "numero_estudantes");
    }

    public static String getRequestedRoomName(Map<String, Object> classRow, Map<String, Object> mappingData) {
        return getClassValue(classRow, mappingData, "sala");
    }

    public static String getRequestedRoomCharacteristics(
            Map<String, Object> classRow,
            Map<String, Object> mappingData
    ) {
        return getClassValue(classRow, mappingData, "caracteristicas_pedidas_para_sala");
    }
}