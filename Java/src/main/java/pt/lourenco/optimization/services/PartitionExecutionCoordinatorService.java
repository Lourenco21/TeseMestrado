package pt.lourenco.optimization.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutionRegistry;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutor;
import pt.lourenco.optimization.jmetal.partitioning.PartitionReuseStrategy;
import pt.lourenco.optimization.jmetal.partitioning.PartitionSignatureService;
import pt.lourenco.optimization.jmetal.partitioning.PartitionType;
import pt.lourenco.optimization.jmetal.partitioning.PartitionedProblemInputData;
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;
import pt.lourenco.optimization.jmetal.partitioning.SchedulePartitionService;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        long executionStartNs = System.nanoTime();

        AlgorithmExecutor executor =
                algorithmExecutionRegistry.getByName(inputData.getSelectedAlgorithm());

        List<PartitionedProblemInputData> partitions =
                schedulePartitionService.buildPartitions(inputData, partitionType);

        log.info("Total partitions created: {}", partitions.size());

        PreviousPartitionAssignmentsContext previousAssignmentsContext =
                new PreviousPartitionAssignmentsContext();

        Map<String, Map<String, Object>> executionResultBySignature = new LinkedHashMap<>();
        List<Map<String, Object>> partitionResults = new ArrayList<>();

        long totalPartitionExecutionMs = 0L;
        int reusedPartitions = 0;

        for (PartitionedProblemInputData partition : partitions) {

            long partitionStartNs = System.nanoTime();

            LocalDateTime partitionStart = extractPartitionStart(partition);
            previousAssignmentsContext.removeEndedBefore(partitionStart);

            injectPreviousAssignmentsContext(partition.getInputData(), previousAssignmentsContext);

            String partitionSignature = partitionSignatureService.buildSignature(partition);

            boolean reused = false;
            String reusedFromSignature = null;
            Map<String, Object> executionResult;

            int classCount = partition.getClassesInPartition() == null
                    ? 0
                    : partition.getClassesInPartition().size();

            if (reuseStrategy == PartitionReuseStrategy.REUSE_EQUAL_PARTITION_SOLUTION
                    && executionResultBySignature.containsKey(partitionSignature)) {

                log.info("Reusing equivalent partition solution for '{}'", partition.getPartitionKey());
                executionResult = deepCopyMap(executionResultBySignature.get(partitionSignature));
                executionResult.put("reusedFromEquivalentPartition", true);
                executionResult.put("reusedFromSignature", partitionSignature);
                reused = true;
                reusedFromSignature = partitionSignature;
                reusedPartitions++;

            } else {
                executionResult = executor.run(partition.getInputData(), parameters);
                executionResultBySignature.put(partitionSignature, deepCopyMap(executionResult));
            }

            Object solutionsObject = executionResult.get("solutions");
            int solutionCount = (solutionsObject instanceof List<?> list) ? list.size() : 0;

            List<PreviousPartitionAssignmentsContext.ResolvedAssignment> resolvedAssignments =
                    resolveAssignmentsFromExecution(executionResult, partition);

            previousAssignmentsContext.addAll(resolvedAssignments);

            long partitionDurationMs = (System.nanoTime() - partitionStartNs) / 1_000_000L;
            totalPartitionExecutionMs += partitionDurationMs;

            Double objectiveValue = extractObjectiveValue(executionResult);
            Integer evaluationsUsed = extractEvaluationsUsed(executionResult);
            Double avgEvaluationMs = extractAvgEvaluationMs(executionResult);
            Long totalEvaluateMs = extractTotalEvaluateMs(executionResult);
            Long buildAssignmentsMs = extractBuildAssignmentsMs(executionResult);
            Long constraintEvalMs = extractConstraintEvalMs(executionResult);
            Integer hardViolations = extractHardViolationsCount(executionResult);

            log.info(
                    "Partition finished | type={} order={} key={} classes={} reused={} durationMs={} solutions={} resolvedAssignments={} objective={} evaluations={} evalAvgMs={} evalTotalMs={} buildAssignmentsMs={} constraintEvalMs={} hardViolations={}",
                    partition.getPartitionType().name(),
                    partition.getPartitionOrder(),
                    partition.getPartitionKey(),
                    classCount,
                    reused,
                    partitionDurationMs,
                    solutionCount,
                    resolvedAssignments.size(),
                    objectiveValue,
                    evaluationsUsed,
                    avgEvaluationMs,
                    totalEvaluateMs,
                    buildAssignmentsMs,
                    constraintEvalMs,
                    hardViolations
            );

            Map<String, Object> wrapped = new LinkedHashMap<>();
            wrapped.put("partitionKey", partition.getPartitionKey());
            wrapped.put("partitionType", partition.getPartitionType().name());
            wrapped.put("partitionOrder", partition.getPartitionOrder());
            wrapped.put("classCount", classCount);
            wrapped.put("partitionSignature", partitionSignature);
            wrapped.put("reused", reused);
            wrapped.put("reusedFromSignature", reusedFromSignature);
            wrapped.put("resolvedAssignmentCount", resolvedAssignments.size());
            wrapped.put("partitionDurationMs", partitionDurationMs);
            wrapped.put("solutionCount", solutionCount);
            wrapped.put("objectiveValue", objectiveValue);
            wrapped.put("evaluationsUsed", evaluationsUsed);
            wrapped.put("avgEvaluationMs", avgEvaluationMs);
            wrapped.put("totalEvaluateMs", totalEvaluateMs);
            wrapped.put("buildAssignmentsMs", buildAssignmentsMs);
            wrapped.put("constraintEvalMs", constraintEvalMs);
            wrapped.put("hardViolations", hardViolations);
            wrapped.put("executionResult", executionResult);

            partitionResults.add(wrapped);
        }

        long totalExecutionMs = (System.nanoTime() - executionStartNs) / 1_000_000L;

        log.info(
                "Partition execution completed | type={} reuseStrategy={} partitions={} reusedPartitions={} totalDurationMs={} avgPartitionDurationMs={}",
                partitionType.name(),
                reuseStrategy.name(),
                partitionResults.size(),
                reusedPartitions,
                totalExecutionMs,
                partitionResults.isEmpty() ? 0.0 : (double) totalPartitionExecutionMs / partitionResults.size()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partitionType", partitionType.name());
        response.put("reuseStrategy", reuseStrategy.name());
        response.put("partitionCount", partitionResults.size());
        response.put("reusedPartitions", reusedPartitions);
        response.put("totalExecutionMs", totalExecutionMs);
        response.put("averagePartitionDurationMs",
                partitionResults.isEmpty() ? 0.0 : (double) totalPartitionExecutionMs / partitionResults.size());
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
                    ScheduleMappingUtils.getDay(classData, inputData.getMappingData())
            );
            LocalTime start = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getStartTime(classData, inputData.getMappingData())
            );
            LocalTime end = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getEndTime(classData, inputData.getMappingData())
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

            LocalDateTime startDateTime = LocalDateTime.of(day, start);
            LocalDateTime endDateTime = LocalDateTime.of(day, end);

            String uniqueKey = buildResolvedAssignmentUniqueKey(
                    roomIdentity,
                    startDateTime,
                    endDateTime,
                    rawAssignment,
                    inputData
            );

            resolved.add(new PreviousPartitionAssignmentsContext.ResolvedAssignment(
                    uniqueKey,
                    classIndexNumber.intValue(),
                    roomIndexNumber.intValue(),
                    rawAssignment,
                    startDateTime,
                    endDateTime,
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
        String roomName = RoomsMappingUtils.getRoomName(roomData, inputData.getRoomsMappingData());
        if (roomName != null && !roomName.trim().isBlank()) {
            return normalizeRoomIdentity(roomName);
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
            String day = ScheduleMappingUtils.getDay(classData, partition.getInputData().getMappingData());
            String start = ScheduleMappingUtils.getStartTime(classData, partition.getInputData().getMappingData());
            String requestedRoom = ScheduleMappingUtils.getRequestedRoomCharacteristics(
                    classData,
                    partition.getInputData().getMappingData()
            );

            String roomName = RoomsMappingUtils.getRoomName(roomData, partition.getInputData().getRoomsMappingData());
            String capacity = RoomsMappingUtils.getCapacity(roomData, partition.getInputData().getRoomsMappingData());
            String building = RoomsMappingUtils.getBuilding(roomData, partition.getInputData().getRoomsMappingData());

            log.info(
                    "Class [{}] {} | day={} | start={} | students={} | requestedRoom={} | roomIndex={} | room={} | building={} | capacity={}",
                    classIndex,
                    course,
                    day,
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

    @SuppressWarnings("unchecked")
    private Double extractObjectiveValue(Map<String, Object> executionResult) {
        Object value = executionResult.get("objectiveValue");
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        Object solutions = executionResult.get("solutions");
        if (solutions instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                Object objective = map.get("objectiveValue");
                if (objective instanceof Number number) {
                    return number.doubleValue();
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Integer extractEvaluationsUsed(Map<String, Object> executionResult) {
        Object metricsObj = executionResult.get("metrics");
        if (metricsObj instanceof Map<?, ?> metrics) {
            Object value = metrics.get("evaluationCount");
            if (value instanceof Number number) {
                return number.intValue();
            }
        }

        Object direct = executionResult.get("evaluationCount");
        if (direct instanceof Number number) {
            return number.intValue();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Double extractAvgEvaluationMs(Map<String, Object> executionResult) {
        Object metricsObj = executionResult.get("metrics");
        if (metricsObj instanceof Map<?, ?> metrics) {
            Object value = metrics.get("averageEvaluateTimeMs");
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long extractTotalEvaluateMs(Map<String, Object> executionResult) {
        Object metricsObj = executionResult.get("metrics");
        if (metricsObj instanceof Map<?, ?> metrics) {
            Object value = metrics.get("totalEvaluateTimeMs");
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long extractBuildAssignmentsMs(Map<String, Object> executionResult) {
        Object metricsObj = executionResult.get("metrics");
        if (metricsObj instanceof Map<?, ?> metrics) {
            Object value = metrics.get("totalBuildAssignmentsTimeMs");
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long extractConstraintEvalMs(Map<String, Object> executionResult) {
        Object metricsObj = executionResult.get("metrics");
        if (metricsObj instanceof Map<?, ?> metrics) {
            Object value = metrics.get("totalConstraintEvaluationTimeMs");
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Integer extractHardViolationsCount(Map<String, Object> executionResult) {
        Object value = executionResult.get("hardViolations");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private LocalDateTime extractPartitionStart(PartitionedProblemInputData partition) {
        if (partition == null || partition.getClassesInPartition() == null || partition.getClassesInPartition().isEmpty()) {
            return null;
        }

        LocalDateTime minStart = null;

        for (Map<String, Object> classData : partition.getClassesInPartition()) {
            if (classData == null) {
                continue;
            }

            LocalDate day = schedulePartitionService.parseDateValue(
                    ScheduleMappingUtils.getDay(classData, partition.getInputData().getMappingData())
            );
            LocalTime start = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getStartTime(classData, partition.getInputData().getMappingData())
            );

            if (day == null || start == null) {
                continue;
            }

            LocalDateTime startDateTime = LocalDateTime.of(day, start);
            if (minStart == null || startDateTime.isBefore(minStart)) {
                minStart = startDateTime;
            }
        }

        return minStart;
    }

    private String buildResolvedAssignmentUniqueKey(
            String roomIdentity,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            ClassRoomAssignment rawAssignment,
            ProblemInputData inputData
    ) {
        String normalizedRoom = normalizeRoomIdentity(roomIdentity);
        String start = startDateTime == null ? "" : startDateTime.toString();
        String end = endDateTime == null ? "" : endDateTime.toString();
        String classIdentity = extractStableClassIdentity(rawAssignment, inputData);

        return classIdentity + "|" + normalizedRoom + "|" + start + "|" + end;
    }

    private String extractStableClassIdentity(
            ClassRoomAssignment rawAssignment,
            ProblemInputData inputData
    ) {
        if (rawAssignment == null || rawAssignment.getClassData() == null) {
            return "unknown-class";
        }

        Map<String, Object> classData = rawAssignment.getClassData();

        Object explicitId = classData.get("id");
        if (explicitId != null) {
            return String.valueOf(explicitId);
        }

        return String.join("|",
                Objects.toString(ScheduleMappingUtils.getCourse(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getDegree(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getClassGroup(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getShift(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getDay(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getStartTime(classData, inputData.getMappingData()), ""),
                Objects.toString(ScheduleMappingUtils.getEndTime(classData, inputData.getMappingData()), "")
        );
    }

    private String normalizeRoomIdentity(String roomIdentity) {
        return roomIdentity == null ? "" : roomIdentity.trim().toLowerCase();
    }
}