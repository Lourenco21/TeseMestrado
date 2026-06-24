package pt.lourenco.optimization.jmetal.problems.problems;

import lombok.extern.slf4j.Slf4j;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.IncrementalConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.metrics.PartitionMetrics;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ScheduleOptimizationProblem extends AbstractIntegerProblem {

    private final ProblemInputData inputData;
    private final ConstraintEvaluationService constraintEvaluationService;
    private final IncrementalConstraintEvaluationService incrementalConstraintEvaluationService;

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
            ConstraintEvaluationService constraintEvaluationService,
            IncrementalConstraintEvaluationService incrementalConstraintEvaluationService
    ) {
        this.inputData = inputData;
        this.constraintEvaluationService = constraintEvaluationService;
        this.incrementalConstraintEvaluationService = incrementalConstraintEvaluationService;

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
    public IntegerSolution createSolution() {
        IntegerSolution solution = new DefaultIntegerSolution(
                variableBounds(),
                numberOfObjectives(),
                numberOfConstraints()
        );

        PartialSolutionContext partialContext =
                new PartialSolutionContext(baseContext.getPreparedEvaluationData());

        List<Integer> orderedClassIndexes = orderClassesByDifficulty();

        for (Integer classIndex : orderedClassIndexes) {
            int selectedRoomIndex = chooseBestRoom(classIndex, partialContext);
            solution.variables().set(classIndex, selectedRoomIndex);

            ClassRoomAssignment assignment = new ClassRoomAssignment(
                    classIndex,
                    classes.get(classIndex),
                    selectedRoomIndex,
                    rooms.get(selectedRoomIndex)
            );

            partialContext.addAssignment(assignment);
            indexAssignment(partialContext, classIndex, selectedRoomIndex);
        }

        return solution;
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        long evaluateStartNs = System.nanoTime();
        partitionMetrics.incrementEvaluationCount();

        long buildAssignmentsStartNs = System.nanoTime();
        List<ClassRoomAssignment> assignments = buildAssignments(solution);
        partitionMetrics.addBuildAssignmentsTime(System.nanoTime() - buildAssignmentsStartNs);

        SolutionContext evaluationContext = copyBaseContextWithAssignments(assignments);

        long constraintEvalStartNs = System.nanoTime();
        ConstraintEvaluationResult evaluationResult =
                constraintEvaluationService.evaluate(evaluationContext, selectedConstraints);
        partitionMetrics.addConstraintEvaluationTime(System.nanoTime() - constraintEvalStartNs);

        double softPenalty = evaluationResult.getSoftScore() == null
                ? 0.0
                : evaluationResult.getSoftScore();

        solution.objectives()[0] = softPenalty;

        fillHardConstraintViolations(solution, evaluationResult);

        partitionMetrics.addEvaluateTime(System.nanoTime() - evaluateStartNs);

        return solution;
    }

    private int chooseBestRoom(int classIndex, PartialSolutionContext partialContext) {
        double bestHardScore = Double.POSITIVE_INFINITY;
        double bestSoftScore = Double.POSITIVE_INFINITY;
        List<Integer> bestRoomIndexes = new ArrayList<>();

        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            CandidateAssignment candidate = new CandidateAssignment(classIndex, roomIndex);

            IncrementalConstraintEvaluationService.CandidateScore score =
                    incrementalConstraintEvaluationService.evaluateCandidate(
                            partialContext,
                            candidate,
                            selectedConstraints
                    );

            boolean isBetter =
                    score.hardScore() < bestHardScore
                            || (Double.compare(score.hardScore(), bestHardScore) == 0
                            && score.softScore() < bestSoftScore);

            boolean isTie =
                    Double.compare(score.hardScore(), bestHardScore) == 0
                            && Double.compare(score.softScore(), bestSoftScore) == 0;

            if (isBetter) {
                bestHardScore = score.hardScore();
                bestSoftScore = score.softScore();
                bestRoomIndexes.clear();
                bestRoomIndexes.add(roomIndex);
            } else if (isTie) {
                bestRoomIndexes.add(roomIndex);
            }
        }

        if (bestRoomIndexes.isEmpty()) {
            return ThreadLocalRandom.current().nextInt(rooms.size());
        }

        return bestRoomIndexes.get(ThreadLocalRandom.current().nextInt(bestRoomIndexes.size()));
    }

    private void indexAssignment(
            PartialSolutionContext partialContext,
            int classIndex,
            int roomIndex
    ) {
        PreparedClassData preparedClass = baseContext.getPreparedEvaluationData().getClasses().get(classIndex);
        PreparedRoomData preparedRoom = baseContext.getPreparedEvaluationData().getRooms().get(roomIndex);

        if (preparedClass.getStartDateTime() == null
                || preparedClass.getEndDateTime() == null
                || !preparedClass.getEndDateTime().isAfter(preparedClass.getStartDateTime())) {
            return;
        }

        String roomIdentity = normalizeRoomIdentity(preparedRoom.getRoomIdentity());
        if (roomIdentity.isBlank()) {
            return;
        }

        partialContext.indexOccupation(
                roomIdentity,
                classIndex,
                preparedClass.getStartDateTime(),
                preparedClass.getEndDateTime()
        );
    }

    private List<Integer> orderClassesByDifficulty() {
        List<Integer> classIndexes = new ArrayList<>();
        for (int i = 0; i < numberOfVariables; i++) {
            classIndexes.add(i);
        }

        classIndexes.sort((a, b) -> {
            PreparedClassData classA = baseContext.getPreparedEvaluationData().getClasses().get(a);
            PreparedClassData classB = baseContext.getPreparedEvaluationData().getClasses().get(b);

            int studentsA = classA.getStudents() == null ? 0 : classA.getStudents();
            int studentsB = classB.getStudents() == null ? 0 : classB.getStudents();

            int featureCountA = classA.getRequestedCharacteristics() == null ? 0 : classA.getRequestedCharacteristics().size();
            int featureCountB = classB.getRequestedCharacteristics() == null ? 0 : classB.getRequestedCharacteristics().size();

            int compareStudents = Integer.compare(studentsB, studentsA);
            if (compareStudents != 0) {
                return compareStudents;
            }

            return Integer.compare(featureCountB, featureCountA);
        });

        return classIndexes;
    }

    private SolutionContext copyBaseContextWithAssignments(List<ClassRoomAssignment> assignments) {
        SolutionContext copy = new SolutionContext();

        copy.setProblemId(baseContext.getProblemId());
        copy.setProblemName(baseContext.getProblemName());
        copy.setProblemType(baseContext.getProblemType());
        copy.setProblemSubtype(baseContext.getProblemSubtype());

        copy.setSelectedAlgorithm(baseContext.getSelectedAlgorithm());
        copy.setResolutionScope(baseContext.getResolutionScope());
        copy.setRepeatedInstanceStrategy(baseContext.getRepeatedInstanceStrategy());

        copy.setScheduleData(baseContext.getScheduleData());
        copy.setRoomsData(baseContext.getRoomsData());
        copy.setMappingData(baseContext.getMappingData());
        copy.setRoomsMappingData(baseContext.getRoomsMappingData());
        copy.setConstraintsSummary(baseContext.getConstraintsSummary());
        copy.setInstanceCharacteristics(baseContext.getInstanceCharacteristics());

        copy.setAssignments(assignments);
        copy.setSelectedConstraints(baseContext.getSelectedConstraints());
        copy.setPreviousPartitionAssignmentsContext(baseContext.getPreviousPartitionAssignmentsContext());
        copy.setPreparedEvaluationData(baseContext.getPreparedEvaluationData());

        return copy;
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

    private String normalizeRoomIdentity(String roomIdentity) {
        return roomIdentity == null ? "" : roomIdentity.trim().toLowerCase();
    }

    public PartitionMetrics getPartitionMetrics() {
        return partitionMetrics;
    }
}