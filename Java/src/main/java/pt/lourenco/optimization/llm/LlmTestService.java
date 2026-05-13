package pt.lourenco.optimization.llm;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LlmTestService {

    private final ResultWriter resultWriter;
    private final OllamaClient ollamaClient;

    public LlmTestService() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.resultWriter = new ResultWriter("llm-test-outputs");
    }

    public List<LlmResponse> runTestsWithPrompt(String prompt) {
        String algorithm = "algorithm";
        String code = "code";
        String parameters = "parameters";

        List<String> models = List.of(
                "llama3.1:8b",
                "deepseek-r1:8b",
                "qwen2.5-coder:7b"
        );

        List<LlmResponse> results = new ArrayList<>();

        for (String model : models) {
            System.out.println("A testar modelo: " + model);

            LlmResponse response = ollamaClient.chat(model, prompt);
            resultWriter.writeResult(response, /**algorithm code **/parameters);
            results.add(response);

            if (response.hasError()) {
                System.out.println("Erro no modelo " + model + ": " + response.getError());
                System.out.println("HTTP Status: " + response.getHttpStatusCode());
                System.out.println("Raw response: " + response.getRawResponse());
            } else {
                System.out.println("Teste concluído para " + model + " em " + response.getDurationMs() + " ms");
            }
        }

        return results;
    }
}