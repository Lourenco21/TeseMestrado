package pt.lourenco.optimization.jmetal.constraints.model.incremental;

import lombok.Getter;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.partitioning.GlobalRoomOccupationTracker;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PartialSolutionContext {

    private static final int SLOT_MINUTES = 30;

    private final PreparedEvaluationData preparedEvaluationData;
    private final LocalDateTime scheduleOriginDateTime;
    private final GlobalRoomOccupationTracker globalTracker;

    private final List<ClassRoomAssignment> assignments = new ArrayList<>();
    private final Map<Integer, ClassRoomAssignment> assignmentsByClassIndex = new HashMap<>();
    private final Map<Integer, Integer> assignedRoomByClassIndex = new HashMap<>();

    /**
     * Local-only occupation counts for THIS partition's assignments (not a copy of
     * the global tracker). Availability checks combine this with a point lookup
     * against globalTracker, avoiding any full-map copy per partition.
     */
    private final Map<Integer, Integer>[] roomOccupationCounts;

    private final Map<String, Map<LocalDateTime, List<StudentGroupSlot>>> studentGroupSlotsByStart = new HashMap<>();
    private final Map<String, Map<LocalDateTime, List<StudentGroupSlot>>> studentGroupSlotsByEnd = new HashMap<>();

    private final Map<String, Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>>> consecutiveRoomChangeSlotsByStart = new HashMap<>();
    private final Map<String, Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>>> consecutiveRoomChangeSlotsByEnd = new HashMap<>();

    private final Map<Integer, List<Integer>> slotsByClassIndex = new HashMap<>();
    private final Map<Integer, List<RelocationEntryRef>> studentRelocationRefsByClassIndex = new HashMap<>();
    private final Map<Integer, List<ConsecutiveEntryRef>> consecutiveRefsByClassIndex = new HashMap<>();

    @SuppressWarnings("unchecked")
    public PartialSolutionContext(
            PreparedEvaluationData preparedEvaluationData,
            GlobalRoomOccupationTracker globalTracker
    ) {
        this.preparedEvaluationData = preparedEvaluationData;
        this.globalTracker = globalTracker;

        int numberOfRooms;
        if (globalTracker != null) {
            this.scheduleOriginDateTime = globalTracker.getScheduleOriginDateTime();
            numberOfRooms = globalTracker.getNumberOfRooms();
        } else {
            this.scheduleOriginDateTime = LocalDateTime.now();
            numberOfRooms = preparedEvaluationData.getRooms().size();
        }

        // Always starts EMPTY, regardless of globalTracker — no per-partition copy
        // of the accumulated global occupation map. Availability checks consult the
        // global tracker directly (point lookups) instead.
        this.roomOccupationCounts = new Map[numberOfRooms];
        for (int i = 0; i < numberOfRooms; i++) {
            this.roomOccupationCounts[i] = new HashMap<>();
        }
    }

    private int toSlotIndex(LocalDateTime dateTime) {
        long minutesBetween = java.time.temporal.ChronoUnit.MINUTES.between(scheduleOriginDateTime, dateTime);
        return (int) (minutesBetween / SLOT_MINUTES);
    }

    public void addAssignment(ClassRoomAssignment assignment) {
        if (assignment == null) {
            return;
        }

        assignments.add(assignment);
        assignmentsByClassIndex.put(assignment.getClassIndex(), assignment);
        assignedRoomByClassIndex.put(assignment.getClassIndex(), assignment.getRoomIndex());

        indexRoomOccupationBySlots(assignment);
        indexStudentRelocation(assignment);
        indexConsecutiveRoomChange(assignment);
    }

    public void removeAssignment(int classIndex) {
        ClassRoomAssignment existing = assignmentsByClassIndex.remove(classIndex);
        assignedRoomByClassIndex.remove(classIndex);

        if (existing == null) {
            return;
        }

        assignments.removeIf(a -> a.getClassIndex() == classIndex);

        int roomIndex = existing.getRoomIndex();

        List<Integer> touchedSlots = slotsByClassIndex.remove(classIndex);
        if (touchedSlots != null && roomIndex >= 0 && roomIndex < roomOccupationCounts.length) {
            for (int slot : touchedSlots) {
                roomOccupationCounts[roomIndex].computeIfPresent(slot, (k, count) -> count > 1 ? count - 1 : null);
            }
        }

        List<RelocationEntryRef> relocationRefs = studentRelocationRefsByClassIndex.remove(classIndex);
        if (relocationRefs != null) {
            for (RelocationEntryRef ref : relocationRefs) {
                removeFromBucket(studentGroupSlotsByStart, ref.bucketKey(), ref.startDateTime(), classIndex);
                removeFromBucket(studentGroupSlotsByEnd, ref.bucketKey(), ref.endDateTime(), classIndex);
            }
        }

        List<ConsecutiveEntryRef> consecutiveRefs = consecutiveRefsByClassIndex.remove(classIndex);
        if (consecutiveRefs != null) {
            for (ConsecutiveEntryRef ref : consecutiveRefs) {
                removeFromConsecutiveBucket(consecutiveRoomChangeSlotsByStart, ref.bucketKey(), ref.startDateTime(), classIndex);
                removeFromConsecutiveBucket(consecutiveRoomChangeSlotsByEnd, ref.bucketKey(), ref.endDateTime(), classIndex);
            }
        }
    }

    private <T> void removeFromBucket(
            Map<String, Map<LocalDateTime, List<StudentGroupSlot>>> byBucket,
            String bucketKey,
            LocalDateTime key,
            int classIndex
    ) {
        Map<LocalDateTime, List<StudentGroupSlot>> inner = byBucket.get(bucketKey);
        if (inner == null) return;
        List<StudentGroupSlot> list = inner.get(key);
        if (list == null) return;
        list.removeIf(s -> s.classIndex() == classIndex);
        if (list.isEmpty()) {
            inner.remove(key);
        }
        if (inner.isEmpty()) {
            byBucket.remove(bucketKey);
        }
    }

    private void removeFromConsecutiveBucket(
            Map<String, Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>>> byBucket,
            String bucketKey,
            LocalDateTime key,
            int classIndex
    ) {
        Map<LocalDateTime, List<ConsecutiveRoomChangeSlot>> inner = byBucket.get(bucketKey);
        if (inner == null) return;
        List<ConsecutiveRoomChangeSlot> list = inner.get(key);
        if (list == null) return;
        list.removeIf(s -> s.classIndex() == classIndex);
        if (list.isEmpty()) {
            inner.remove(key);
        }
        if (inner.isEmpty()) {
            byBucket.remove(bucketKey);
        }
    }

    /**
     * Availability now combines LOCAL occupation (this partition's own assignments)
     * with a point lookup against the GLOBAL tracker (previously committed
     * partitions) — no full-map copy is ever made.
     */
    public boolean isRoomAvailable(int roomIndex, LocalDateTime start, LocalDateTime end) {
        if (roomIndex < 0 || roomIndex >= roomOccupationCounts.length
                || start == null || end == null || !start.isBefore(end)) {
            return false;
        }

        int fromSlot = toSlotIndex(start);
        int toSlotExclusive = toSlotIndex(end);

        Map<Integer, Integer> localCounts = roomOccupationCounts[roomIndex];

        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            int localCount = localCounts.getOrDefault(slot, 0);
            int globalCount = globalTracker == null ? 0 : globalTracker.getOccupationCount(roomIndex, slot);
            if (localCount + globalCount > 0) {
                return false;
            }
        }
        return true;
    }

    private void indexRoomOccupationBySlots(ClassRoomAssignment assignment) {
        if (preparedEvaluationData == null || assignment == null) {
            return;
        }

        int classIndex = assignment.getClassIndex();
        int roomIndex = assignment.getRoomIndex();

        List<PreparedClassData> classes = preparedEvaluationData.getClasses();
        List<PreparedRoomData> rooms = preparedEvaluationData.getRooms();

        if (classIndex < 0 || classIndex >= classes.size()) return;
        if (roomIndex < 0 || roomIndex >= rooms.size() || roomIndex >= roomOccupationCounts.length) return;

        PreparedClassData preparedClass = classes.get(classIndex);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();

        if (start == null || end == null || !start.isBefore(end)) return;

        int fromSlot = toSlotIndex(start);
        int toSlotExclusive = toSlotIndex(end);

        List<Integer> touchedSlots = slotsByClassIndex.computeIfAbsent(classIndex, k -> new ArrayList<>());
        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            roomOccupationCounts[roomIndex].merge(slot, 1, Integer::sum);
            touchedSlots.add(slot);
        }
    }

    private void indexStudentRelocation(ClassRoomAssignment assignment) {
        if (preparedEvaluationData == null || assignment == null) return;

        int classIndex = assignment.getClassIndex();
        int roomIndex = assignment.getRoomIndex();

        List<PreparedClassData> classes = preparedEvaluationData.getClasses();
        if (classIndex < 0 || classIndex >= classes.size()) return;
        if (roomIndex < 0 || roomIndex >= preparedEvaluationData.getRooms().size()) return;

        PreparedClassData preparedClass = classes.get(classIndex);
        PreparedRoomData preparedRoom = preparedEvaluationData.getRooms().get(roomIndex);

        String groupKey = buildStudentRelocationGroupKey(preparedClass);
        LocalDate day = extractDay(preparedClass);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();
        String building = normalizeText(preparedRoom.getBuilding());

        if (groupKey == null || day == null || start == null || end == null || building == null) return;

        String bucketKey = buildBucketKey(groupKey, day);
        StudentGroupSlot slot = new StudentGroupSlot(classIndex, roomIndex, start, end, building);

        studentGroupSlotsByStart.computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(start, k -> new ArrayList<>()).add(slot);
        studentGroupSlotsByEnd.computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(end, k -> new ArrayList<>()).add(slot);

        studentRelocationRefsByClassIndex.computeIfAbsent(classIndex, k -> new ArrayList<>())
                .add(new RelocationEntryRef(bucketKey, start, end));
    }

    private void indexConsecutiveRoomChange(ClassRoomAssignment assignment) {
        if (preparedEvaluationData == null || assignment == null) return;

        int classIndex = assignment.getClassIndex();
        int roomIndex = assignment.getRoomIndex();

        List<PreparedClassData> classes = preparedEvaluationData.getClasses();
        if (classIndex < 0 || classIndex >= classes.size()) return;
        if (roomIndex < 0 || roomIndex >= preparedEvaluationData.getRooms().size()) return;

        PreparedClassData preparedClass = classes.get(classIndex);
        PreparedRoomData preparedRoom = preparedEvaluationData.getRooms().get(roomIndex);

        String groupKey = buildConsecutiveRoomChangeGroupKey(preparedClass);
        LocalDate day = extractDay(preparedClass);
        LocalDateTime start = preparedClass.getStartDateTime();
        LocalDateTime end = preparedClass.getEndDateTime();
        String roomIdentity = normalizeText(preparedRoom.getRoomIdentity());

        if (groupKey == null || day == null || start == null || end == null || roomIdentity == null) return;

        String bucketKey = buildBucketKey(groupKey, day);
        ConsecutiveRoomChangeSlot slot = new ConsecutiveRoomChangeSlot(classIndex, roomIndex, start, end, roomIdentity);

        consecutiveRoomChangeSlotsByStart.computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(start, k -> new ArrayList<>()).add(slot);
        consecutiveRoomChangeSlotsByEnd.computeIfAbsent(bucketKey, k -> new HashMap<>())
                .computeIfAbsent(end, k -> new ArrayList<>()).add(slot);

        consecutiveRefsByClassIndex.computeIfAbsent(classIndex, k -> new ArrayList<>())
                .add(new ConsecutiveEntryRef(bucketKey, start, end));
    }

    private String buildStudentRelocationGroupKey(PreparedClassData preparedClass) {
        String degree = normalizeText(preparedClass.getDegree());
        String classGroup = normalizeText(preparedClass.getClassGroup());
        if (degree == null || classGroup == null) return null;
        return "degree::" + degree + "||group::" + classGroup;
    }

    private String buildConsecutiveRoomChangeGroupKey(PreparedClassData preparedClass) {
        String degree = normalizeText(preparedClass.getDegree());
        String course = normalizeText(preparedClass.getCourse());
        String classGroup = normalizeText(preparedClass.getClassGroup());
        String shift = normalizeText(preparedClass.getShift());
        if (degree == null || course == null || classGroup == null || shift == null) return null;
        return "degree::" + degree + "||course::" + course + "||group::" + classGroup + "||shift::" + shift;
    }

    private LocalDate extractDay(PreparedClassData preparedClass) {
        if (preparedClass.getDay() != null) return preparedClass.getDay();
        if (preparedClass.getStartDateTime() != null) return preparedClass.getStartDateTime().toLocalDate();
        return null;
    }

    private String buildBucketKey(String groupKey, LocalDate day) {
        return groupKey + "##" + day;
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    public record StudentGroupSlot(int classIndex, int roomIndex, LocalDateTime startDateTime, LocalDateTime endDateTime, String building) {}

    public record ConsecutiveRoomChangeSlot(int classIndex, int roomIndex, LocalDateTime startDateTime, LocalDateTime endDateTime, String roomIdentity) {}

    private record RelocationEntryRef(String bucketKey, LocalDateTime startDateTime, LocalDateTime endDateTime) {}

    private record ConsecutiveEntryRef(String bucketKey, LocalDateTime startDateTime, LocalDateTime endDateTime) {}
}