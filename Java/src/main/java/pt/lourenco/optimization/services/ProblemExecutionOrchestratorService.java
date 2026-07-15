package pt.lourenco.optimization.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataProvider;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataRegistry;
import pt.lourenco.optimization.jmetal.partitioning.PartitionType;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.problems.service.ProblemDataBuilderService;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemExecutionOrchestratorService {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final LlmParameterSuggestionService llmParameterSuggestionService;
    private final AlgorithmMetadataRegistry algorithmMetadataRegistry;
    private final PartitionExecutionCoordinatorService partitionExecutionCoordinatorService;
    private final ObjectMapper objectMapper;
    private final ScheduleResponseBuilderService scheduleResponseBuilderService;

    public Map<String, Object> executeProblem(JSONGetters request) throws JsonProcessingException {
        long totalStartNs = System.nanoTime();

        log.debug("Raw schedule_data present: {}", request.getSchedule_data() != null);
        log.debug("Raw rooms_data present: {}", request.getRooms_data() != null);

        if (request.getSchedule_data() != null) {
            log.debug("Raw schedule_data keys: {}", request.getSchedule_data().keySet());
            Object classesObject = request.getSchedule_data().get("classes");
            log.debug("Raw schedule_data.classes type: {}", classesObject == null ? null : classesObject.getClass().getName());
            log.debug("Raw schedule_data.classes size: {}", classesObject instanceof java.util.List<?> list ? list.size() : null);
        }

        if (request.getRooms_data() != null) {
            log.debug("Raw rooms_data keys: {}", request.getRooms_data().keySet());
            Object roomsObject = request.getRooms_data().get("rooms");
            log.debug("Raw rooms_data.rooms type: {}", roomsObject == null ? null : roomsObject.getClass().getName());
            log.debug("Raw rooms_data.rooms size: {}", roomsObject instanceof java.util.List<?> list ? list.size() : null);
        }

        log.info("=== START problem execution ===");
        log.info("Selected algorithm: {}", request.getSelected_algorithm());
        log.info("Resolution scope: {}", request.getResolution_scope());
        log.info("Repeated instance strategy: {}", request.getRepeated_instance_strategy());
        log.debug("Problem name: {}", request.getName());
        log.debug("Problem type/subtype: {}/{}", request.getProblem_type(), request.getProblem_subtype());
        log.debug("Mapping data: {}", request.getMapping_data());
        log.debug("Rooms mapping data: {}", request.getRooms_mapping_data());

        String promptPath = "prompts/parameters-prompt.txt";

        String problemData = problemDataBuilderService.buildExecutionProblemData(request);

        String selectedAlgorithmName = request.getSelected_algorithm();
        AlgorithmMetadataProvider algorithmMetadata = algorithmMetadataRegistry.getByName(selectedAlgorithmName);

        String finalPrompt = promptBuilderService.buildPromptWithPlaceholders(
                promptPath,
                Map.of(
                        "ALGORITHM_NAME", algorithmMetadata.getDisplayName(),
                        "ALGORITHM_OPERATORS_DESCRIPTION", algorithmMetadata.getOperatorsDescription(),
                        "ALGORITHM_COHERENCE_RULE", algorithmMetadata.getCoherenceRule(),
                        "PARAMETERS_LIST", algorithmMetadata.getParametersList(),
                        "PARAMETERS_JSON", algorithmMetadata.getParametersJson(),
                        "PROBLEM_DATA", problemData
                )
        );

        Map<String, Object> llmResponse =
                llmParameterSuggestionService.requestAndParseExecutionParameters(finalPrompt);

        log.info("LLM parameters received");
        log.debug("LLM parameters payload: {}", llmResponse);

        Map<String, Object> algorithmParameters = extractAlgorithmParameters(llmResponse);
        algorithmParameters = applyAlgorithmDefaults(algorithmMetadata, algorithmParameters);
        validateAlgorithmParameters(algorithmMetadata, algorithmParameters);

        log.info("Normalized algorithm parameters ready");
        log.debug("Normalized algorithm parameters: {}", algorithmParameters);

        ProblemInputData inputData = problemDataBuilderService.buildProblemInputData(request);

        Object classesObject = inputData.getScheduleData() == null ? null : inputData.getScheduleData().get("classes");
        Object roomsObject = inputData.getRoomsData() == null ? null : inputData.getRoomsData().get("rooms");

        int classCount = (classesObject instanceof java.util.List<?> list) ? list.size() : 0;
        int roomCount = (roomsObject instanceof java.util.List<?> list) ? list.size() : 0;

        log.info("Input loaded: {} classes, {} rooms", classCount, roomCount);
        log.debug("Selected constraints count: {}", inputData.getSelectedConstraints() == null ? 0 : inputData.getSelectedConstraints().size());

        PartitionType partitionType = resolvePartitionType(request.getResolution_scope());

        Map<String, Object> executionResult =
                partitionExecutionCoordinatorService.execute(
                        inputData,
                        algorithmParameters,
                        partitionType
                );

        List<Map<String, Object>> originalSchedule = extractOriginalScheduleRows(inputData.getScheduleData());

        List<ScheduleResponseBuilderService.SolvedClassRoomAssignment> solvedAssignments =
                extractSolvedAssignments(executionResult, inputData);

        Map<String, Object> aggregatedConstraintValues = aggregateConstraintValues(executionResult);
        Map<String, Object> aggregatedPenaltySummary = aggregatePenaltySummary(executionResult);

        List<Map<String, Object>> updatedSchedule =
                scheduleResponseBuilderService.buildUpdatedSchedule(
                        originalSchedule,
                        inputData.getMappingData(),
                        solvedAssignments
                );

        log.info("=== END problem execution ===");
        long totalElapsedNs = System.nanoTime() - totalStartNs;
        double totalElapsedSeconds = totalElapsedNs / 1_000_000_000.0 ;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Problem executed successfully.");
        response.put("schedule", updatedSchedule);
        response.put("algorithm_used", selectedAlgorithmName);
        response.put("used_parameters", algorithmParameters);
        response.put("partition_type", partitionType.name());
        response.put("partition_count", executionResult.get("partitionCount"));
        response.put("constraint_values", aggregatedConstraintValues);
        response.put("penalty_summary", aggregatedPenaltySummary);
        response.put("execution_time_seconds", totalElapsedSeconds);



        log.info("Total problem execution time: {} seconds", String.format("%.3f", totalElapsedSeconds));

        return response;
    }

    private Map<String, Object> extractAlgorithmParameters(Map<String, Object> llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            throw new IllegalArgumentException("LLM response is empty.");
        }

        Object parametersNode = llmResponse.get("parameters");

        if (parametersNode instanceof Map<?, ?> parametersMap && !parametersMap.isEmpty()) {
            return objectMapper.convertValue(
                    parametersMap,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        if (parametersNode instanceof String parametersJson && !parametersJson.isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        parametersJson,
                        new TypeReference<Map<String, Object>>() {}
                );
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException("Failed to parse 'parameters' JSON from LLM response.", ex);
            }
        }

        boolean looksFlat =
                llmResponse.containsKey("populationSize") ||
                        llmResponse.containsKey("maxEvaluations") ||
                        llmResponse.containsKey("crossoverProbability") ||
                        llmResponse.containsKey("mutationProbability") ||
                        llmResponse.containsKey("etaC") ||
                        llmResponse.containsKey("etaM");

        if (looksFlat) {
            return objectMapper.convertValue(
                    llmResponse,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        throw new IllegalArgumentException("No algorithm parameters found in LLM response: " + llmResponse);
    }

    private Map<String, Object> applyAlgorithmDefaults(
            AlgorithmMetadataProvider algorithmMetadata,
            Map<String, Object> parameters
    ) {
        Map<String, Object> result = new LinkedHashMap<>(parameters);
        Map<String, Object> defaults = algorithmMetadata.getDefaultParameterValues();

        if (defaults == null || defaults.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            putIfMissingOrNull(result, entry.getKey(), entry.getValue());
        }

        return result;
    }

    private void validateAlgorithmParameters(
            AlgorithmMetadataProvider algorithmMetadata,
            Map<String, Object> parameters
    ) {
        List<String> requiredKeys = algorithmMetadata.getRequiredParameterKeys();

        if (requiredKeys == null || requiredKeys.isEmpty()) {
            return;
        }

        List<String> missing = requiredKeys.stream()
                .filter(key -> parameters.get(key) == null)
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required parameters for algorithm '" +
                            algorithmMetadata.getAlgorithmKey() +
                            "': " + missing +
                            " | payload=" + parameters
            );
        }
    }

    private void putIfMissingOrNull(Map<String, Object> target, String key, Object defaultValue) {
        if (!target.containsKey(key) || target.get(key) == null) {
            target.put(key, defaultValue);
        }
    }

    private PartitionType resolvePartitionType(String value) {
        if (value == null || value.isBlank()) {
            return PartitionType.SEMESTER;
        }

        return switch (value.trim().toLowerCase()) {
            case "semester" -> PartitionType.SEMESTER;
            case "week" -> PartitionType.WEEK;
            case "day" -> PartitionType.DAY;
            case "start_half_hour" -> PartitionType.START_HALF_HOUR;
            default -> throw new IllegalArgumentException("Unsupported partition type: " + value);
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractOriginalScheduleRows(Map<String, Object> scheduleData) {
        if (scheduleData == null) {
            return List.of();
        }

        Object classes = scheduleData.get("classes");
        return classes instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ScheduleResponseBuilderService.SolvedClassRoomAssignment> extractSolvedAssignments(
            Map<String, Object> executionResult,
            ProblemInputData inputData
    ) {
        List<ScheduleResponseBuilderService.SolvedClassRoomAssignment> solvedAssignments = new ArrayList<>();

        Object partitionsObject = executionResult.get("partitions");
        if (!(partitionsObject instanceof List<?> partitions)) {
            return solvedAssignments;
        }

        for (Object partitionObject : partitions) {
            if (!(partitionObject instanceof Map<?, ?> rawPartition)) {
                continue;
            }

            Map<String, Object> partition = (Map<String, Object>) rawPartition;
            Object partitionExecutionResultObject = partition.get("executionResult");

            if (!(partitionExecutionResultObject instanceof Map<?, ?> rawPartitionExecutionResult)) {
                continue;
            }

            Map<String, Object> partitionExecutionResult = (Map<String, Object>) rawPartitionExecutionResult;
            Object solutionsObject = partitionExecutionResult.get("solutions");

            if (!(solutionsObject instanceof List<?> solutions) || solutions.isEmpty()) {
                continue;
            }

            Object bestSolutionObject = solutions.get(0);
            if (!(bestSolutionObject instanceof Map<?, ?> rawBestSolution)) {
                continue;
            }

            Map<String, Object> bestSolution = (Map<String, Object>) rawBestSolution;
            Object assignmentsObject = bestSolution.get("assignments");

            if (!(assignmentsObject instanceof List<?> assignments)) {
                continue;
            }

            for (Object assignmentObject : assignments) {
                if (!(assignmentObject instanceof Map<?, ?> rawAssignment)) {
                    continue;
                }

                Map<String, Object> assignment = (Map<String, Object>) rawAssignment;
                Object classDataObject = assignment.get("classData");
                Object roomDataObject = assignment.get("roomData");

                if (!(classDataObject instanceof Map<?, ?> rawClassData)) {
                    continue;
                }

                if (!(roomDataObject instanceof Map<?, ?> rawRoomData)) {
                    continue;
                }

                Map<String, Object> classData = (Map<String, Object>) rawClassData;
                Map<String, Object> roomData = (Map<String, Object>) rawRoomData;

                String classKey = scheduleResponseBuilderService.buildClassKey(
                        classData,
                        inputData.getMappingData()
                );

                String roomName = RoomsMappingUtils.getRoomName(
                        roomData,
                        inputData.getRoomsMappingData()
                );

                solvedAssignments.add(
                        new ScheduleResponseBuilderService.SolvedClassRoomAssignment(classKey, roomName)
                );
            }
        }

        return solvedAssignments;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregateConstraintValues(Map<String, Object> executionResult) {
        Map<String, Object> aggregated = new LinkedHashMap<>();

        Object partitionsObject = executionResult.get("partitions");
        if (!(partitionsObject instanceof List<?> partitions)) {
            return aggregated;
        }

        for (Object partitionObject : partitions) {
            if (!(partitionObject instanceof Map<?, ?> rawPartition)) {
                continue;
            }

            Map<String, Object> partition = (Map<String, Object>) rawPartition;
            Object partitionExecutionResultObject = partition.get("executionResult");

            if (!(partitionExecutionResultObject instanceof Map<?, ?> rawPartitionExecutionResult)) {
                continue;
            }

            Map<String, Object> partitionExecutionResult = (Map<String, Object>) rawPartitionExecutionResult;
            Object solutionsObject = partitionExecutionResult.get("solutions");

            if (!(solutionsObject instanceof List<?> solutions) || solutions.isEmpty()) {
                continue;
            }

            Object bestSolutionObject = solutions.get(0);
            if (!(bestSolutionObject instanceof Map<?, ?> rawBestSolution)) {
                continue;
            }

            Map<String, Object> bestSolution = (Map<String, Object>) rawBestSolution;
            Object constraintValuesObject = bestSolution.get("constraintValues");

            if (!(constraintValuesObject instanceof Map<?, ?> rawConstraintValues)) {
                continue;
            }

            Map<String, Object> constraintValues = (Map<String, Object>) rawConstraintValues;

            for (Map.Entry<String, Object> entry : constraintValues.entrySet()) {
                String constraintId = entry.getKey();

                if (!(entry.getValue() instanceof Map<?, ?> rawItem)) {
                    continue;
                }

                Map<String, Object> item = (Map<String, Object>) rawItem;
                String goal = item.get("goal") == null ? null : String.valueOf(item.get("goal"));
                double raw = asDouble(item.get("raw"));
                double weighted = asDouble(item.get("weighted"));

                Map<String, Object> aggregateItem = (Map<String, Object>) aggregated.computeIfAbsent(
                        constraintId,
                        key -> {
                            Map<String, Object> created = new LinkedHashMap<>();
                            created.put("goal", goal);
                            created.put("raw", 0.0);
                            created.put("weighted", 0.0);
                            return created;
                        }
                );

                aggregateItem.put("raw", asDouble(aggregateItem.get("raw")) + raw);
                aggregateItem.put("weighted", asDouble(aggregateItem.get("weighted")) + weighted);
            }
        }

        return aggregated;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregatePenaltySummary(Map<String, Object> executionResult) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("soft_raw_total", 0.0);
        summary.put("soft_weighted_total", 0.0);
        summary.put("hard_raw_total", 0.0);
        summary.put("hard_weighted_total", 0.0);

        Object partitionsObject = executionResult.get("partitions");
        if (!(partitionsObject instanceof List<?> partitions)) {
            return summary;
        }

        for (Object partitionObject : partitions) {
            if (!(partitionObject instanceof Map<?, ?> rawPartition)) {
                continue;
            }

            Map<String, Object> partition = (Map<String, Object>) rawPartition;
            Object partitionExecutionResultObject = partition.get("executionResult");

            if (!(partitionExecutionResultObject instanceof Map<?, ?> rawPartitionExecutionResult)) {
                continue;
            }

            Map<String, Object> partitionExecutionResult = (Map<String, Object>) rawPartitionExecutionResult;
            Object solutionsObject = partitionExecutionResult.get("solutions");

            if (!(solutionsObject instanceof List<?> solutions) || solutions.isEmpty()) {
                continue;
            }

            Object bestSolutionObject = solutions.get(0);
            if (!(bestSolutionObject instanceof Map<?, ?> rawBestSolution)) {
                continue;
            }

            Map<String, Object> bestSolution = (Map<String, Object>) rawBestSolution;
            Object penaltySummaryObject = bestSolution.get("penaltySummary");

            if (!(penaltySummaryObject instanceof Map<?, ?> rawPenaltySummary)) {
                continue;
            }

            Map<String, Object> penaltySummary = (Map<String, Object>) rawPenaltySummary;

            summary.put("soft_raw_total",
                    asDouble(summary.get("soft_raw_total")) + asDouble(penaltySummary.get("soft_raw_total")));
            summary.put("soft_weighted_total",
                    asDouble(summary.get("soft_weighted_total")) + asDouble(penaltySummary.get("soft_weighted_total")));
            summary.put("hard_raw_total",
                    asDouble(summary.get("hard_raw_total")) + asDouble(penaltySummary.get("hard_raw_total")));
            summary.put("hard_weighted_total",
                    asDouble(summary.get("hard_weighted_total")) + asDouble(penaltySummary.get("hard_weighted_total")));
        }

        return summary;
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }

        return 0.0;
    }
}