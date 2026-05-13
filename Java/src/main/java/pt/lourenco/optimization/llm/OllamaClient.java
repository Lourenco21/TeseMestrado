package pt.lourenco.optimization.llm;

import pt.lourenco.optimization.llm.dto.OllamaChatRequest;
import pt.lourenco.optimization.llm.dto.OllamaChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public OllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public LlmResponse chat(String model, String prompt) {
        long start = System.currentTimeMillis();

        OllamaChatRequest requestPayload = new OllamaChatRequest(
                model,
                false,
                List.of(
                        new OllamaChatRequest.Message("user", prompt)
                ),
                0
        );

        try {
            String requestBody = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            String rawBody = response.body();

            if (response.statusCode() >= 400) {
                return new LlmResponse(
                        model,
                        null,
                        response.statusCode(),
                        duration,
                        rawBody,
                        "Erro HTTP na chamada ao Ollama."
                );
            }

            OllamaChatResponse ollamaResponse = objectMapper.readValue(rawBody, OllamaChatResponse.class);
            String content = extractContent(ollamaResponse);

            return new LlmResponse(
                    model,
                    content,
                    response.statusCode(),
                    duration,
                    rawBody,
                    null
            );

        } catch (JsonProcessingException e) {
            long duration = System.currentTimeMillis() - start;
            return new LlmResponse(
                    model,
                    null,
                    500,
                    duration,
                    null,
                    "Erro a serializar/desserializar JSON: " + e.getMessage()
            );
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - start;
            return new LlmResponse(
                    model,
                    null,
                    500,
                    duration,
                    null,
                    "Erro de I/O ao comunicar com o Ollama: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            long duration = System.currentTimeMillis() - start;
            Thread.currentThread().interrupt();
            return new LlmResponse(
                    model,
                    null,
                    500,
                    duration,
                    null,
                    "Thread interrompida durante chamada ao Ollama: " + e.getMessage()
            );
        }
    }

    private String extractContent(OllamaChatResponse response) {
        if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
            return "";
        }
        return response.getMessage().getContent();
    }
}