package pt.lourenco.optimization.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.lourenco.optimization.llm.LlmResponse;
import pt.lourenco.optimization.llm.LlmTestService;
import pt.lourenco.optimization.services.ProblemDataBuilderService;
import pt.lourenco.optimization.services.PromptBuilderService;
import pt.lourenco.optimization.utils.JSONGetters;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "*")
public class ProblemAlgorithmRequestController {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final LlmTestService llmTestService;

    public ProblemAlgorithmRequestController(
            ProblemDataBuilderService problemDataBuilderService,
            PromptBuilderService promptBuilderService,
            LlmTestService llmTestService
    ) {
        this.problemDataBuilderService = problemDataBuilderService;
        this.promptBuilderService = promptBuilderService;
        this.llmTestService = llmTestService;
    }

    @PostMapping("/request-algorithms")
    public ResponseEntity<Object> testProblemAlgorithmRequest(
            @RequestBody JSONGetters request
    ) {
        try {
            String promptPath = "prompts/request-algorithms-prompt.txt";

            String problemData = problemDataBuilderService.buildProblemData(request);
            String finalPrompt = promptBuilderService.buildPrompt(promptPath, problemData);

            List<LlmResponse> llmResults = llmTestService.runTestsWithPrompt(finalPrompt);

            if (llmResults == null || llmResults.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "O LLM não devolveu resultados."));
            }

            String content = llmResults.get(0).getContent();
            System.out.println(llmResults.get(0));
            System.out.println(content);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(content, Map.class);

            return ResponseEntity.ok(parsed);

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