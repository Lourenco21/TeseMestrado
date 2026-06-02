package pt.lourenco.optimization.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.llm.LlmResponse;
import pt.lourenco.optimization.llm.OllamaClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmParameterSuggestionService {

    private static final String DEFAULT_MODEL = "llama3.1:8b";

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public LlmParameterSuggestionService() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> requestAndParseExecutionParameters(String prompt) {
        LlmResponse firstResponse = callModel(prompt);

        if (isUnusable(firstResponse)) {
            LlmResponse retrySamePrompt = callModel(prompt);

            if (isUnusable(retrySamePrompt)) {
                return fallback(
                        "The LLM returned no usable content.",
                        retrySamePrompt != null ? retrySamePrompt : firstResponse,
                        null,
                        null,
                        "empty_or_error_retry_failed"
                );
            }

            firstResponse = retrySamePrompt;
        }

        try {
            Map<String, Object> parsed = parseContent(firstResponse.getContent());
            parsed.put("selected_model", firstResponse.getModel());
            parsed.put("duration_ms", firstResponse.getDurationMs());
            parsed.put("retry_stage", "none");
            return parsed;

        } catch (Exception parseError) {
            String correctivePrompt = buildCorrectivePrompt(
                    prompt,
                    firstResponse.getContent(),
                    parseError.getMessage()
            );

            LlmResponse correctiveRetry = callModel(correctivePrompt);

            if (isUnusable(correctiveRetry)) {
                return fallback(
                        "The LLM returned invalid JSON and the corrective retry produced no usable content.",
                        correctiveRetry,
                        firstResponse != null ? firstResponse.getContent() : null,
                        parseError.getMessage(),
                        "parse_failed_then_empty_retry"
                );
            }

            try {
                Map<String, Object> parsedRetry = parseContent(correctiveRetry.getContent());
                parsedRetry.put("selected_model", correctiveRetry.getModel());
                parsedRetry.put("duration_ms", correctiveRetry.getDurationMs());
                parsedRetry.put("retry_stage", "corrective_prompt");
                return parsedRetry;

            } catch (Exception secondParseError) {
                return fallback(
                        "The LLM returned invalid JSON after corrective retry.",
                        correctiveRetry,
                        correctiveRetry.getContent(),
                        secondParseError.getMessage(),
                        "parse_failed_after_corrective_retry"
                );
            }
        }
    }

    private LlmResponse callModel(String prompt) {
        System.out.println("A testar modelo único para parâmetros: " + DEFAULT_MODEL);

        LlmResponse response = ollamaClient.chat(DEFAULT_MODEL, prompt);

        if (response.hasError()) {
            System.out.println("Erro no modelo " + DEFAULT_MODEL + ": " + response.getError());
            System.out.println("HTTP Status: " + response.getHttpStatusCode());
            System.out.println("Raw response: " + response.getRawResponse());
        } else {
            System.out.println("Teste concluído para " + DEFAULT_MODEL + " em " + response.getDurationMs() + " ms");
            System.out.println(response.getContent());
        }

        return response;
    }

    private boolean isUnusable(LlmResponse response) {
        return response == null
                || response.hasError()
                || response.getContent() == null
                || response.getContent().isBlank();
    }

    private Map<String, Object> parseContent(String content) throws Exception {
        String cleaned = extractJsonObject(stripMarkdownCodeFences(content));

        Map<String, Object> parsed = objectMapper.readValue(
                cleaned,
                new TypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> normalized = new HashMap<>();
        normalized.put("algorithm", parsed.get("algorithm"));
        normalized.put("parameters", parsed.getOrDefault("parameters", Map.of()));
        normalized.put("justification", parsed.getOrDefault("justification", List.of()));

        return normalized;
    }

    private String stripMarkdownCodeFences(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

    private String extractJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }

        return text;
    }

    private String buildCorrectivePrompt(String originalPrompt, String invalidContent, String parseError) {
        String invalid = invalidContent == null ? "" : invalidContent;
        String error = parseError == null ? "Unknown parsing error." : parseError;

        return originalPrompt + """

        IMPORTANT OUTPUT CORRECTION:
        Your previous answer was not valid JSON and could not be parsed by Jackson.

        Parsing error:
        """ + error + """

        Previous invalid answer:
        """ + invalid + """

        Now return ONLY one valid JSON object.
        Do not use markdown code fences.
        Do not include any text before or after the JSON.
        Do not include comments.
        Do not include literal line breaks inside JSON string values.
        If a line break is needed inside a string, escape it as \\n.
        Keep "justification" as a JSON array of short strings.
        Keep "parameters" as a JSON object.
        """;
    }

    private Map<String, Object> fallback(
            String message,
            LlmResponse response,
            String rawLlmResponse,
            String parsingError,
            String retryStage
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("algorithm", null);
        result.put("parameters", Map.of());
        result.put("justification", List.of(message));
        result.put("selected_model", response != null ? response.getModel() : DEFAULT_MODEL);
        result.put("duration_ms", response != null ? response.getDurationMs() : null);
        result.put("retry_stage", retryStage);
        result.put("parsing_error", parsingError);
        result.put("raw_llm_response", rawLlmResponse);
        result.put("llm_error", response != null ? response.getError() : null);
        result.put("http_status_code", response != null ? response.getHttpStatusCode() : null);
        return result;
    }
}