package pt.lourenco.optimization.llm;

import java.util.List;

public class LlmTestService {

    private final PromptLoader promptLoader;
    private final OllamaClient ollamaClient;
    private final ResultWriter resultWriter;

    public LlmTestService(PromptLoader promptLoader, OllamaClient ollamaClient, ResultWriter resultWriter) {
        this.promptLoader = promptLoader;
        this.ollamaClient = ollamaClient;
        this.resultWriter = resultWriter;
    }

    public void runTests() {
        String systemPrompt = promptLoader.loadFromResources("prompts/system-prompt.txt");
        String userPrompt = promptLoader.loadFromResources("prompts/user-prompt-teste.txt");

        List<String> models = List.of(
                "llama3.1:8b",
                "deepseek-r1:8b",
                "qwen2.5-coder:7b"
        );

        for (String model : models) {
            System.out.println("A testar modelo: " + model);

            LlmResponse response = ollamaClient.chat(model, systemPrompt, userPrompt);

            resultWriter.writeResult(response);

            if (response.hasError()) {
                System.out.println("Erro no modelo " + model + ": " + response.getError());
            } else {
                System.out.println("Teste concluído para " + model + " em " + response.getDurationMs() + " ms");
            }
        }
    }
}