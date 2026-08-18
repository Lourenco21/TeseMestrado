package pt.lourenco.optimization.llm_tests;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.Map;

/**
 * Endpoints para disparar os benchmarks de modelos LLM locais (Ollama).
 * Cada chamada corre a MESMA prompt final contra todos os modelos configurados
 * e grava um ficheiro de resultado por modelo em benchmark-results/.
 */
@RestController
@RequestMapping("/benchmark")
@RequiredArgsConstructor
public class LlmBenchmarkController {

    private final AlgorithmsPromptBenchmarkService algorithmsPromptBenchmarkService;
    private final ParametersPromptBenchmarkService parametersPromptBenchmarkService;

    @PostMapping("/request-algorithms")
    public ResponseEntity<Object> benchmarkAlgorithmsPrompt(@RequestBody JSONGetters request) {
        try {
            Map<String, Object> summary = algorithmsPromptBenchmarkService.runBenchmark(request);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao correr o benchmark de algoritmos.",
                            "details", e.getMessage()
                    ));
        }
    }

    @PostMapping("/request-parameters")
    public ResponseEntity<Object> benchmarkParametersPrompt(@RequestBody JSONGetters request) {
        try {
            Map<String, Object> summary = parametersPromptBenchmarkService.runBenchmark(request);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Erro ao correr o benchmark de parametros.",
                            "details", e.getMessage()
                    ));
        }
    }
}
