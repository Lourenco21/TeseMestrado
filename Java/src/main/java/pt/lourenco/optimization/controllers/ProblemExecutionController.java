package pt.lourenco.optimization.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.lourenco.optimization.services.ProblemExecutionOrchestratorService;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemExecutionController {

    private final ProblemExecutionOrchestratorService problemExecutionOrchestratorService;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeProblem(@RequestBody JSONGetters request) throws JsonProcessingException {
        Map<String, Object> response = problemExecutionOrchestratorService.executeProblem(request);
        return ResponseEntity.ok(response);
    }
}