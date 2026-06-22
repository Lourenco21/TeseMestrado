package pt.lourenco.optimization.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.lourenco.optimization.llm.LlmResponse;
import pt.lourenco.optimization.llm.OllamaClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SingleModelLlmService {

    private static final String DEFAULT_MODEL = "llama3.1:8b";

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public SingleModelLlmService() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> requestAndParseAlgorithms(String prompt) {
        LlmResponse firstResponse = callModel(prompt);
        log.info("DEBUG LLM: first call completed");

        if (isUnusable(firstResponse)) {
            log.warn("DEBUG LLM: retrying same prompt because first response is unusable");

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
            parsed.put("sanitized_before_parse", false);
            return parsed;

        } catch (Exception parseError) {
            log.warn("DEBUG LLM: normal JSON parse failed: {}", parseError.getMessage());

            try {
                Map<String, Object> parsedSanitized = parseContentWithSanitization(firstResponse.getContent());
                parsedSanitized.put("selected_model", firstResponse.getModel());
                parsedSanitized.put("duration_ms", firstResponse.getDurationMs());
                parsedSanitized.put("retry_stage", "none");
                parsedSanitized.put("sanitized_before_parse", true);
                log.warn("DEBUG LLM: parse succeeded after local sanitization");
                return parsedSanitized;

            } catch (Exception sanitizedParseError) {
                log.warn("DEBUG LLM: sanitization parse also failed: {}", sanitizedParseError.getMessage());

                String correctivePrompt = buildCorrectivePrompt(
                        prompt,
                        firstResponse.getContent(),
                        sanitizedParseError.getMessage()
                );

                log.warn("DEBUG LLM: retrying with corrective prompt because JSON parse failed after sanitization");
                LlmResponse correctiveRetry = callModel(correctivePrompt);

                if (isUnusable(correctiveRetry)) {
                    return fallback(
                            "The LLM returned invalid JSON and the corrective retry produced no usable content.",
                            correctiveRetry,
                            firstResponse != null ? firstResponse.getContent() : null,
                            sanitizedParseError.getMessage(),
                            "parse_failed_then_empty_retry"
                    );
                }

                try {
                    Map<String, Object> parsedRetry = parseContent(correctiveRetry.getContent());
                    parsedRetry.put("selected_model", correctiveRetry.getModel());
                    parsedRetry.put("duration_ms", correctiveRetry.getDurationMs());
                    parsedRetry.put("retry_stage", "corrective_prompt");
                    parsedRetry.put("sanitized_before_parse", false);
                    return parsedRetry;

                } catch (Exception secondParseError) {
                    log.warn("DEBUG LLM: corrective retry parse failed: {}", secondParseError.getMessage());

                    try {
                        Map<String, Object> parsedRetrySanitized =
                                parseContentWithSanitization(correctiveRetry.getContent());

                        parsedRetrySanitized.put("selected_model", correctiveRetry.getModel());
                        parsedRetrySanitized.put("duration_ms", correctiveRetry.getDurationMs());
                        parsedRetrySanitized.put("retry_stage", "corrective_prompt");
                        parsedRetrySanitized.put("sanitized_before_parse", true);
                        log.warn("DEBUG LLM: corrective retry succeeded after sanitization");
                        return parsedRetrySanitized;

                    } catch (Exception secondSanitizedParseError) {
                        return fallback(
                                "The LLM returned invalid JSON after corrective retry.",
                                correctiveRetry,
                                correctiveRetry.getContent(),
                                secondSanitizedParseError.getMessage(),
                                "parse_failed_after_corrective_retry"
                        );
                    }
                }
            }
        }
    }

    private LlmResponse callModel(String prompt) {
        log.info("A testar modelo único: {}", DEFAULT_MODEL);

        LlmResponse response = ollamaClient.chat(DEFAULT_MODEL, prompt);

        if (response == null) {
            log.error("Resposta nula do modelo {}", DEFAULT_MODEL);
            return null;
        }

        if (response.hasError()) {
            log.error("Erro no modelo {}: {}", DEFAULT_MODEL, response.getError());
            log.error("HTTP Status: {}", response.getHttpStatusCode());
            log.error("Raw response: {}", response.getRawResponse());
        } else {
            log.info("Teste concluído para {} em {} ms", DEFAULT_MODEL, response.getDurationMs());
            log.debug("LLM raw content: {}", response.getContent());
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

        return normalizeParsedMap(parsed);
    }

    private Map<String, Object> parseContentWithSanitization(String content) throws Exception {
        String cleaned = extractJsonObject(stripMarkdownCodeFences(content));
        String sanitized = sanitizeJsonText(cleaned);

        log.debug(
                "DEBUG LLM sanitized JSON preview: {}",
                sanitized.length() > 1000 ? sanitized.substring(0, 1000) + "..." : sanitized
        );

        Map<String, Object> parsed = objectMapper.readValue(
                sanitized,
                new TypeReference<Map<String, Object>>() {}
        );

        return normalizeParsedMap(parsed);
    }

    private Map<String, Object> normalizeParsedMap(Map<String, Object> parsed) {
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("algorithms", parsed.getOrDefault("algorithms", List.of()));
        normalized.put("justification", parsed.getOrDefault("justification", ""));
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

    private String sanitizeJsonText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                result.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escaping = true;
                continue;
            }

            if (c == '"') {
                result.append(c);
                inString = !inString;
                continue;
            }

            if (inString) {
                if (c == '\r') {
                    result.append("\\r");
                    continue;
                }
                if (c == '\n') {
                    result.append("\\n");
                    continue;
                }
                if (c == '\t') {
                    result.append("\\t");
                    continue;
                }
                if (c < 0x20) {
                    result.append(String.format("\\u%04x", (int) c));
                    continue;
                }
            }

            result.append(c);
        }

        return result.toString();
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
        Keep "justification" as a single paragraph.
        Every string value must stay on one physical line.
        Escape any newline, carriage return, or tab as \\n, \\r, or \\t.
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
        result.put("algorithms", List.of());
        result.put("justification", message);
        result.put("selected_model", response != null ? response.getModel() : DEFAULT_MODEL);
        result.put("duration_ms", response != null ? response.getDurationMs() : null);
        result.put("retry_stage", retryStage);
        result.put("sanitized_before_parse", false);
        result.put("parsing_error", parsingError);
        result.put("raw_llm_response", rawLlmResponse);
        result.put("llm_error", response != null ? response.getError() : null);
        result.put("http_status_code", response != null ? response.getHttpStatusCode() : null);
        return result;
    }
}