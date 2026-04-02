package pt.lourenco.optimizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class TestLlm {
    public static void main(String[] args) {
        // Cria instância manual (sem Spring)
        var restTemplate = new org.springframework.web.client.RestTemplate();
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        LlmService llmService = new LlmService(restTemplate, objectMapper);

        try {
            // Testa a tua função askBestAlgorithm TAL COMO ESTÁ
            JsonNode result = llmService.askBestAlgorithm("");

            System.out.println("✅ askBestAlgorithm funcionou!");
            System.out.println("Resposta LLM: " + result.asText());

        } catch (Exception e) {
            System.err.println("❌ Erro na askBestAlgorithm: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
