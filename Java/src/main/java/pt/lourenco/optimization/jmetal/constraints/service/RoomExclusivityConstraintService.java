package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class RoomExclusivityConstraintService {

    private final SchedulePartitionServiceAdapter scheduleAdapter = new SchedulePartitionServiceAdapter();

    public double calculateViolationScore(
            ProblemInputData inputData,
            List<ClassRoomAssignment> currentAssignments,
            PreviousPartitionAssignmentsContext previousContext
    ) {
        double violations = 0.0;

        List<ResolvedCurrentAssignment> resolvedCurrent = resolveCurrentAssignments(inputData, currentAssignments);

        for (int i = 0; i < resolvedCurrent.size(); i++) {
            ResolvedCurrentAssignment left = resolvedCurrent.get(i);

            for (int j = i + 1; j < resolvedCurrent.size(); j++) {
                ResolvedCurrentAssignment right = resolvedCurrent.get(j);

                if (sameRoom(left.roomIdentity(), right.roomIdentity()) &&
                        overlaps(left.start(), left.end(), right.start(), right.end())) {
                    violations += 1.0;
                }
            }

            if (previousContext != null) {
                for (PreviousPartitionAssignmentsContext.ResolvedAssignment previous : previousContext.getAssignments()) {
                    if (sameRoom(left.roomIdentity(), previous.getRoomIdentity()) &&
                            overlaps(left.start(), left.end(), previous.getStartDateTime(), previous.getEndDateTime())) {
                        violations += 1.0;
                    }
                }
            }
        }

        return violations;
    }

    private List<ResolvedCurrentAssignment> resolveCurrentAssignments(
            ProblemInputData inputData,
            List<ClassRoomAssignment> currentAssignments
    ) {
        List<ResolvedCurrentAssignment> resolved = new ArrayList<>();

        Map<String, Object> mappingData = inputData.getMappingData();
        Map<String, Object> roomsMappingData = inputData.getRoomsMappingData();

        for (ClassRoomAssignment assignment : currentAssignments) {
            Map<String, Object> classData = assignment.getClassData();
            Map<String, Object> roomData = assignment.getRoomData();

            String dayRaw = ScheduleMappingUtils.getDay(classData, mappingData);
            String startRaw = ScheduleMappingUtils.getStartTime(classData, mappingData);
            String endRaw = ScheduleMappingUtils.getEndTime(classData, mappingData);

            LocalDate day = scheduleAdapter.parseDateValue(dayRaw);
            LocalTime start = scheduleAdapter.coerceToLocalTime(startRaw);
            LocalTime end = scheduleAdapter.coerceToLocalTime(endRaw);

            if (day == null || start == null || end == null || !end.isAfter(start)) {
                continue;
            }

            String roomIdentity = RoomsMappingUtils.getRoomName(roomData, roomsMappingData);
            if (roomIdentity != null) {
                roomIdentity = roomIdentity.trim();
            }

            if (roomIdentity == null || roomIdentity.isBlank()) {
                roomIdentity = "room_index_" + assignment.getRoomIndex();
            }

            resolved.add(new ResolvedCurrentAssignment(
                    assignment,
                    roomIdentity,
                    LocalDateTime.of(day, start),
                    LocalDateTime.of(day, end)
            ));
        }

        return resolved;
    }

    private boolean sameRoom(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean overlaps(
            LocalDateTime start1,
            LocalDateTime end1,
            LocalDateTime start2,
            LocalDateTime end2
    ) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private record ResolvedCurrentAssignment(
            ClassRoomAssignment assignment,
            String roomIdentity,
            LocalDateTime start,
            LocalDateTime end
    ) { }

    static class SchedulePartitionServiceAdapter {

        public LocalDate parseDateValue(Object value) {
            return new pt.lourenco.optimization.jmetal.partitioning.SchedulePartitionService().parseDateValue(value);
        }

        public LocalTime coerceToLocalTime(Object value) {
            return new pt.lourenco.optimization.jmetal.partitioning.SchedulePartitionService().coerceToLocalTime(value);
        }
    }
}