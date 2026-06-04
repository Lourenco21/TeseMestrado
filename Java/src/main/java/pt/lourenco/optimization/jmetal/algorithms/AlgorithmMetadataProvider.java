package pt.lourenco.optimization.jmetal.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;

public interface AlgorithmMetadataProvider {
    String getAlgorithmKey();
    String getDisplayName();
    String getParametersList();
    String getParametersJson() throws JsonProcessingException;
    String getOperatorsDescription();
    String getCoherenceRule();
    default List<String> getRequiredParameterKeys() {
        return List.of();
    }
    default Map<String, Object> getDefaultParameterValues() {
        return Map.of();
    }
}
