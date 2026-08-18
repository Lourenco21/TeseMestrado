package pt.lourenco.optimization.jmetal.algorithms;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlgorithmExecutionRegistry {

    private final Map<String, AlgorithmExecutor> executorsByKey;

    public AlgorithmExecutionRegistry(List<AlgorithmExecutor> executors) {
        this.executorsByKey = executors.stream()
                .collect(Collectors.toMap(
                        e -> e.getAlgorithmKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase(),
                        Function.identity()
                ));
    }

    public AlgorithmExecutor getByName(String algorithmName) {
        if (algorithmName == null || algorithmName.isBlank()) {
            throw new IllegalArgumentException("Selected algorithm was not provided.");
        }
        
        AlgorithmExecutor executor = executorsByKey.get(algorithmName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase());

        if (executor == null) {
            throw new IllegalArgumentException("Unsupported algorithm execution: " + algorithmName);
        }

        return executor;
    }
}