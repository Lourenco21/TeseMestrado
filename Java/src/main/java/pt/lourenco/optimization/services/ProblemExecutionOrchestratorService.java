package pt.lourenco.optimization.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataProvider;
import pt.lourenco.optimization.jmetal.algorithms.AlgorithmMetadataRegistry;
import pt.lourenco.optimization.jmetal.algorithms.Nsgaii;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProblemExecutionOrchestratorService {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final LlmParameterSuggestionService llmParameterSuggestionService;
    private final AlgorithmMetadataRegistry algorithmMetadataRegistry;

    public Map<String, Object> executeProblem(JSONGetters request) throws JsonProcessingException {
        String promptPath = "prompts/parameters-prompt.txt";

        String problemData = problemDataBuilderService.buildExecutionProblemData(request);

        String selectedAlgorithmName = request.getSelected_algorithm();
        AlgorithmMetadataProvider algorithmMetadata = algorithmMetadataRegistry.getByName(selectedAlgorithmName);
        System.out.println(algorithmMetadataRegistry);

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

        Map<String, Object> llmResponse = llmParameterSuggestionService.requestAndParseExecutionParameters(finalPrompt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Problem executed successfully.");
        response.put("selected_algorithm", selectedAlgorithmName);
        response.put("prompt_used", finalPrompt);
        response.put("llm_parameters_response", llmResponse);

        return response;
    }
}