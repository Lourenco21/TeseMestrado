package pt.lourenco.optimization.jmetal.algorithms;

import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.Map;

public interface AlgorithmExecutor {
    String getAlgorithmKey();
    Map<String, Object> run(ProblemInputData inputData, Map<String, Object> inputParameters);
}