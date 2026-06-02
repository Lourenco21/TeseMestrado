package pt.lourenco.optimization.jmetal.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Nsgaii implements AlgorithmMetadataProvider{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**public static Map<String, Object> runNsgaii(
            Map<String, Object> dataset,
            Map<String, Object> variables,
            Map<String, Object> objectives,
            Map<String, Object> constraints,
            Map<String, Object> inputParameters
    ) throws JsonProcessingException {

        ScheduleProblem problem = new ScheduleProblem(
                dataset,
                variables,
                objectives,
                constraints
        );

        int numberOfVariables = problem.numberOfVariables();

        int populationSize = getRequiredInt(inputParameters, "populationSize");
        int maxEvaluations = getRequiredInt(inputParameters, "maxEvaluations");
        double crossoverProbability = getRequiredDouble(inputParameters, "crossoverProbability");
        double etaC = getRequiredDouble(inputParameters, "etaC");
        double etaM = getRequiredDouble(inputParameters, "etaM");

        double mutationProbability = getDoubleOrDefault(
                inputParameters,
                "mutationProbability",
                numberOfVariables > 0 ? 1.0 / numberOfVariables : 0.01
        );

        validateParameters(
                populationSize,
                maxEvaluations,
                crossoverProbability,
                mutationProbability,
                etaC,
                etaM
        );

        IntegerPolynomialMutation mutation =
                new IntegerPolynomialMutation(mutationProbability, etaM);

        IntegerSBXCrossover crossover =
                new IntegerSBXCrossover(crossoverProbability, etaC);

        NSGAII<IntegerSolution> algorithm = new NSGAIIBuilder<>(
                problem,
                crossover,
                mutation,
                populationSize
        )
                .setSelectionOperator(
                        new BinaryTournamentSelection<>(
                                new RankingAndCrowdingDistanceComparator<>()
                        )
                )
                .setMaxEvaluations(maxEvaluations)
                .build();

        algorithm.run();

        List<IntegerSolution> resultPopulation = algorithm.result();

        List<Map<String, Object>> solutions = new ArrayList<>();

        for (IntegerSolution solution : resultPopulation) {
            Map<String, Object> serializedSolution = new LinkedHashMap<>();
            serializedSolution.put("solution", new ArrayList<>(solution.variables()));
            serializedSolution.put("objectives", toDoubleList(solution.objectives()));
            serializedSolution.put("constraints", toDoubleList(solution.constraints()));
            solutions.add(serializedSolution);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", "NSGAII");
        response.put("usedParameters", buildUsedParametersMap(
                populationSize,
                maxEvaluations,
                crossoverProbability,
                mutationProbability,
                etaC,
                etaM
        ));
        response.put("solutionCount", solutions.size());
        response.put("solutions", solutions);

        return response;
    }**/

    public static String getRequiredParametersAsPromptList() {
        return """
                1. populationSize
                2. maxEvaluations
                3. crossoverProbability
                4. mutationProbability
                5. etaC
                6. etaM
                """;
    }

    private static Map<String, Object> buildUsedParametersMap(
            int populationSize,
            int maxEvaluations,
            double crossoverProbability,
            double mutationProbability,
            double etaC,
            double etaM
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("populationSize", populationSize);
        parameters.put("maxEvaluations", maxEvaluations);
        parameters.put("crossoverProbability", crossoverProbability);
        parameters.put("mutationProbability", mutationProbability);
        parameters.put("etaC", etaC);
        parameters.put("etaM", etaM);
        return parameters;
    }

    private static List<Double> toDoubleList(double[] values) {
        List<Double> result = new ArrayList<>();
        for (double value : values) {
            result.add(value);
        }
        return result;
    }

    private static int getRequiredInt(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(value.toString());
    }

    private static double getRequiredDouble(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(value.toString());
    }

    private static double getDoubleOrDefault(Map<String, Object> parameters, String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(value.toString());
    }

    private static void validateParameters(
            int populationSize,
            int maxEvaluations,
            double crossoverProbability,
            double mutationProbability,
            double etaC,
            double etaM
    ) {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("populationSize must be > 0");
        }

        if (maxEvaluations <= 0) {
            throw new IllegalArgumentException("maxEvaluations must be > 0");
        }

        if (crossoverProbability < 0.0 || crossoverProbability > 1.0) {
            throw new IllegalArgumentException("crossoverProbability must be between 0 and 1");
        }

        if (mutationProbability < 0.0 || mutationProbability > 1.0) {
            throw new IllegalArgumentException("mutationProbability must be between 0 and 1");
        }

        if (etaC <= 0.0) {
            throw new IllegalArgumentException("etaC must be > 0");
        }

        if (etaM <= 0.0) {
            throw new IllegalArgumentException("etaM must be > 0");
        }
    }

    @Override
    public String getAlgorithmKey() {
        return "NSGA-II";
    }

    @Override
    public String getDisplayName() {
        return "NSGA-II";
    }

    @Override
    public String getParametersList() {
        return """
                1. populationSize
                2. maxEvaluations
                3. crossoverProbability
                4. mutationProbability
                5. etaC
                6. etaM
                """;
    }

    @Override
    public String getParametersJson() throws JsonProcessingException {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("populationSize", null);
        template.put("maxEvaluations", null);
        template.put("crossoverProbability", null);
        template.put("mutationProbability", null);
        template.put("etaC", null);
        template.put("etaM", null);
        template.put("Justification", "");
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(template);
    }

    @Override
    public String getOperatorsDescription() {
        return "- The intended implementation uses SBX crossover and Polynomial Mutation.";
    }

    @Override
    public String getCoherenceRule() {
        return "- The selected values must be coherent with NSGA-II, SBX crossover, and Polynomial Mutation.";
    }
}