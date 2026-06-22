package pt.lourenco.optimization.jmetal.problems.problems;

import lombok.extern.slf4j.Slf4j;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.metrics.PartitionMetrics;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScheduleOptimizationProblem extends AbstractIntegerProblem {

    private static final AtomicInteger EVALUATION_COUNTER = new AtomicInteger(0);

    private final ProblemInputData inputData;
    private final ConstraintEvaluationService constraintEvaluationService;

    private final List<Map<String, Object>> classes;
    private final List<Map<String, Object>> rooms;
    private final List<UserConstraintSelection> selectedConstraints;
    private final SolutionContext baseContext;

    private final int numberOfVariables;
    private final int numberOfObjectives;
    private final int numberOfConstraints;

    private final PartitionMetrics partitionMetrics = new PartitionMetrics();

    public ScheduleOptimizationProblem(
            ProblemInputData inputData,
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService
    ) {
        this.inputData = inputData;
        this.constraintEvaluationService = constraintEvaluationService;

        this.name("ScheduleOptimizationProblem");

        this.classes = extractClasses(inputData.getScheduleData());
        this.rooms = extractRooms(inputData.getRoomsData());
        this.selectedConstraints = inputData.getSelectedConstraints() == null
                ? List.of()
                : inputData.getSelectedConstraints();

        this.baseContext = solutionContextBuilderService.buildFromProblemInput(inputData);

        this.numberOfVariables = classes.size();
        this.numberOfObjectives = 1;
        this.numberOfConstraints = countHardConstraints(this.selectedConstraints);

        variableBounds(buildLowerBounds(), buildUpperBounds());
        numberOfObjectives(this.numberOfObjectives);
        numberOfConstraints(this.numberOfConstraints);

        if (classes == null || classes.isEmpty()) {
            throw new IllegalArgumentException("Schedule data must contain at least one class.");
        }

        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Rooms data must contain at least one room.");
        }

        Object classesObject = inputData.getScheduleData().get("classes");
        if (classesObject instanceof List<?> classes) {
            log.info("Classes in problem input: {}", classes.size());
        } else {
            log.warn("Classes in problem input are missing or invalid");
        }

        log.info("Problem numberOfVariables: {}", numberOfVariables());
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        long evaluateStartNs = System.nanoTime();
        partitionMetrics.incrementEvaluationCount();

        long buildAssignmentsStartNs = System.nanoTime();
        List<ClassRoomAssignment> assignments = buildAssignments(solution);
        partitionMetrics.addBuildAssignmentsTime(System.nanoTime() - buildAssignmentsStartNs);

        baseContext.setAssignments(assignments);

        long constraintEvalStartNs = System.nanoTime();
        ConstraintEvaluationResult evaluationResult =
                constraintEvaluationService.evaluate(baseContext, selectedConstraints);
        partitionMetrics.addConstraintEvaluationTime(System.nanoTime() - constraintEvalStartNs);

        double softPenalty = evaluationResult.getSoftScore() == null
                ? 0.0
                : evaluationResult.getSoftScore();

        solution.objectives()[0] = softPenalty;

        fillHardConstraintViolations(solution, evaluationResult);

        partitionMetrics.addEvaluateTime(System.nanoTime() - evaluateStartNs);

        return solution;
    }

    private void fillHardConstraintViolations(
            IntegerSolution solution,
            ConstraintEvaluationResult evaluationResult
    ) {
        double[] constraintArray = solution.constraints();

        for (int i = 0; i < constraintArray.length; i++) {
            constraintArray[i] = 0.0;
        }

        if (evaluationResult == null || evaluationResult.getConstraintResults() == null) {
            return;
        }

        int index = 0;
        for (var item : evaluationResult.getConstraintResults()) {
            if (item.getGoal() != null && item.getGoal().name().equalsIgnoreCase("HARD")) {
                if (index < constraintArray.length) {
                    double weightedScore = item.getWeightedScore() == null ? 0.0 : item.getWeightedScore();
                    constraintArray[index] = weightedScore > 0.0 ? -Math.abs(weightedScore) : 0.0;
                    index++;
                }
            }
        }
    }

    private List<Integer> buildLowerBounds() {
        List<Integer> lowerBounds = new ArrayList<>(numberOfVariables);
        for (int i = 0; i < numberOfVariables; i++) {
            lowerBounds.add(0);
        }
        return lowerBounds;
    }

    private List<Integer> buildUpperBounds() {
        List<Integer> upperBounds = new ArrayList<>(numberOfVariables);
        int upperBound = rooms.size() - 1;

        for (int i = 0; i < numberOfVariables; i++) {
            upperBounds.add(upperBound);
        }
        return upperBounds;
    }

    private List<ClassRoomAssignment> buildAssignments(IntegerSolution solution) {
        int size = solution.variables().size();
        List<ClassRoomAssignment> assignments = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            int roomIndex = solution.variables().get(i);
            assignments.add(new ClassRoomAssignment(
                    i,
                    classes.get(i),
                    roomIndex,
                    rooms.get(roomIndex)
            ));
        }

        return assignments;
    }

    private int countHardConstraints(List<UserConstraintSelection> selectedConstraints) {
        if (selectedConstraints == null || selectedConstraints.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UserConstraintSelection selection : selectedConstraints) {
            if (selection.getGoal() != null && selection.getGoal().name().equalsIgnoreCase("HARD")) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractClasses(Map<String, Object> scheduleData) {
        Object classesObject = scheduleData.get("classes");
        return (List<Map<String, Object>>) classesObject;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRooms(Map<String, Object> roomsData) {
        Object roomsObject = roomsData.get("rooms");
        return (List<Map<String, Object>>) roomsObject;
    }

    public PartitionMetrics getPartitionMetrics() {
        return partitionMetrics;
    }
}