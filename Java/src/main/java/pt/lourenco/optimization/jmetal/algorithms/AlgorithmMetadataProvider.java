package pt.lourenco.optimization.jmetal.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface AlgorithmMetadataProvider {
    String getAlgorithmKey();
    String getDisplayName();
    String getParametersList();
    String getParametersJson() throws JsonProcessingException;
    String getOperatorsDescription();
    String getCoherenceRule();
}
