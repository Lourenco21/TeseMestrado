package pt.lourenco.optimization.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public OllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public LlmResponse chat(String model, String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();

        String requestBody = """
                {
                  "model": "%s",
                  "stream": false,
                  "messages": [
                    {
                      "role": "system",
                      "content": %s
                    },
                    {
                      "role": "user",
                      "content": %s
                    }
                  ]
                }
                """.formatted(
                escapeJson(model),
                toJsonString(systemPrompt),
                toJsonString(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            String body = response.body();
            String content = extractAssistantContent(body);

            return new LlmResponse(
                    model,
                    content,
                    response.statusCode(),
                    duration,
                    body,
                    response.statusCode() >= 400 ? "Erro HTTP na chamada ao Ollama." : null
            );
        } catch (IOException | InterruptedException e) {
            long duration = System.currentTimeMillis() - start;
            Thread.currentThread().interrupt();

            return new LlmResponse(
                    model,
                    null,
                    500,
                    duration,
                    null,
                    "Erro ao comunicar com o Ollama: " + e.getMessage()
            );
        }
    }

    private String extractAssistantContent(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }

        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start == -1) {
            return json;
        }

        start += marker.length();
        StringBuilder content = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                switch (c) {
                    case 'n' -> content.append('\n');
                    case 't' -> content.append('\t');
                    case 'r' -> content.append('\r');
                    case '"' -> content.append('"');
                    case '\\' -> content.append('\\');
                    default -> content.append(c);
                }
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                break;
            }

            content.append(c);
        }

        return content.toString();
    }

    private String toJsonString(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}