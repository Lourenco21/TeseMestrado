package pt.lourenco.optimization.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResultWriter {

    private final Path outputDirectory;

    public ResultWriter(String outputDirectory) {
        this.outputDirectory = Path.of(outputDirectory);
        createDirectoryIfNeeded();
    }

    public void writeResult(LlmResponse response) {
        String safeModelName = response.getModel().replace(":", "-");
        Path outputFile = outputDirectory.resolve(safeModelName + ".txt");

        StringBuilder content = new StringBuilder();
        content.append("Modelo: ").append(response.getModel()).append(System.lineSeparator());
        content.append("HTTP Status: ").append(response.getHttpStatusCode()).append(System.lineSeparator());
        content.append("Duração (ms): ").append(response.getDurationMs()).append(System.lineSeparator());
        content.append("Erro: ").append(response.getError() == null ? "nenhum" : response.getError()).append(System.lineSeparator());
        content.append(System.lineSeparator());
        content.append("===== RESPOSTA =====").append(System.lineSeparator());
        content.append(response.getContent() == null ? "" : response.getContent()).append(System.lineSeparator());
        content.append(System.lineSeparator());
        content.append("===== RAW RESPONSE =====").append(System.lineSeparator());
        content.append(response.getRawResponse() == null ? "" : response.getRawResponse()).append(System.lineSeparator());

        try {
            Files.writeString(outputFile, content.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao escrever resultado do modelo " + response.getModel(), e);
        }
    }

    private void createDirectoryIfNeeded() {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de output: " + outputDirectory, e);
        }
    }
}