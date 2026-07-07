package pt.lourenco.optimization.jmetal.constraints.rules;

import org.springframework.stereotype.Component;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.IncrementalConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class ConsecutiveRoomChangeConstraint implements ConstraintRule, IncrementalConstraintRule {

    private static final String CONSTRAINT_ID = "consecutive_room_change";

    @Override
    public String getConstraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public ConstraintResult evaluate(SolutionContext context, UserConstraintSelection selection) {
        double rawViolation = calculateRawViolation(context);

        return new ConstraintResult(
                CONSTRAINT_ID,
                selection.getGoal(),
                rawViolation,
                rawViolation
        );
    }

    @Override
    public IncrementalConstraintResult evaluateIncrementally(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            UserConstraintSelection selection
    ) {
        PreparedEvaluationData prepared = context.getPreparedEvaluationData();
        if (prepared == null) {
            return IncrementalConstraintResult.zero();
        }

        List<PreparedClassData> classes = prepared.getClasses();
        List<PreparedRoomData> rooms = prepared.getRooms();

        if (candidate.classIndex() < 0 || candidate.classIndex() >= classes.size()) {
            return IncrementalConstraintResult.zero();
        }

        if (candidate.roomIndex() < 0 || candidate.roomIndex() >= rooms.size()) {
            return IncrementalConstraintResult.zero();
        }

        PreparedClassData currentClass = classes.get(candidate.classIndex());
        PreparedRoomData currentRoom = rooms.get(candidate.roomIndex());

        String groupKey = buildGroupKey(currentClass);
        LocalDate day = extractDay(currentClass);
        LocalDateTime start = currentClass.getStartDateTime();
        LocalDateTime end = currentClass.getEndDateTime();
        String roomIdentity = normalizeText(currentRoom.getRoomIdentity());

        if (groupKey == null || day == null || start == null || end == null || roomIdentity == null) {
            return IncrementalConstraintResult.zero();
        }

        String bucketKey = buildBucketKey(groupKey, day);
        double penalty = 0.0;

        Map<LocalDateTime, List<PartialSolutionContext.ConsecutiveRoomChangeSlot>> byEnd =
                context.getConsecutiveRoomChangeSlotsByEnd().getOrDefault(bucketKey, Map.of());

        for (PartialSolutionContext.ConsecutiveRoomChangeSlot previous : byEnd.getOrDefault(start, List.of())) {
            if (!roomIdentity.equals(previous.roomIdentity())) {
                penalty += 1.0;
            }
        }

        Map<LocalDateTime, List<PartialSolutionContext.ConsecutiveRoomChangeSlot>> byStart =
                context.getConsecutiveRoomChangeSlotsByStart().getOrDefault(bucketKey, Map.of());

        for (PartialSolutionContext.ConsecutiveRoomChangeSlot next : byStart.getOrDefault(end, List.of())) {
            if (!roomIdentity.equals(next.roomIdentity())) {
                penalty += 1.0;
            }
        }

        return new IncrementalConstraintResult(penalty);
    }

    private double calculateRawViolation(SolutionContext context) {
        PreparedEvaluationData prepared = context.getPreparedEvaluationData();

        if (prepared == null || context.getAssignments() == null || context.getAssignments().isEmpty()) {
            return 0.0;
        }

        List<ClassRoomAssignment> assignments = context.getAssignments();
        List<PreparedClassData> classes = prepared.getClasses();
        List<PreparedRoomData> rooms = prepared.getRooms();

        Map<String, List<AssignedSlot>> slotsByBucket = new HashMap<>();

        for (ClassRoomAssignment assignment : assignments) {
            int classIndex = assignment.getClassIndex();
            int roomIndex = assignment.getRoomIndex();

            if (classIndex < 0 || classIndex >= classes.size()) {
                continue;
            }

            if (roomIndex < 0 || roomIndex >= rooms.size()) {
                continue;
            }

            PreparedClassData preparedClass = classes.get(classIndex);
            PreparedRoomData preparedRoom = rooms.get(roomIndex);

            String groupKey = buildGroupKey(preparedClass);
            LocalDate day = extractDay(preparedClass);
            LocalDateTime start = preparedClass.getStartDateTime();
            LocalDateTime end = preparedClass.getEndDateTime();
            String roomIdentity = normalizeText(preparedRoom.getRoomIdentity());

            if (groupKey == null || day == null || start == null || end == null || roomIdentity == null) {
                continue;
            }

            String bucketKey = buildBucketKey(groupKey, day);

            slotsByBucket.computeIfAbsent(bucketKey, k -> new ArrayList<>())
                    .add(new AssignedSlot(classIndex, start, end, roomIdentity));
        }

        double totalPenalty = 0.0;

        for (List<AssignedSlot> bucketSlots : slotsByBucket.values()) {
            bucketSlots.sort(Comparator
                    .comparing(AssignedSlot::startDateTime)
                    .thenComparing(AssignedSlot::endDateTime)
                    .thenComparing(AssignedSlot::classIndex));

            for (int i = 0; i < bucketSlots.size() - 1; i++) {
                AssignedSlot current = bucketSlots.get(i);
                AssignedSlot next = bucketSlots.get(i + 1);

                if (current.endDateTime().equals(next.startDateTime())
                        && !current.roomIdentity().equals(next.roomIdentity())) {
                    totalPenalty += 1.0;
                }
            }
        }

        return totalPenalty;
    }

    private String buildGroupKey(PreparedClassData preparedClass) {
        String subject = normalizeText(resolveSubject(preparedClass));
        String classGroup = normalizeText(preparedClass.getClassGroup());
        String shift = normalizeText(resolveShift(preparedClass));

        if (subject == null || classGroup == null || shift == null) {
            return null;
        }

        String degree = normalizeText(preparedClass.getDegree());
        if (degree == null) {
            return "subject::" + subject + "||group::" + classGroup + "||shift::" + shift;
        }

        return "degree::" + degree
                + "||subject::" + subject
                + "||group::" + classGroup
                + "||shift::" + shift;
    }

    private LocalDate extractDay(PreparedClassData preparedClass) {
        if (preparedClass.getDay() != null) {
            return preparedClass.getDay();
        }

        if (preparedClass.getStartDateTime() != null) {
            return preparedClass.getStartDateTime().toLocalDate();
        }

        return null;
    }

    private String buildBucketKey(String groupKey, LocalDate day) {
        return groupKey + "##" + day;
    }

    private String resolveSubject(PreparedClassData preparedClass) {
        return preparedClass.getCourse();
    }

    private String resolveShift(PreparedClassData preparedClass) {
        return preparedClass.getShift();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private record AssignedSlot(
            int classIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String roomIdentity
    ) {}
}