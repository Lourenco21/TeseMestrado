package pt.lourenco.optimization.services;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleResponseBuilderService {

    public List<Map<String, Object>> buildUpdatedSchedule(
            List<Map<String, Object>> originalSchedule,
            Map<String, Object> mappingData,
            List<SolvedClassRoomAssignment> solvedAssignments
    ) {
        String mappedRoomColumn = ScheduleMappingUtils.getMappedColumn(mappingData, "sala");
        String roomColumnName = hasText(mappedRoomColumn) ? mappedRoomColumn : "sala";

        Map<String, String> assignedRoomsByClassKey = indexAssignedRooms(solvedAssignments);

        List<Map<String, Object>> updatedSchedule = new ArrayList<>();

        for (Map<String, Object> originalRow : originalSchedule) {
            Map<String, Object> updatedRow = new LinkedHashMap<>(originalRow);

            String classKey = buildClassKey(originalRow, mappingData);
            String assignedRoom = assignedRoomsByClassKey.get(classKey);

            updatedRow.put(roomColumnName, assignedRoom);
            updatedSchedule.add(updatedRow);
        }

        return updatedSchedule;
    }

    private Map<String, String> indexAssignedRooms(List<SolvedClassRoomAssignment> solvedAssignments) {
        Map<String, String> assignedRoomsByClassKey = new LinkedHashMap<>();

        for (SolvedClassRoomAssignment assignment : solvedAssignments) {
            assignedRoomsByClassKey.put(assignment.classKey(), assignment.roomName());
        }

        return assignedRoomsByClassKey;
    }

    public String buildClassKey(Map<String, Object> row, Map<String, Object> mappingData) {
        return normalize(ScheduleMappingUtils.getWeek(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getDay(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getStartTime(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getEndTime(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getCourse(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getClassGroup(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getShift(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getDegree(row, mappingData)) + "|"
                + normalize(ScheduleMappingUtils.getClassType(row, mappingData));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record SolvedClassRoomAssignment(
            String classKey,
            String roomName
    ) {
    }
}