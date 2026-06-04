package pt.lourenco.optimization.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.lourenco.optimization.jmetal.problems.service.ProblemDataBuilderService;
import pt.lourenco.optimization.services.PromptBuilderService;
import pt.lourenco.optimization.services.SingleModelLlmService;
import pt.lourenco.optimization.utils.JSONGetters;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "*")
public class ProblemAlgorithmRequestController {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final SingleModelLlmService singleModelLlmService;

    public ProblemAlgorithmRequestController(
            ProblemDataBuilderService problemDataBuilderService,
            PromptBuilderService promptBuilderService,
            SingleModelLlmService singleModelLlmService
    ) {
        this.problemDataBuilderService = problemDataBuilderService;
        this.promptBuilderService = promptBuilderService;
        this.singleModelLlmService = singleModelLlmService;
    }

    @PostMapping("/request-algorithms")
    public ResponseEntity<Object> testProblemAlgorithmRequest(
            @RequestBody JSONGetters request
    ) {
        try {
            String promptPath = "prompts/request-algorithms-prompt.txt";

            String problemData = problemDataBuilderService.buildProblemData(request);
            String finalPrompt = promptBuilderService.buildPrompt(promptPath, problemData);

            Map<String, Object> result = singleModelLlmService.requestAndParseAlgorithms(finalPrompt);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao processar resposta do LLM.",
                            "details", e.getMessage()
                    ));
        }
    }
}