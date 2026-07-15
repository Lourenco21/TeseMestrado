package pt.lourenco.optimization.jmetal.problems.problems;

import lombok.extern.slf4j.Slf4j;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.IncrementalConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.metrics.PartitionMetrics;
import pt.lourenco.optimization.jmetal.partitioning.GlobalRoomOccupationTracker;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ScheduleOptimizationProblem extends AbstractIntegerProblem {

    private static final String ROOM_EXCLUSIVITY_CONSTRAINT_ID = "room_exclusivity";
    private static final String CAPACITY_WASTE_CONSTRAINT_ID = "capacity_waste";

    private final ProblemInputData inputData;
    private final ConstraintEvaluationService constraintEvaluationService;
    private final IncrementalConstraintEvaluationService incrementalConstraintEvaluationService;

    private final List<Map<String, Object>> classes;
    private final List<Map<String, Object>> rooms;
    private final List<UserConstraintSelection> selectedConstraints;
    private final List<UserConstraintSelection> hardConstraints;
    private final List<UserConstraintSelection> softConstraints;
    private final SolutionContext baseContext;

    private final int numberOfVariables;
    private final int numberOfObjectives;
    private final int numberOfConstraints;

    private final Map<String, Double> fallbackHardViolations = new java.util.HashMap<>();

    private final PartitionMetrics partitionMetrics = new PartitionMetrics();

    private final GlobalRoomOccupationTracker globalRoomOccupationTracker;

    public ScheduleOptimizationProblem(
            ProblemInputData inputData,
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService,
            IncrementalConstraintEvaluationService incrementalConstraintEvaluationService,
            GlobalRoomOccupationTracker globalRoomOccupationTracker
    ) {

        this.inputData = inputData;
        this.constraintEvaluationService = constraintEvaluationService;
        this.incrementalConstraintEvaluationService = incrementalConstraintEvaluationService;
        this.globalRoomOccupationTracker = globalRoomOccupationTracker;

        this.name("ScheduleOptimizationProblem");

        this.classes = extractClasses(inputData.getScheduleData());
        this.rooms = extractRooms(inputData.getRoomsData());
        this.selectedConstraints = inputData.getSelectedConstraints() == null
                ? List.of()
                : inputData.getSelectedConstraints();

        this.hardConstraints = this.selectedConstraints.stream()
                .filter(selection -> selection != null)
                .filter(selection ->
                        selection.getGoal() == ConstraintGoal.HARD
                                || ROOM_EXCLUSIVITY_CONSTRAINT_ID.equalsIgnoreCase(selection.getId()))
                .filter(selection -> !CAPACITY_WASTE_CONSTRAINT_ID.equalsIgnoreCase(selection.getId()))
                .toList();

        this.softConstraints = this.selectedConstraints.stream()
                .filter(selection -> selection != null)
                .filter(selection ->
                        (selection.getGoal() == ConstraintGoal.SOFT
                                || CAPACITY_WASTE_CONSTRAINT_ID.equalsIgnoreCase(selection.getId()))
                                && !ROOM_EXCLUSIVITY_CONSTRAINT_ID.equalsIgnoreCase(selection.getId()))
                .toList();

        this.baseContext = solutionContextBuilderService.buildFromProblemInput(inputData);

        this.numberOfVariables = classes.size();
        this.numberOfObjectives = 1;
        this.numberOfConstraints = hardConstraints.size();

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

        fallbackHardViolations.clear();

        IntegerSolution solution = new DefaultIntegerSolution(
                variableBounds(),
                numberOfObjectives(),
                numberOfConstraints()
        );

        PreparedEvaluationData prepared = baseContext.getPreparedEvaluationData();
        PartialSolutionContext partialContext = new PartialSolutionContext(prepared, globalRoomOccupationTracker);

        List<Integer> orderedClassIndexes = orderClassesByDifficulty();

        for (Integer classIndex : orderedClassIndexes) {
            Integer selectedRoomIndex = findFirstFeasibleRoom(classIndex, partialContext);

            if (selectedRoomIndex == null) {
                selectedRoomIndex = fallbackRoomIndex(classIndex, partialContext);
                recordFallbackViolations(classIndex, selectedRoomIndex, partialContext);
            }

            solution.variables().set(classIndex, selectedRoomIndex);
            registerAssignment(partialContext, classIndex, selectedRoomIndex);
        }

        return solution;
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        long evaluateStartNs = System.nanoTime();
        partitionMetrics.incrementEvaluationCount();

        solution = repairSolution(solution);

        long buildAssignmentsStartNs = System.nanoTime();
        List<ClassRoomAssignment> assignments = buildAssignments(solution);
        partitionMetrics.addBuildAssignmentsTime(System.nanoTime() - buildAssignmentsStartNs);

        SolutionContext evaluationContext = copyBaseContextWithAssignments(assignments);

        long constraintEvalStartNs = System.nanoTime();

        ConstraintEvaluationResult softEvaluationResult =
                constraintEvaluationService.evaluate(evaluationContext, softConstraints);

        partitionMetrics.addConstraintEvaluationTime(System.nanoTime() - constraintEvalStartNs);

        double softPenalty = softEvaluationResult.getSoftScore() == null
                ? 0.0
                : softEvaluationResult.getSoftScore();

        solution.objectives()[0] = softPenalty;

        double[] constraintArray = solution.constraints();
        for (int i = 0; i < hardConstraints.size(); i++) {
            UserConstraintSelection selection = hardConstraints.get(i);
            double violations = fallbackHardViolations.getOrDefault(selection.getId(), 0.0);
            constraintArray[i] = violations > 0.0 ? -violations : 0.0;
        }

        partitionMetrics.addEvaluateTime(System.nanoTime() - evaluateStartNs);

        return solution;
    }

    private Integer findFirstFeasibleRoom(int classIndex, PartialSolutionContext partialContext) {
        PreparedEvaluationData prepared = baseContext.getPreparedEvaluationData();
        PreparedClassData preparedClass = prepared.getClasses().get(classIndex);

        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();

        if (start == null || end == null || !end.isAfter(start)) {
            return null;
        }

        Integer bestAvailableRoomIndex = null;
        int bestViolationCount = Integer.MAX_VALUE;

        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            if (!partialContext.isRoomAvailable(roomIndex, start, end)) {
                continue;
            }

            CandidateAssignment candidate = new CandidateAssignment(classIndex, roomIndex);

            IncrementalConstraintEvaluationService.HardConstraintCheckResult result =
                    incrementalConstraintEvaluationService.checkHardConstraints(
                            partialContext, candidate, hardConstraints
                    );

            int violationCount = result.violatedConstraintIds().size();

            if (violationCount == 0) {
                return roomIndex;
            }

            if (violationCount < bestViolationCount) {
                bestViolationCount = violationCount;
                bestAvailableRoomIndex = roomIndex;
            }
        }

        return bestAvailableRoomIndex;
    }

    public IntegerSolution repairSolution(IntegerSolution solution) {

        fallbackHardViolations.clear();

        if (solution == null || hardConstraints == null || hardConstraints.isEmpty()) {
            return solution;
        }

        PreparedEvaluationData prepared = baseContext.getPreparedEvaluationData();
        PartialSolutionContext partialContext = new PartialSolutionContext(prepared, globalRoomOccupationTracker);

        for (ClassRoomAssignment assignment : buildAssignments(solution)) {
            partialContext.addAssignment(assignment);
        }

        List<Integer> orderedClassIndexes = orderClassesByDifficulty();

        for (Integer classIndex : orderedClassIndexes) {
            Integer currentRoomIndex = solution.variables().get(classIndex);

            if (currentRoomIndex == null || currentRoomIndex < 0 || currentRoomIndex >= rooms.size()) {
                Integer repairedRoomIndex = findFirstFeasibleRoom(classIndex, partialContext);

                if (repairedRoomIndex == null) {
                    repairedRoomIndex = fallbackRoomIndex(classIndex, partialContext);
                    recordFallbackViolations(classIndex, repairedRoomIndex, partialContext);
                }

                solution.variables().set(classIndex, repairedRoomIndex);
                registerAssignment(partialContext, classIndex, repairedRoomIndex);
                continue;
            }

            partialContext.removeAssignment(classIndex);

            PreparedClassData preparedClass = prepared.getClasses().get(classIndex);
            LocalDateTime start = preparedClass.getStartDateTime();
            LocalDateTime end = preparedClass.getEndDateTime();

            boolean currentRoomAvailable = start != null
                    && end != null
                    && end.isAfter(start)
                    && partialContext.isRoomAvailable(currentRoomIndex, start, end);

            if (currentRoomAvailable) {
                CandidateAssignment currentCandidate = new CandidateAssignment(classIndex, currentRoomIndex);

                boolean currentIsFeasible = incrementalConstraintEvaluationService.isHardFeasible(
                        partialContext,
                        currentCandidate,
                        hardConstraints
                );

                if (currentIsFeasible) {
                    registerAssignment(partialContext, classIndex, currentRoomIndex);
                    continue;
                }
            }

            Integer repairedRoomIndex = findFirstFeasibleRoom(classIndex, partialContext);

            if (repairedRoomIndex == null) {
                log.warn("No room available at all for classIndex={} (start={}, end={}). " +
                                "This means concurrent demand exceeds room count for this slot.",
                        classIndex, preparedClass.getStartDateTime(), preparedClass.getEndDateTime());
                repairedRoomIndex = fallbackRoomIndex(classIndex, partialContext);
            }

            solution.variables().set(classIndex, repairedRoomIndex);
            registerAssignment(partialContext, classIndex, repairedRoomIndex);
        }

        return solution;
    }

    private int fallbackRoomIndex(int classIndex, PartialSolutionContext partialContext) {
        PreparedEvaluationData prepared = baseContext.getPreparedEvaluationData();
        PreparedClassData preparedClass = prepared.getClasses().get(classIndex);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();

        Integer bestRoomIndex = null;
        Integer bestCapacityGap = null;

        for (int roomIndex = 0; roomIndex < prepared.getRooms().size(); roomIndex++) {
            if (start != null && end != null && end.isAfter(start)
                    && !partialContext.isRoomAvailable(roomIndex, start, end)) {
                continue;
            }

            PreparedRoomData preparedRoom = prepared.getRooms().get(roomIndex);
            Integer students = preparedClass.getStudents();
            Integer capacity = preparedRoom.getCapacity();
            int gap = (students == null || capacity == null) ? Integer.MAX_VALUE / 4 : Math.abs(capacity - students);

            if (bestRoomIndex == null || gap < bestCapacityGap) {
                bestRoomIndex = roomIndex;
                bestCapacityGap = gap;
            }
        }

        if (bestRoomIndex == null) {
            log.error("SATURAÇÃO REAL: nenhuma sala livre para classIndex={} start={} end={} — " +
                    "procura excede oferta de salas neste slot.", classIndex, start, end);
            return fallbackRoomIndexIgnoringAvailability(classIndex);
        }

        return bestRoomIndex;
    }

    private int fallbackRoomIndexIgnoringAvailability(int classIndex) {
        PreparedEvaluationData prepared = baseContext.getPreparedEvaluationData();
        PreparedClassData preparedClass = prepared.getClasses().get(classIndex);

        Integer bestRoomIndex = null;
        Integer bestCapacityGap = null;

        for (int roomIndex = 0; roomIndex < prepared.getRooms().size(); roomIndex++) {
            PreparedRoomData preparedRoom = prepared.getRooms().get(roomIndex);

            Integer students = preparedClass.getStudents();
            Integer capacity = preparedRoom.getCapacity();

            int gap;
            if (students == null || capacity == null) {
                gap = Integer.MAX_VALUE / 4;
            } else {
                gap = Math.abs(capacity - students);
            }

            if (bestRoomIndex == null || gap < bestCapacityGap) {
                bestRoomIndex = roomIndex;
                bestCapacityGap = gap;
            }
        }

        return bestRoomIndex == null ? 0 : bestRoomIndex;
    }

    private void registerAssignment(
            PartialSolutionContext partialContext,
            int classIndex,
            int roomIndex
    ) {
        ClassRoomAssignment assignment = new ClassRoomAssignment(
                classIndex,
                classes.get(classIndex),
                roomIndex,
                rooms.get(roomIndex)
        );

        partialContext.addAssignment(assignment);
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

            int requirementsA = classA.getRequestedCharacteristics() == null
                    ? 0
                    : classA.getRequestedCharacteristics().size();

            int requirementsB = classB.getRequestedCharacteristics() == null
                    ? 0
                    : classB.getRequestedCharacteristics().size();

            int compareStudents = Integer.compare(studentsB, studentsA);
            if (compareStudents != 0) {
                return compareStudents;
            }

            return Integer.compare(requirementsB, requirementsA);
        });

        return classIndexes;
    }

    private void recordFallbackViolations(
            int classIndex,
            int roomIndex,
            PartialSolutionContext partialContext
    ) {
        CandidateAssignment candidate = new CandidateAssignment(classIndex, roomIndex);

        IncrementalConstraintEvaluationService.HardConstraintCheckResult result =
                incrementalConstraintEvaluationService.checkHardConstraints(
                        partialContext, candidate, hardConstraints
                );

        for (String violatedId : result.violatedConstraintIds()) {
            fallbackHardViolations.merge(violatedId, 1.0, Double::sum);
        }
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