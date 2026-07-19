package pt.lourenco.optimization.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutionRegistry;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmExecutor;
import pt.lourenco.optimization.jmetal.partitioning.*;
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
    private final AlgorithmExecutionRegistry algorithmExecutionRegistry;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public PartitionExecutionCoordinatorService(
            SchedulePartitionService schedulePartitionService,
            AlgorithmExecutionRegistry algorithmExecutionRegistry,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.schedulePartitionService = schedulePartitionService;
        this.algorithmExecutionRegistry = algorithmExecutionRegistry;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(
            ProblemInputData inputData,
            Map<String, Object> parameters,
            PartitionType partitionType
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

        LocalDateTime scheduleOriginDateTime = resolveScheduleOriginDateTime(inputData);
        int numberOfRooms = extractRooms(inputData.getRoomsData()).size();

        GlobalRoomOccupationTracker globalRoomOccupationTracker =
                new GlobalRoomOccupationTracker(numberOfRooms, scheduleOriginDateTime);

        for (PartitionedProblemInputData partition : partitions) {

            long partitionStartNs = System.nanoTime();

            LocalDateTime partitionStart = extractPartitionStart(partition);
            previousAssignmentsContext.removeEndedBefore(partitionStart);

            injectPreviousAssignmentsContext(partition.getInputData(), previousAssignmentsContext, globalRoomOccupationTracker);

            Map<String, Object> executionResult;

            int classCount = partition.getClassesInPartition() == null
                    ? 0
                    : partition.getClassesInPartition().size();

            executionResult = executor.run(partition.getInputData(), parameters);

            Object solutionsObject = executionResult.get("solutions");
            int solutionCount = (solutionsObject instanceof List<?> list) ? list.size() : 0;

            List<PreviousPartitionAssignmentsContext.ResolvedAssignment> resolvedAssignments =
                    resolveAssignmentsFromExecution(executionResult, partition);

            previousAssignmentsContext.addAll(resolvedAssignments);
            for (PreviousPartitionAssignmentsContext.ResolvedAssignment resolved : resolvedAssignments) {
                globalRoomOccupationTracker.commitOccupation(
                        resolved.getRoomIndex(),
                        resolved.getStartDateTime(),
                        resolved.getEndDateTime()
                );
            }

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
                    "Partition finished | type={} order={} key={} classes={} durationMs={} solutions={} resolvedAssignments={} objective={} evaluations={} evalAvgMs={} evalTotalMs={} buildAssignmentsMs={} constraintEvalMs={} hardViolations={}",
                    partition.getPartitionType().name(),
                    partition.getPartitionOrder(),
                    partition.getPartitionKey(),
                    classCount,
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
                "Partition execution completed | type={} partitions={} totalDurationMs={} avgPartitionDurationMs={}",
                partitionType.name(),
                partitionResults.size(),
                totalExecutionMs,
                partitionResults.isEmpty() ? 0.0 : (double) totalPartitionExecutionMs / partitionResults.size()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("partitionType", partitionType.name());
        response.put("partitionCount", partitionResults.size());
        response.put("totalExecutionMs", totalExecutionMs);
        response.put("averagePartitionDurationMs",
                partitionResults.isEmpty() ? 0.0 : (double) totalPartitionExecutionMs / partitionResults.size());
        response.put("partitions", partitionResults);
        return response;
    }

    private LocalDateTime resolveScheduleOriginDateTime(ProblemInputData inputData) {
        Object classesObject = inputData.getScheduleData().get("classes");
        if (!(classesObject instanceof List<?> rawClasses)) {
            return LocalDateTime.now();
        }

        LocalDateTime earliest = null;

        for (Object classObject : rawClasses) {
            if (!(classObject instanceof Map<?, ?> rawClassData)) continue;
            Map<String, Object> classData = (Map<String, Object>) rawClassData;

            LocalDate day = schedulePartitionService.parseDateValue(
                    ScheduleMappingUtils.getDay(classData, inputData.getMappingData()));
            LocalTime start = schedulePartitionService.coerceToLocalTime(
                    ScheduleMappingUtils.getStartTime(classData, inputData.getMappingData()));

            if (day == null || start == null) continue;

            LocalDateTime candidate = LocalDateTime.of(day, start);
            if (earliest == null || candidate.isBefore(earliest)) {
                earliest = candidate;
            }
        }

        return earliest == null ? LocalDateTime.now() : earliest;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRooms(Map<String, Object> roomsData) {
        Object roomsObject = roomsData.get("rooms");
        return roomsObject instanceof List<?> rawRooms ? (List<Map<String, Object>>) rawRooms : List.of();
    }

    private void injectPreviousAssignmentsContext(
            ProblemInputData inputData,
            PreviousPartitionAssignmentsContext previousAssignmentsContext,
            GlobalRoomOccupationTracker globalRoomOccupationTracker
    ) {
        Map<String, Object> metadata = inputData.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(inputData.getMetadata());

        metadata.put("previousPartitionAssignmentsContext", previousAssignmentsContext);
        metadata.put("globalRoomOccupationTracker", globalRoomOccupationTracker);
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
                log.warn("SKIPPED assignment while resolving for global tracker | classIndex={} day={} start={} end={} classData={}",
                        classIndexNumber, day, start, end, classData);
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