package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;
import pt.lourenco.optimization.jmetal.problems.mapping.RequestedRoomCharacteristicsUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.partitioning.SchedulePartitionService;
import pt.lourenco.optimization.utils.NumericParsingUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PreparedEvaluationDataBuilderService {

    private final SchedulePartitionService schedulePartitionService;

    public PreparedEvaluationDataBuilderService(SchedulePartitionService schedulePartitionService) {
        this.schedulePartitionService = schedulePartitionService;
    }

    @SuppressWarnings("unchecked")
    public PreparedEvaluationData build(ProblemInputData inputData) {
        List<Map<String, Object>> rawClasses =
                (List<Map<String, Object>>) inputData.getScheduleData().getOrDefault("classes", List.of());

        List<Map<String, Object>> rawRooms =
                (List<Map<String, Object>>) inputData.getRoomsData().getOrDefault("rooms", List.of());

        List<PreparedClassData> preparedClasses = new ArrayList<>();
        for (int i = 0; i < rawClasses.size(); i++) {
            Map<String, Object> classRow = rawClasses.get(i);

            LocalDate day = schedulePartitionService.parseDateValue(
                    ScheduleMappingUtils.getDay(classRow, inputData.getMappingData())
            );
            LocalTime start = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getStartTime(classRow, inputData.getMappingData())
            );
            LocalTime end = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getEndTime(classRow, inputData.getMappingData())
            );

            LocalDateTime startDateTime = (day != null && start != null) ? LocalDateTime.of(day, start) : null;
            LocalDateTime endDateTime = (day != null && end != null) ? LocalDateTime.of(day, end) : null;

            preparedClasses.add(new PreparedClassData(
                    i,
                    classRow,
                    ScheduleMappingUtils.getCourse(classRow, inputData.getMappingData()),
                    ScheduleMappingUtils.getClassType(classRow, inputData.getMappingData()),
                    ScheduleMappingUtils.getTeacher(classRow, inputData.getMappingData()),
                    ScheduleMappingUtils.getClassGroup(classRow, inputData.getMappingData()),
                    ScheduleMappingUtils.getWeek(classRow, inputData.getMappingData()),
                    day,
                    start,
                    end,
                    startDateTime,
                    endDateTime,
                    NumericParsingUtils.parseIntegerSafely(
                            ScheduleMappingUtils.getStudents(classRow, inputData.getMappingData())
                    ),
                    ScheduleMappingUtils.getRequestedRoomName(classRow, inputData.getMappingData()),
                    RequestedRoomCharacteristicsUtils.extractRequestedCharacteristics(
                            classRow,
                            inputData.getMappingData()
                    )
            ));
        }

        List<PreparedRoomData> preparedRooms = new ArrayList<>();
        for (int i = 0; i < rawRooms.size(); i++) {
            Map<String, Object> roomRow = rawRooms.get(i);

            String roomName = RoomsMappingUtils.getRoomName(roomRow, inputData.getRoomsMappingData());
            String roomIdentity = roomName != null && !roomName.trim().isBlank()
                    ? normalizeRoomIdentity(roomName)
                    : "room_index_" + i;

            preparedRooms.add(new PreparedRoomData(
                    i,
                    roomRow,
                    roomIdentity,
                    roomName,
                    RoomsMappingUtils.getBuilding(roomRow, inputData.getRoomsMappingData()),
                    NumericParsingUtils.parseIntegerSafely(
                            RoomsMappingUtils.getCapacity(roomRow, inputData.getRoomsMappingData())
                    ),
                    RoomsMappingUtils.extractRoomCharacteristics(roomRow, inputData.getRoomsMappingData())
            ));
        }

        PreviousPartitionAssignmentsContext previousContext = null;
        Object previous = inputData.getMetadata() == null
                ? null
                : inputData.getMetadata().get("previousPartitionAssignmentsContext");

        if (previous instanceof PreviousPartitionAssignmentsContext ctx) {
            previousContext = ctx;
        }

        Map<String, List<PreviousPartitionAssignmentsContext.ResolvedAssignment>> previousAssignmentsByRoom =
                previousContext == null
                        ? Map.of()
                        : previousContext.getAssignments().stream()
                        .collect(Collectors.groupingBy(a -> normalizeRoomIdentity(a.getRoomIdentity())));

        return new PreparedEvaluationData(
                Collections.unmodifiableList(preparedClasses),
                Collections.unmodifiableList(preparedRooms),
                Collections.unmodifiableMap(previousAssignmentsByRoom)
        );
    }

    private String normalizeRoomIdentity(String roomIdentity) {
        return roomIdentity == null ? "" : roomIdentity.trim().toLowerCase();
    }
}