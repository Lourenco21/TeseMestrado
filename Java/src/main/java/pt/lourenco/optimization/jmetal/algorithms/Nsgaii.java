package pt.lourenco.optimization.jmetal.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.IncrementalConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.metrics.PartitionMetrics;
import pt.lourenco.optimization.jmetal.partitioning.GlobalRoomOccupationTracker;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.problems.problems.ScheduleOptimizationProblem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Nsgaii implements AlgorithmMetadataProvider, AlgorithmExecutor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SolutionContextBuilderService solutionContextBuilderService;
    private final ConstraintEvaluationService constraintEvaluationService;
    private final IncrementalConstraintEvaluationService incrementalConstraintEvaluationService;

    public Nsgaii(
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService,
            IncrementalConstraintEvaluationService incrementalConstraintEvaluationService
    ) {
        this.solutionContextBuilderService = solutionContextBuilderService;
        this.constraintEvaluationService = constraintEvaluationService;
        this.incrementalConstraintEvaluationService = incrementalConstraintEvaluationService;
    }

    public Map<String, Object> run(ProblemInputData inputData, Map<String, Object> inputParameters) {
        long algorithmStartNs = System.nanoTime();

        Object trackerObj = inputData.getMetadata() == null
                ? null
                : inputData.getMetadata().get("globalRoomOccupationTracker");

        GlobalRoomOccupationTracker globalRoomOccupationTracker =
                trackerObj instanceof GlobalRoomOccupationTracker tracker ? tracker : null;

        ScheduleOptimizationProblem problem = new ScheduleOptimizationProblem(
                inputData,
                solutionContextBuilderService,
                constraintEvaluationService,
                incrementalConstraintEvaluationService,
                globalRoomOccupationTracker
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
                .setMaxEvaluations(50000/*maxEvaluations*/)
                .build();

        algorithm.run();

        long algorithmDurationMs = (System.nanoTime() - algorithmStartNs) / 1_000_000L;

        List<IntegerSolution> resultPopulation = algorithm.result();
        List<Map<String, Object>> solutions = new ArrayList<>();

        for (IntegerSolution solution : resultPopulation) {
            List<Map<String, Object>> decodedAssignments = decodeAssignments(solution, inputData);
            Map<String, Object> evaluationDetails = evaluateDecodedAssignments(decodedAssignments, inputData);

            Map<String, Object> serializedSolution = new LinkedHashMap<>();
            serializedSolution.put("solution", new ArrayList<>(solution.variables()));
            serializedSolution.put("objectives", toDoubleList(solution.objectives()));
            serializedSolution.put("constraints", toDoubleList(solution.constraints()));
            serializedSolution.put("assignments", decodedAssignments);
            serializedSolution.put("constraintValues", evaluationDetails.get("constraintValues"));
            serializedSolution.put("penaltySummary", evaluationDetails.get("penaltySummary"));
            solutions.add(serializedSolution);
        }

        Map<String, Object> metrics = buildProblemMetrics(problem, algorithmDurationMs);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", "NSGA-II");
        response.put("usedParameters", buildUsedParametersMap(
                populationSize,
                maxEvaluations,
                crossoverProbability,
                mutationProbability,
                etaC,
                etaM
        ));
        response.put("algorithmDurationMs", algorithmDurationMs);
        response.put("metrics", metrics);
        response.put("solutionCount", solutions.size());
        response.put("solutions", solutions);

        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> decodeAssignments(IntegerSolution solution, ProblemInputData inputData) {
        List<Map<String, Object>> decoded = new ArrayList<>();

        Object classesObject = inputData.getScheduleData().get("classes");
        Object roomsObject = inputData.getRoomsData().get("rooms");

        if (!(classesObject instanceof List<?> rawClasses) || !(roomsObject instanceof List<?> rawRooms)) {
            return decoded;
        }

        for (int i = 0; i < solution.variables().size(); i++) {
            int roomIndex = solution.variables().get(i);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("classIndex", i);
            row.put("classData", rawClasses.get(i));
            row.put("roomIndex", roomIndex);
            row.put("roomData", rawRooms.get(roomIndex));
            decoded.add(row);
        }

        return decoded;
    }

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
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(template);
    }

    @Override
    public String getOperatorsDescription() {
        return "- The intended implementation uses SBX crossover and Polynomial Mutation.";
    }

    @Override
    public String getCoherenceRule() {
        return "- The selected values must be coherent with NSGA-II, SBX crossover, and Polynomial Mutation.";
    }

    @Override
    public List<String> getRequiredParameterKeys() {
        return List.of(
                "populationSize",
                "maxEvaluations",
                "crossoverProbability",
                "mutationProbability",
                "etaC",
                "etaM"
        );
    }

    @Override
    public Map<String, Object> getDefaultParameterValues() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("populationSize", 100);
        defaults.put("maxEvaluations", 1000);
        defaults.put("crossoverProbability", 0.9);
        defaults.put("mutationProbability", 0.01);
        defaults.put("etaC", 20.0);
        defaults.put("etaM", 20.0);
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateDecodedAssignments(
            List<Map<String, Object>> decodedAssignments,
            ProblemInputData inputData
    ) {
        List<ClassRoomAssignment> assignments = new ArrayList<>();

        for (Map<String, Object> decodedAssignment : decodedAssignments) {
            Object classIndexObject = decodedAssignment.get("classIndex");
            Object roomIndexObject = decodedAssignment.get("roomIndex");
            Object classDataObject = decodedAssignment.get("classData");
            Object roomDataObject = decodedAssignment.get("roomData");

            if (!(classIndexObject instanceof Number classIndexNumber)) continue;
            if (!(roomIndexObject instanceof Number roomIndexNumber)) continue;
            if (!(classDataObject instanceof Map<?, ?> rawClassData)) continue;
            if (!(roomDataObject instanceof Map<?, ?> rawRoomData)) continue;

            assignments.add(new ClassRoomAssignment(
                    classIndexNumber.intValue(),
                    (Map<String, Object>) rawClassData,
                    roomIndexNumber.intValue(),
                    (Map<String, Object>) rawRoomData
            ));
        }

        var context = solutionContextBuilderService.buildFromProblemInput(inputData);
        context.setAssignments(assignments);

        var evaluationResult = constraintEvaluationService.evaluate(context, inputData.getSelectedConstraints());

        Map<String, Object> constraintValues = new LinkedHashMap<>();

        double softRawTotal = 0.0;
        double softWeightedTotal = 0.0;
        double hardRawTotal = 0.0;
        double hardWeightedTotal = 0.0;

        if (evaluationResult.getConstraintResults() != null) {
            for (var result : evaluationResult.getConstraintResults()) {
                double raw = result.getViolationScore() == null ? 0.0 : result.getViolationScore();
                double weighted = result.getWeightedScore() == null ? 0.0 : result.getWeightedScore();
                String goal = result.getGoal() == null ? null : result.getGoal().name();

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("goal", goal);
                item.put("raw", raw);
                item.put("weighted", weighted);

                constraintValues.put(result.getConstraintId(), item);

                if ("HARD".equalsIgnoreCase(goal)) {
                    hardRawTotal += raw;
                    hardWeightedTotal += weighted;
                } else {
                    softRawTotal += raw;
                    softWeightedTotal += weighted;
                }
            }
        }

        Map<String, Object> penaltySummary = new LinkedHashMap<>();
        penaltySummary.put("soft_raw_total", softRawTotal);
        penaltySummary.put("soft_weighted_total", softWeightedTotal);
        penaltySummary.put("hard_raw_total", hardRawTotal);
        penaltySummary.put("hard_weighted_total", hardWeightedTotal);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("constraintValues", constraintValues);
        response.put("penaltySummary", penaltySummary);
        return response;
    }

    private Map<String, Object> buildProblemMetrics(
            ScheduleOptimizationProblem problem,
            long algorithmDurationMs
    ) {
        PartitionMetrics partitionMetrics = problem.getPartitionMetrics();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("algorithmDurationMs", algorithmDurationMs);
        metrics.put("evaluationCount", partitionMetrics.getEvaluationCount());
        metrics.put("totalEvaluateTimeMs", partitionMetrics.getTotalEvaluateTimeMs());
        metrics.put("averageEvaluateTimeMs", partitionMetrics.getAverageEvaluateTimeMs());
        metrics.put("totalBuildAssignmentsTimeMs", partitionMetrics.getTotalBuildAssignmentsTimeMs());
        metrics.put("totalConstraintEvaluationTimeMs", partitionMetrics.getTotalConstraintEvaluationTimeMs());

        long otherTimeMs = algorithmDurationMs - partitionMetrics.getTotalEvaluateTimeMs();
        metrics.put("otherAlgorithmTimeMs", Math.max(otherTimeMs, 0L));

        return metrics;
    }
}