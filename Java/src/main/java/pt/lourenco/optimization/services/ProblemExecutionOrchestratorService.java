package pt.lourenco.optimization.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataProvider;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataRegistry;
import pt.lourenco.optimization.jmetal.partitioning.PartitionReuseStrategy;
import pt.lourenco.optimization.jmetal.partitioning.PartitionType;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.problems.service.ProblemDataBuilderService;
import pt.lourenco.optimization.utils.JSONGetters;

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

    public Map<String, Object> executeProblem(JSONGetters request) throws JsonProcessingException {

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
        PartitionReuseStrategy reuseStrategy =
                resolveReuseStrategy(request.getRepeated_instance_strategy());

        Map<String, Object> executionResult =
                partitionExecutionCoordinatorService.execute(
                        inputData,
                        algorithmParameters,
                        partitionType,
                        reuseStrategy
                );

        log.info("=== END problem execution ===");
        log.debug("Execution result summary: {}", executionResult);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Problem executed successfully.");
        response.put("selected_algorithm", selectedAlgorithmName);
        response.put("partition_type", partitionType.name());
        response.put("reuse_strategy", reuseStrategy.name());
        response.put("prompt_used", finalPrompt);
        response.put("llm_parameters_response", llmResponse);
        response.put("normalized_algorithm_parameters", algorithmParameters);
        response.put("execution_result", executionResult);

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

    private PartitionReuseStrategy resolveReuseStrategy(String value) {
        if (value == null || value.isBlank()) {
            return PartitionReuseStrategy.INDEPENDENT;
        }

        return switch (value.trim().toLowerCase()) {
            case "reuse_solution" -> PartitionReuseStrategy.REUSE_EQUAL_PARTITION_SOLUTION;
            case "generate_new" -> PartitionReuseStrategy.INDEPENDENT;
            default -> throw new IllegalArgumentException("Unsupported repeated instance strategy: " + value);
        };
    }
}