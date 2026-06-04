package pt.lourenco.optimization.jmetal.problems.problems;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScheduleOptimizationProblem extends AbstractIntegerProblem {

    private static final AtomicInteger EVALUATION_COUNTER = new AtomicInteger(0);

    private final ProblemInputData inputData;
    private final SolutionContextBuilderService solutionContextBuilderService;
    private final ConstraintEvaluationService constraintEvaluationService;

    private final List<Map<String, Object>> classes;
    private final List<Map<String, Object>> rooms;
    private final int numberOfVariables;
    private final int numberOfObjectives;
    private final int numberOfConstraints;

    public ScheduleOptimizationProblem(
            ProblemInputData inputData,
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService
    ) {
        this.inputData = inputData;
        this.solutionContextBuilderService = solutionContextBuilderService;
        this.constraintEvaluationService = constraintEvaluationService;

        this.name("ScheduleOptimizationProblem");

        this.classes = extractClasses(inputData.getScheduleData());
        this.rooms = extractRooms(inputData.getRoomsData());

        this.numberOfVariables = classes.size();
        this.numberOfObjectives = 1;
        this.numberOfConstraints = countHardConstraints(inputData.getSelectedConstraints());

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

        int evaluationNumber = EVALUATION_COUNTER.incrementAndGet();

        List<ClassRoomAssignment> assignments = buildAssignments(solution);

        SolutionContext context = solutionContextBuilderService.buildFromProblemInput(inputData);
        context.setAssignments(assignments);

        List<UserConstraintSelection> selectedConstraints = inputData.getSelectedConstraints();
        ConstraintEvaluationResult evaluationResult =
                constraintEvaluationService.evaluate(context, selectedConstraints);

        double softPenalty = evaluationResult.getSoftScore() == null
                ? 0.0
                : evaluationResult.getSoftScore();

        solution.objectives()[0] = softPenalty;

        double[] hardViolations = extractHardConstraintViolations(evaluationResult);
        for (int i = 0; i < solution.constraints().length && i < hardViolations.length; i++) {
            solution.constraints()[i] = hardViolations[i];
        }

        return solution;
    }

    private List<Integer> buildLowerBounds() {
        List<Integer> lowerBounds = new ArrayList<>();
        for (int i = 0; i < numberOfVariables; i++) {
            lowerBounds.add(0);
        }
        return lowerBounds;
    }

    private List<Integer> buildUpperBounds() {
        List<Integer> upperBounds = new ArrayList<>();
        int upperBound = rooms.size() - 1;

        for (int i = 0; i < numberOfVariables; i++) {
            upperBounds.add(upperBound);
        }
        return upperBounds;
    }

    private List<ClassRoomAssignment> buildAssignments(IntegerSolution solution) {
        List<ClassRoomAssignment> assignments = new ArrayList<>();

        for (int i = 0; i < solution.variables().size(); i++) {
            int roomIndex = solution.variables().get(i);

            Map<String, Object> classData = classes.get(i);
            Map<String, Object> roomData = rooms.get(roomIndex);

            assignments.add(new ClassRoomAssignment(i, classData, roomIndex, roomData));
        }

        return assignments;
    }

    private Object buildSoftObjectiveContext(List<ClassRoomAssignment> assignments) {
        return assignments;
    }

    private Object buildHardConstraintContext(List<ClassRoomAssignment> assignments) {
        return assignments;
    }

    private double[] extractHardConstraintViolations(ConstraintEvaluationResult evaluationResult) {
        if (numberOfConstraints == 0) {
            return new double[0];
        }

        double[] violations = new double[numberOfConstraints];

        if (evaluationResult == null || evaluationResult.getConstraintResults() == null) {
            return violations;
        }

        int index = 0;
        for (var item : evaluationResult.getConstraintResults()) {
            if (item.getGoal() != null && item.getGoal().name().equalsIgnoreCase("HARD")) {
                if (index < violations.length) {
                    violations[index] = item.getWeightedScore() == null ? 0.0 : item.getWeightedScore();
                    index++;
                }
            }
        }

        return violations;
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
}