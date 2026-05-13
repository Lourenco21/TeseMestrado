package pt.lourenco.optimization.services;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.llm.LlmResponse;
import pt.lourenco.optimization.llm.OllamaClient;
import pt.lourenco.optimization.utils.JSONGetters;

@Service
public class LlmExecutionService {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final OllamaClient ollamaClient;

    public LlmExecutionService(
            ProblemDataBuilderService problemDataBuilderService,
            PromptBuilderService promptBuilderService
    ) {
        this.problemDataBuilderService = problemDataBuilderService;
        this.promptBuilderService = promptBuilderService;
        this.ollamaClient = new OllamaClient("http://localhost:11434");
    }

    public LlmResponse executeAlgorithmSelection(JSONGetters request, String model) {
        String problemData = problemDataBuilderService.buildProblemData(request);
        String finalPrompt = promptBuilderService.buildPrompt("prompts/prompt-teste.txt", problemData);

        return ollamaClient.chat(model, finalPrompt);
    }
}