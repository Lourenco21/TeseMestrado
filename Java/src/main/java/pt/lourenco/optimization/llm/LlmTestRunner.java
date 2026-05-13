package pt.lourenco.optimization.llm;

/**public class LlmTestRunner {

    public static void main(String[] args) {
        PromptLoader promptLoader = new PromptLoader();
        OllamaClient ollamaClient = new OllamaClient("http://localhost:11434");
        ResultWriter resultWriter = new ResultWriter("llm_test_outputs");

        LlmTestService llmTestService = new LlmTestService(
                promptLoader,
                ollamaClient,
                resultWriter
        );

        llmTestService.runTests();
        System.out.println("Testes concluídos.");
    }
}**/