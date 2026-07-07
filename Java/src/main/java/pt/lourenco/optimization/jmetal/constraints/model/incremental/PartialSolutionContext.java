package pt.lourenco.optimization.jmetal.constraints.model.incremental;

import lombok.Getter;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PartialSolutionContext {

    private final PreparedEvaluationData preparedEvaluationData;
    private final List<ClassRoomAssignment> assignments = new ArrayList<>();
    private final Map<String, List<OccupiedRoomSlot>> occupationsByRoom = new HashMap<>();

    private final Map<String, Map<LocalDateTime, List<StudentGroupSlot>>> studentGroupSlotsByStart = new HashMap<>();
    private final Map<String, Map<LocalDateTime, List<StudentGroupSlot>>> studentGroupSlotsByEnd = new HashMap<>();

    private final Map<String, Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>>> consecutiveRoomChangeSlotsByStart = new HashMap<>();
    private final Map<String, Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>>> consecutiveRoomChangeSlotsByEnd = new HashMap<>();

    public PartialSolutionContext(PreparedEvaluationData preparedEvaluationData) {
        this.preparedEvaluationData = preparedEvaluationData;
    }

    public void addAssignment(ClassRoomAssignment assignment) {
        assignments.add(assignment);
        indexStudentRelocation(assignment);
        indexConsecutiveRoomChange(assignment);
    }

    public void indexOccupation(
            String roomIdentity,
            int classIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        occupationsByRoom
                .computeIfAbsent(roomIdentity, key -> new ArrayList<>())
                .add(new OccupiedRoomSlot(classIndex, startDateTime, endDateTime));
    }

    private void indexStudentRelocation(ClassRoomAssignment assignment) {
        if (preparedEvaluationData == null || assignment == null) {
            return;
        }

        int classIndex = assignment.getClassIndex();
        int roomIndex = assignment.getRoomIndex();

        List<PreparedClassData> classes = preparedEvaluationData.getClasses();
        if (classIndex < 0 || classIndex >= classes.size()) {
            return;
        }

        if (roomIndex < 0 || roomIndex >= preparedEvaluationData.getRooms().size()) {
            return;
        }

        PreparedClassData preparedClass = classes.get(classIndex);
        var preparedRoom = preparedEvaluationData.getRooms().get(roomIndex);

        String groupKey = buildStudentRelocationGroupKey(preparedClass);
        LocalDate day = extractDay(preparedClass);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();
        String building = normalizeText(preparedRoom.getBuilding());

        if (groupKey == null || day == null || start == null || end == null || building == null) {
            return;
        }

        String bucketKey = buildBucketKey(groupKey, day);
        StudentGroupSlot slot = new StudentGroupSlot(classIndex, roomIndex, start, end, building);

        studentGroupSlotsByStart
                .computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(start, k -> new ArrayList<>())
                .add(slot);

        studentGroupSlotsByEnd
                .computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(end, k -> new ArrayList<>())
                .add(slot);
    }

    private void indexConsecutiveRoomChange(ClassRoomAssignment assignment) {
        if (preparedEvaluationData == null || assignment == null) {
            return;
        }

        int classIndex = assignment.getClassIndex();
        int roomIndex = assignment.getRoomIndex();

        List<PreparedClassData> classes = preparedEvaluationData.getClasses();
        if (classIndex < 0 || classIndex >= classes.size()) {
            return;
        }

        if (roomIndex < 0 || roomIndex >= preparedEvaluationData.getRooms().size()) {
            return;
        }

        PreparedClassData preparedClass = classes.get(classIndex);
        var preparedRoom = preparedEvaluationData.getRooms().get(roomIndex);

        String groupKey = buildConsecutiveRoomChangeGroupKey(preparedClass);
        LocalDate day = extractDay(preparedClass);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();
        String roomIdentity = normalizeText(preparedRoom.getRoomIdentity());

        if (groupKey == null || day == null || start == null || end == null || roomIdentity == null) {
            return;
        }

        String bucketKey = buildBucketKey(groupKey, day);
        ConsecutiveRoomChangeSlot slot = new ConsecutiveRoomChangeSlot(
                classIndex,
                roomIndex,
                start,
                end,
                roomIdentity
        );

        consecutiveRoomChangeSlotsByStart
                .computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(start, k -> new ArrayList<>())
                .add(slot);

        consecutiveRoomChangeSlotsByEnd
                .computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(end, k -> new ArrayList<>())
                .add(slot);
    }

    private String buildStudentRelocationGroupKey(PreparedClassData preparedClass) {
        String classGroup = normalizeText(preparedClass.getClassGroup());
        if (classGroup == null) {
            return null;
        }

        String degree = normalizeText(preparedClass.getDegree());
        if (degree == null) {
            return "group::" + classGroup;
        }

        return "degree::" + degree + "||group::" + classGroup;
    }

    private String buildConsecutiveRoomChangeGroupKey(PreparedClassData preparedClass) {
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

    private String resolveSubject(PreparedClassData preparedClass) {
        return preparedClass.getCourse();
    }

    private String resolveShift(PreparedClassData preparedClass) {
        return preparedClass.getShift();
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

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    public record OccupiedRoomSlot(
            int classIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {}

    public record StudentGroupSlot(
            int classIndex,
            int roomIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String building
    ) {}

    public record ConsecutiveRoomChangeSlot(
            int classIndex,
            int roomIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String roomIdentity
    ) {}
}