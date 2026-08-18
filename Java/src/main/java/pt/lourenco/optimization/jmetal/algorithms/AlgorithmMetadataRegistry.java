package pt.lourenco.optimization.jmetal.algorithms;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlgorithmMetadataRegistry {

    private final Map<String, AlgorithmMetadataProvider> providersByKey;

    public AlgorithmMetadataRegistry(List<AlgorithmMetadataProvider> providers) {
        System.out.println("Providers encontrados: " + providers.size());
        this.providersByKey = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getAlgorithmKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase(),
                        Function.identity()
                ));
    }

    public AlgorithmMetadataProvider getByName(String algorithmName) {
        if (algorithmName == null || algorithmName.isBlank()) {
            throw new IllegalArgumentException("Selected algorithm was not provided.");
        }

        AlgorithmMetadataProvider provider = providersByKey.get(algorithmName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase());

        if (provider == null) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithmName);
        }

        return provider;
    }
}
