package pt.lourenco.optimization.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.lourenco.optimization.llm.LlmResponse;
import pt.lourenco.optimization.services.LlmTestService;
import pt.lourenco.optimization.jmetal.problems.service.ProblemDataBuilderService;
import pt.lourenco.optimization.services.PromptBuilderService;
import pt.lourenco.optimization.utils.JSONGetters;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "*")
public class ProblemExecutionController_OG {

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final LlmTestService llmTestService;

    public ProblemExecutionController_OG(ProblemDataBuilderService problemDataBuilderService, PromptBuilderService promptBuilderService, LlmTestService llmTestService){
        this.problemDataBuilderService = problemDataBuilderService;
        this.promptBuilderService = promptBuilderService;
        this.llmTestService = llmTestService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runProblem(
            @RequestBody JSONGetters request
    ) {

        String algorithmPrompt = "prompts/algorithm-prompt.txt";
        String codePrompt = "prompts/code-prompt.txt";
        String parametersPrompt = "prompts/parameters-prompt.txt";

        String problemData = problemDataBuilderService.buildProblemData(request);
        String finalPrompt = promptBuilderService.buildPrompt(/**algorithmPrompt codePrompt**/ parametersPrompt, problemData);

        List<LlmResponse> llmResults = llmTestService.runTestsWithPrompt(finalPrompt);

        Map<String, Object> response = getStringObjectMap(request);
        System.out.println("Problema recebido");
        System.out.println(response);

        return ResponseEntity.ok(response);
    }

    private static Map<String, Object> getStringObjectMap(JSONGetters request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Problema recebido com sucesso no backend Java.");
        response.put("problem_id", request.getProblem_id());
        response.put("name", request.getName());
        response.put("problem_type", request.getProblem_type());
        response.put("problem_subtype", request.getProblem_subtype());
        response.put("schedule_file_id", request.getSchedule_file_id());
        response.put("rooms_file_id", request.getRooms_file_id());
        response.put("mapping_received", request.getMapping_data() != null);
        response.put("rooms_mapping_received", request.getRooms_mapping_data() != null);
        response.put("objectives_count", request.getObjectives() != null ? request.getObjectives().size() : 0);
        response.put("constraints_count", request.getConstraints() != null ? request.getConstraints().size() : 0);
        return response;
    }
}