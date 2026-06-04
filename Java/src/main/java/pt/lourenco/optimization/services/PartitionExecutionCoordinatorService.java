package pt.lourenco.optimization.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutionRegistry;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutor;
import pt.lourenco.optimization.jmetal.partitioning.*;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class PartitionExecutionCoordinatorService {

    private final SchedulePartitionService schedulePartitionService;
    private final PartitionSignatureService partitionSignatureService;
    private final AlgorithmExecutionRegistry algorithmExecutionRegistry;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public PartitionExecutionCoordinatorService(
            SchedulePartitionService schedulePartitionService,
            PartitionSignatureService partitionSignatureService,
            AlgorithmExecutionRegistry algorithmExecutionRegistry,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.schedulePartitionService = schedulePartitionService;
        this.partitionSignatureService = partitionSignatureService;
        this.algorithmExecutionRegistry = algorithmExecutionRegistry;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(
            ProblemInputData inputData,
            Map<String, Object> parameters,
            PartitionType partitionType,
            PartitionReuseStrategy reuseStrategy
    ) {

        AlgorithmExecutor executor =
                algorithmExecutionRegistry.getByName(inputData.getSelectedAlgorithm());

        List<PartitionedProblemInputData> partitions =
                schedulePartitionService.buildPartitions(inputData, partitionType);

        log.info("Total partitions created: {}", partitions.size());

        for (PartitionedProblemInputData partition : partitions) {
            log.info("Partition [{}] key='{}' classes={}",
                    partition.getPartitionOrder(),
                    partition.getPartitionKey(),
                    partition.getClassesInPartition() == null ? 0 : partition.getClassesInPartition().size());
        }

        PreviousPartitionAssignmentsContext previousAssignmentsContext =
                new PreviousPartitionAssignmentsContext();

        Map<String, Map<String, Object>> executionResultBySignature = new LinkedHashMap<>();
        List<Map<String, Object>> partitionResults = new ArrayList<>();

        for (PartitionedProblemInputData partition : partitions) {

            injectPreviousAssignmentsContext(partition.getInputData(), previousAssignmentsContext);

            String partitionSignature = partitionSignatureService.buildSignature(partition);

            boolean reused = false;
            String reusedFromSignature = null;
            Map<String, Object> executionResult;

            if (reuseStrategy == PartitionReuseStrategy.REUSE_EQUAL_PARTITION_SOLUTION
                    && executionResultBySignature.containsKey(partitionSignature)) {

                log.info("Reusing equivalent partition solution for '{}'", partition.getPartitionKey());
                executionResult = deepCopyMap(executionResultBySignature.get(partitionSignature));
                executionResult.put("reusedFromEquivalentPartition", true);
                executionResult.put("reusedFromSignature", partitionSignature);
                reused = true;
                reusedFromSignature = partitionSignature;

            } else {
                executionResult = executor.run(partition.getInputData(), parameters);
                executionResultBySignature.put(partitionSignature, deepCopyMap(executionResult));

                logPartitionSummary(partition, executionResult);
            }

            Object solutionsObject = executionResult.get("solutions");
            int solutionCount = (solutionsObject instanceof List<?> list) ? list.size() : 0;

            List<PreviousPartitionAssignmentsContext.ResolvedAssignment> resolvedAssignments =
                    resolveAssignmentsFromExecution(executionResult, partition);

            previousAssignmentsContext.addAll(resolvedAssignments);

            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("partitionKey", partition.getPartitionKey());
            wrapped.put("partitionType", partition.getPartitionType().name());
            wrapped.put("partitionOrder", partition.getPartitionOrder());
            wrapped.put("classCount", partition.getClassesInPartition().size());
            wrapped.put("partitionSignature", partitionSignature);
            wrapped.put("reused", reused);
            wrapped.put("reusedFromSignature", reusedFromSignature);
            wrapped.put("resolvedAssignmentCount", resolvedAssignments.size());
            wrapped.put("executionResult", executionResult);

            partitionResults.add(wrapped);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partitionType", partitionType.name());
        response.put("reuseStrategy", reuseStrategy.name());
        response.put("partitionCount", partitionResults.size());
        response.put("partitions", partitionResults);
        return response;
    }

    private void injectPreviousAssignmentsContext(
            ProblemInputData inputData,
            PreviousPartitionAssignmentsContext previousAssignmentsContext
    ) {
        Map<String, Object> metadata = inputData.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(inputData.getMetadata());

        metadata.put("previousPartitionAssignmentsContext", previousAssignmentsContext);
        inputData.setMetadata(metadata);
    }

    @SuppressWarnings("unchecked")
    private List<PreviousPartitionAssignmentsContext.ResolvedAssignment> resolveAssignmentsFromExecution(
            Map<String, Object> executionResult,
            PartitionedProblemInputData partition
    ) {
        Object solutionsObject = executionResult.get("solutions");
        if (!(solutionsObject instanceof List<?> solutions) || solutions.isEmpty()) {
            return List.of();
        }

        Object firstSolutionObject = solutions.get(0);
        if (!(firstSolutionObject instanceof Map<?, ?> firstSolution)) {
            return List.of();
        }

        Object assignmentsObject = firstSolution.get("assignments");
        if (!(assignmentsObject instanceof List<?> assignments)) {
            return List.of();
        }

        List<PreviousPartitionAssignmentsContext.ResolvedAssignment> resolved = new ArrayList<>();
        ProblemInputData inputData = partition.getInputData();

        for (Object assignmentObject : assignments) {
            if (!(assignmentObject instanceof Map<?, ?> assignment)) {
                continue;
            }

            Object classIndexObject = assignment.get("classIndex");
            Object roomIndexObject = assignment.get("roomIndex");
            Object classDataObject = assignment.get("classData");
            Object roomDataObject = assignment.get("roomData");

            if (!(classIndexObject instanceof Number classIndexNumber)) continue;
            if (!(roomIndexObject instanceof Number roomIndexNumber)) continue;
            if (!(classDataObject instanceof Map<?, ?> rawClassData)) continue;
            if (!(roomDataObject instanceof Map<?, ?> rawRoomData)) continue;

            Map<String, Object> classData = (Map<String, Object>) rawClassData;
            Map<String, Object> roomData = (Map<String, Object>) rawRoomData;

            LocalDate day = schedulePartitionService.parseDateValue(
                    classData.get(schedulePartitionService.getMappedColumn(inputData.getMappingData(), "dia"))
            );
            LocalTime start = schedulePartitionService.coerceToLocalTime(
                    classData.get(schedulePartitionService.getMappedColumn(inputData.getMappingData(), "hora_inicio"))
            );
            LocalTime end = schedulePartitionService.coerceToLocalTime(
                    classData.get(schedulePartitionService.getMappedColumn(inputData.getMappingData(), "hora_fim"))
            );

            if (day == null || start == null || end == null || !end.isAfter(start)) {
                continue;
            }

            ClassRoomAssignment rawAssignment = new ClassRoomAssignment(
                    classIndexNumber.intValue(),
                    classData,
                    roomIndexNumber.intValue(),
                    roomData
            );

            String roomIdentity = extractRoomIdentity(inputData, roomData, roomIndexNumber.intValue());

            resolved.add(new PreviousPartitionAssignmentsContext.ResolvedAssignment(
                    classIndexNumber.intValue(),
                    roomIndexNumber.intValue(),
                    rawAssignment,
                    LocalDateTime.of(day, start),
                    LocalDateTime.of(day, end),
                    partition.getPartitionKey(),
                    roomIdentity
            ));
        }

        return resolved;
    }

    private String extractRoomIdentity(
            ProblemInputData inputData,
            Map<String, Object> roomData,
            int fallbackRoomIndex
    ) {
        String mappedRoomColumn =
                schedulePartitionService.getMappedColumn(inputData.getRoomsMappingData(), "sala");

        if (mappedRoomColumn != null) {
            Object mappedValue = roomData.get(mappedRoomColumn);
            if (mappedValue != null && !mappedValue.toString().trim().isBlank()) {
                return mappedValue.toString().trim();
            }
        }

        Object directName = roomData.get("name");
        if (directName != null && !directName.toString().trim().isBlank()) {
            return directName.toString().trim();
        }

        return "room_index_" + fallbackRoomIndex;
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        try {
            return objectMapper.readValue(
                    objectMapper.writeValueAsString(source),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deep copy execution result.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void logPartitionSummary(
            PartitionedProblemInputData partition,
            Map<String, Object> executionResult
    ) {
        List<Map<String, Object>> solutions = asMapList(executionResult.get("solutions"));

        if (solutions.isEmpty()) {
            log.warn("Partition {} produced no solutions", partition.getPartitionKey());
            return;
        }

        Map<String, Object> bestSolution = solutions.get(0);
        String objectiveValue = extractFirstObjective(bestSolution);

        List<Map<String, Object>> assignments = asMapList(bestSolution.get("assignments"));

        log.info("===== Partition summary: {} =====", partition.getPartitionKey());
        log.info("Partition type: {}", partition.getPartitionType());
        log.info("Partition order: {}", partition.getPartitionOrder());
        log.info("Classes in partition: {}", assignments.size());
        log.info("Objective value: {}", objectiveValue);

        if (assignments.isEmpty()) {
            log.info("No assignments available for partition {}", partition.getPartitionKey());
            log.info("===== End partition summary: {} =====", partition.getPartitionKey());
            return;
        }

        List<Map<String, Object>> rooms = extractRooms(partition);

        for (Map<String, Object> assignment : assignments) {
            Integer classIndex = asInteger(assignment.get("classIndex"));
            Integer roomIndex = asInteger(assignment.get("roomIndex"));

            Map<String, Object> classData = asMap(assignment.get("classData"));
            Map<String, Object> roomData = getRoomByIndex(rooms, roomIndex);

            String course = ScheduleMappingUtils.getCourse(classData, partition.getInputData().getMappingData());
            String students = ScheduleMappingUtils.getStudents(classData, partition.getInputData().getMappingData());
            String start = ScheduleMappingUtils.getStartTime(classData, partition.getInputData().getMappingData());
            String requestedRoom = ScheduleMappingUtils.getRequestedRoomCharacteristics(
                    classData,
                    partition.getInputData().getMappingData()
            );

            String roomName = RoomsMappingUtils.getRoomName(roomData, partition.getInputData().getRoomsMappingData());
            String capacity = RoomsMappingUtils.getCapacity(roomData, partition.getInputData().getRoomsMappingData());
            String building = RoomsMappingUtils.getBuilding(roomData, partition.getInputData().getRoomsMappingData());

            log.info(
                    "Class [{}] {} | start={} | students={} | requestedRoom={} | roomIndex={} | room={} | building={} | capacity={}",
                    classIndex,
                    course,
                    start,
                    students,
                    requestedRoom,
                    roomIndex,
                    roomName,
                    building,
                    capacity
            );
        }

        log.info("===== End partition summary: {} =====", partition.getPartitionKey());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRooms(PartitionedProblemInputData partition) {
        Object roomsObject = partition.getInputData().getRoomsData().get("rooms");
        return roomsObject instanceof List<?> rawRooms ? (List<Map<String, Object>>) rawRooms : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        return value instanceof List<?> rawList ? (List<Map<String, Object>>) rawList : List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> rawMap ? (Map<String, Object>) rawMap : null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractFirstObjective(Map<String, Object> bestSolution) {
        List<?> objectives = bestSolution.get("objectives") instanceof List<?> raw ? raw : List.of();
        if (objectives.isEmpty()) {
            return null;
        }
        Object first = objectives.get(0);
        return first == null ? null : String.valueOf(first);
    }

    private Map<String, Object> getRoomByIndex(List<Map<String, Object>> rooms, Integer roomIndex) {
        if (roomIndex == null || roomIndex < 0 || roomIndex >= rooms.size()) {
            return null;
        }
        return rooms.get(roomIndex);
    }
}