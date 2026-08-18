package pt.lourenco.optimization.llm_tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP mínimo para o Ollama, usado apenas para efeitos de benchmark.
 * Usa /api/chat (em vez de /api/generate) porque o parâmetro "think": false
 * é ignorado em /api/generate para alguns modelos (ex. Qwen3.5), mas é
 * respeitado de forma fiável em /api/chat.
 */
@Slf4j
@Component
public class OllamaBenchmarkClient {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    // Limite de tokens de saída, como rede de segurança contra "overthinking"
    // (modelos que entram em loops de raciocínio muito longos).
    private static final int MAX_OUTPUT_TOKENS = 2048;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaBenchmarkResult generate(String model, String prompt) {
        long wallClockStartNs = System.nanoTime();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false,
                "think", false,
                "options", Map.of("num_predict", MAX_OUTPUT_TOKENS)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/api/chat",
                    entity,
                    Map.class
            );

            long wallClockElapsedMs = Duration.ofNanos(System.nanoTime() - wallClockStartNs).toMillis();

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                return OllamaBenchmarkResult.failure(model, wallClockElapsedMs, "Corpo de resposta vazio.");
            }

            String rawResponse = extractContent(responseBody);
            Long totalDurationNanos = toLongOrNull(responseBody.get("total_duration"));
            Long evalCount = toLongOrNull(responseBody.get("eval_count"));
            Long evalDurationNanos = toLongOrNull(responseBody.get("eval_duration"));

            boolean jsonParseable = isJsonParseable(rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.warn("Modelo '{}' devolveu resposta vazia (possível esgotamento do orçamento de 'thinking').", model);
            }

            return new OllamaBenchmarkResult(
                    model,
                    true,
                    null,
                    rawResponse,
                    wallClockElapsedMs,
                    totalDurationNanos,
                    evalCount,
                    evalDurationNanos,
                    jsonParseable
            );

        } catch (Exception e) {
            long wallClockElapsedMs = Duration.ofNanos(System.nanoTime() - wallClockStartNs).toMillis();
            log.error("Erro ao chamar o modelo '{}' no Ollama.", model, e);
            return OllamaBenchmarkResult.failure(model, wallClockElapsedMs, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> responseBody) {
        Object messageObject = responseBody.get("message");
        if (messageObject instanceof Map<?, ?> message) {
            Object content = message.get("content");
            if (content != null) {
                return String.valueOf(content);
            }
        }
        return null;
    }

    private boolean isJsonParseable(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return false;
        }
        int start = rawResponse.indexOf('{');
        int end = rawResponse.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            return false;
        }
        String candidate = rawResponse.substring(start, end + 1);
        try {
            JsonNode node = objectMapper.readTree(candidate);
            return node != null;
        } catch (Exception e) {
            return false;
        }
    }

    private Long toLongOrNull(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public record OllamaBenchmarkResult(
            String model,
            boolean success,
            String errorMessage,
            String rawResponse,
            long wallClockMillis,
            Long totalDurationNanos,
            Long evalCount,
            Long evalDurationNanos,
            boolean jsonParseable
    ) {
        public static OllamaBenchmarkResult failure(String model, long wallClockMillis, String errorMessage) {
            return new OllamaBenchmarkResult(
                    model, false, errorMessage, null, wallClockMillis, null, null, null, false
            );
        }

        public Double tokensPerSecond() {
            if (evalCount == null || evalDurationNanos == null || evalDurationNanos == 0) {
                return null;
            }
            return evalCount / (evalDurationNanos / 1_000_000_000.0);
        }
    }
}