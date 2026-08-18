package pt.lourenco.optimization.llm_tests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.problems.service.ProblemDataBuilderService;
import pt.lourenco.optimization.services.PromptBuilderService;
import pt.lourenco.optimization.utils.JSONGetters;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Testa a prompt "request-algorithms" contra vários modelos locais (Ollama),
 * repetindo N vezes por modelo para avaliar consistência.
 * Nome do ficheiro: algorithms_<nome-modelo-sanitizado>.txt
 * Cada ficheiro contém TODAS as repetições feitas a esse modelo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmsPromptBenchmarkService {

    private static final String PROMPT_TYPE = "algorithms";
    private static final String PROMPT_PATH = "prompts/request-algorithms-prompt.txt";
    private static final String OUTPUT_DIR = "benchmark-results/algorithms";

    // Quantas vezes repetir a MESMA prompt no MESMO modelo (para medir consistência).
    private static final int REPEATS_PER_MODEL = 20;

    private static final List<String> MODELS_TO_TEST = List.of(
            "qwen2.5:7b",
            "llama3.1:8b",
            "gemma4:e4b",
            "phi4-mini:3.8b",
            "mistral:7b",
            "deepseek-r1:8b"
    );

    private final ProblemDataBuilderService problemDataBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final OllamaBenchmarkClient ollamaBenchmarkClient;

    public Map<String, Object> runBenchmark(JSONGetters request) throws IOException {
        String problemData = problemDataBuilderService.buildProblemData(request);
        String finalPrompt = promptBuilderService.buildPrompt(PROMPT_PATH, problemData);

        Files.createDirectories(Paths.get(OUTPUT_DIR));

        Map<String, Object> summary = new LinkedHashMap<>();
        double benchmarkTotalSeconds = 0.0;

        for (String model : MODELS_TO_TEST) {
            List<OllamaBenchmarkClient.OllamaBenchmarkResult> runs = new ArrayList<>();
            double modelTotalSeconds = 0.0;

            for (int i = 1; i <= REPEATS_PER_MODEL; i++) {
                log.info("[{}] Modelo '{}' - repetição {}/{}", PROMPT_TYPE, model, i, REPEATS_PER_MODEL);

                OllamaBenchmarkClient.OllamaBenchmarkResult result =
                        ollamaBenchmarkClient.generate(model, finalPrompt);

                runs.add(result);
                modelTotalSeconds += result.wallClockMillis() / 1000.0;
            }

            benchmarkTotalSeconds += modelTotalSeconds;

            Path outputFile = Paths.get(OUTPUT_DIR, buildFileName(model));
            writeResultsToFile(outputFile, finalPrompt, runs, modelTotalSeconds);

            long successCount = runs.stream().filter(OllamaBenchmarkClient.OllamaBenchmarkResult::success).count();
            long jsonValidCount = runs.stream().filter(OllamaBenchmarkClient.OllamaBenchmarkResult::jsonParseable).count();

            Map<String, Object> modelSummary = new LinkedHashMap<>();
            modelSummary.put("repeats", REPEATS_PER_MODEL);
            modelSummary.put("successCount", successCount);
            modelSummary.put("jsonValidCount", jsonValidCount);
            modelSummary.put("modelTotalSeconds", modelTotalSeconds);
            modelSummary.put("avgSecondsPerRun", modelTotalSeconds / REPEATS_PER_MODEL);
            modelSummary.put("outputFile", outputFile.toString());

            summary.put(model, modelSummary);

            log.info("[{}] Modelo '{}' concluído: {}/{} sucesso, {}/{} JSON válido, {}s no total",
                    PROMPT_TYPE, model, successCount, REPEATS_PER_MODEL, jsonValidCount, REPEATS_PER_MODEL,
                    String.format("%.2f", modelTotalSeconds));
        }

        summary.put("_benchmarkTotalSeconds", benchmarkTotalSeconds);
        log.info("[{}] Benchmark completo. Tempo total: {}s", PROMPT_TYPE, String.format("%.2f", benchmarkTotalSeconds));

        return summary;
    }

    private String buildFileName(String model) {
        String sanitizedModel = model.replaceAll("[:/.]", "-");
        return PROMPT_TYPE + "_" + sanitizedModel + ".txt";
    }

    private void writeResultsToFile(
            Path outputFile,
            String finalPrompt,
            List<OllamaBenchmarkClient.OllamaBenchmarkResult> runs,
            double modelTotalSeconds
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("=== BENCHMARK: " + PROMPT_TYPE + " ===\n");
            writer.write("Modelo: " + runs.get(0).model() + "\n");
            writer.write("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("Numero de repeticoes: " + runs.size() + "\n");
            writer.write("Tempo TOTAL gasto neste modelo (segundos): " + String.format("%.3f", modelTotalSeconds) + "\n");
            writer.write("Tempo MEDIO por prompt (segundos): " + String.format("%.3f", modelTotalSeconds / runs.size()) + "\n");

            writer.write("\n--- PROMPT ENVIADA (igual em todas as repeticoes) ---\n");
            writer.write(finalPrompt);
            writer.write("\n");

            for (int i = 0; i < runs.size(); i++) {
                OllamaBenchmarkClient.OllamaBenchmarkResult result = runs.get(i);
                double runSeconds = result.wallClockMillis() / 1000.0;

                writer.write("\n==================== REPETICAO " + (i + 1) + "/" + runs.size() + " ====================\n");
                writer.write("Sucesso: " + result.success() + "\n");
                writer.write("JSON parseavel: " + result.jsonParseable() + "\n");
                writer.write("Tempo desta repeticao (segundos): " + String.format("%.3f", runSeconds) + "\n");
                writer.write("Tokens/segundo (eval): " + result.tokensPerSecond() + "\n");

                if (!result.success()) {
                    writer.write("--- ERRO ---\n");
                    writer.write(result.errorMessage() == null ? "N/A" : result.errorMessage());
                    writer.write("\n");
                }

                writer.write("--- RESPOSTA CRUA DO MODELO ---\n");
                writer.write(result.rawResponse() == null ? "N/A" : result.rawResponse());
                writer.write("\n");
            }
        }
    }
}