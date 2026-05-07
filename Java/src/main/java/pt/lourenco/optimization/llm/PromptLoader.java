package pt.lourenco.optimization.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class PromptLoader {

    public String loadFromResources(String resourcePath) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalArgumentException("Ficheiro não encontrado em resources: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }

            return content.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler ficheiro de prompt: " + resourcePath, e);
        }
    }
}