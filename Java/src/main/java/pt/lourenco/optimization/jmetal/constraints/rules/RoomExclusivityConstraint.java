package pt.lourenco.optimization.jmetal.constraints.rules;

import lombok.extern.slf4j.Slf4j;
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
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RoomExclusivityConstraint implements ConstraintRule, IncrementalConstraintRule {

    private static final String CONSTRAINT_ID = "room_exclusivity";

    private static final boolean TRACE_SUMMARY = true;
    private static final boolean TRACE_DETAILS = true;
    private static final int MAX_DETAILED_CONFLICTS = 25;

    @Override
    public String getConstraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public ConstraintResult evaluate(SolutionContext context, UserConstraintSelection selection) {
        RoomConflictStats stats = calculateStats(context);

//        if (TRACE_SUMMARY) {
//            log.info(
//                    "[ROOM_EXCLUSIVITY] currentOccupations={} previousAssignments={} internalPairs={} previousHits={} violatingCurrentClasses={} topRooms={}",
//                    stats.currentOccupationCount,
//                    stats.previousAssignmentCount,
//                    stats.internalConflictPairs,
//                    stats.previousConflictHits,
//                    stats.violatingCurrentClassIndexes.size(),
//                    stats.topRoomSummary()
//            );
//        }
//
//        if (TRACE_DETAILS && !stats.detailedConflicts.isEmpty()) {
//            int limit = Math.min(MAX_DETAILED_CONFLICTS, stats.detailedConflicts.size());
//            for (int i = 0; i < limit; i++) {
//                log.warn("[ROOM_EXCLUSIVITY][DETAIL {}] {}", i + 1, stats.detailedConflicts.get(i));
//            }
//
//            if (stats.detailedConflicts.size() > limit) {
//                log.warn(
//                        "[ROOM_EXCLUSIVITY] {} additional conflicts omitted from detailed log",
//                        stats.detailedConflicts.size() - limit
//                );
//            }
//        }

        double rawViolation = stats.violatingCurrentClassIndexes.size();

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
        PreparedClassData preparedClass = prepared.getClasses().get(candidate.classIndex());
        PreparedRoomData preparedRoom = prepared.getRooms().get(candidate.roomIndex());

        if (preparedClass.getStartDateTime() == null
                || preparedClass.getEndDateTime() == null
                || !preparedClass.getEndDateTime().isAfter(preparedClass.getStartDateTime())) {
            return new IncrementalConstraintResult(1.0);
        }

        String roomIdentity = normalizeRoomIdentity(preparedRoom.getRoomIdentity());
        if (roomIdentity.isBlank()) {
            return new IncrementalConstraintResult(1.0);
        }

        List<PartialSolutionContext.OccupiedRoomSlot> roomOccupations =
                context.getOccupationsByRoom().getOrDefault(roomIdentity, List.of());

        for (PartialSolutionContext.OccupiedRoomSlot occupied : roomOccupations) {
            if (overlaps(
                    preparedClass.getStartDateTime(),
                    preparedClass.getEndDateTime(),
                    occupied.startDateTime(),
                    occupied.endDateTime()
            )) {
                return new IncrementalConstraintResult(1.0);
            }
        }

        Map<String, List<PreviousPartitionAssignmentsContext.ResolvedAssignment>> previousByRoom =
                prepared.getPreviousAssignmentsByRoom();

        if (previousByRoom != null && !previousByRoom.isEmpty()) {
            List<PreviousPartitionAssignmentsContext.ResolvedAssignment> previousAssignments =
                    previousByRoom.getOrDefault(roomIdentity, List.of());

            for (PreviousPartitionAssignmentsContext.ResolvedAssignment previous : previousAssignments) {
                if (previous.overlaps(preparedClass.getStartDateTime(), preparedClass.getEndDateTime())) {
                    return new IncrementalConstraintResult(1.0);
                }
            }
        }

        return IncrementalConstraintResult.zero();
    }

    private RoomConflictStats calculateStats(SolutionContext context) {
        PreparedEvaluationData prepared = context.getPreparedEvaluationData();

        if (prepared == null || context.getAssignments() == null || context.getAssignments().isEmpty()) {
            return RoomConflictStats.empty();
        }

        List<CurrentOccupation> currentOccupations = buildCurrentOccupations(context, prepared);
        if (currentOccupations.isEmpty()) {
            return RoomConflictStats.empty();
        }

        RoomConflictStats stats = new RoomConflictStats();
        stats.currentOccupationCount = currentOccupations.size();
        stats.previousAssignmentCount = countPreviousAssignments(prepared.getPreviousAssignmentsByRoom());

        markInternalConflicts(currentOccupations, stats);
        markPreviousPartitionConflicts(currentOccupations, prepared, stats);

        return stats;
    }

    private List<CurrentOccupation> buildCurrentOccupations(
            SolutionContext context,
            PreparedEvaluationData prepared
    ) {
        List<ClassRoomAssignment> assignments = context.getAssignments();
        List<PreparedClassData> preparedClasses = prepared.getClasses();
        List<PreparedRoomData> preparedRooms = prepared.getRooms();

        List<CurrentOccupation> occupations = new ArrayList<>(assignments.size());

        for (ClassRoomAssignment assignment : assignments) {
            int classIndex = assignment.getClassIndex();
            int roomIndex = assignment.getRoomIndex();

            if (classIndex < 0 || classIndex >= preparedClasses.size()) {
                continue;
            }

            if (roomIndex < 0 || roomIndex >= preparedRooms.size()) {
                continue;
            }

            PreparedClassData preparedClass = preparedClasses.get(classIndex);
            PreparedRoomData preparedRoom = preparedRooms.get(roomIndex);

            if (preparedClass.getStartDateTime() == null
                    || preparedClass.getEndDateTime() == null
                    || !preparedClass.getEndDateTime().isAfter(preparedClass.getStartDateTime())) {
                continue;
            }

            occupations.add(new CurrentOccupation(
                    classIndex,
                    roomIndex,
                    normalizeRoomIdentity(preparedRoom.getRoomIdentity()),
                    preparedClass.getStartDateTime(),
                    preparedClass.getEndDateTime()
            ));
        }

        return occupations;
    }

    private void markInternalConflicts(
            List<CurrentOccupation> occupations,
            RoomConflictStats stats
    ) {
        Map<String, List<CurrentOccupation>> byRoom = occupations.stream()
                .filter(o -> o.roomIdentity() != null && !o.roomIdentity().isBlank())
                .collect(Collectors.groupingBy(CurrentOccupation::roomIdentity));

        for (Map.Entry<String, List<CurrentOccupation>> entry : byRoom.entrySet()) {
            String roomIdentity = entry.getKey();
            List<CurrentOccupation> roomOccupations = entry.getValue();

            roomOccupations.sort(Comparator
                    .comparing(CurrentOccupation::startDateTime)
                    .thenComparing(CurrentOccupation::endDateTime)
                    .thenComparing(CurrentOccupation::classIndex));

            for (int i = 0; i < roomOccupations.size(); i++) {
                CurrentOccupation current = roomOccupations.get(i);

                for (int j = i + 1; j < roomOccupations.size(); j++) {
                    CurrentOccupation next = roomOccupations.get(j);

                    if (!next.startDateTime().isBefore(current.endDateTime())) {
                        break;
                    }

                    if (overlaps(
                            current.startDateTime(),
                            current.endDateTime(),
                            next.startDateTime(),
                            next.endDateTime()
                    )) {
                        stats.internalConflictPairs++;
                        stats.violatingCurrentClassIndexes.add(current.classIndex());
                        stats.violatingCurrentClassIndexes.add(next.classIndex());
                        stats.roomConflictCount.merge(roomIdentity, 1, Integer::sum);

                        stats.addDetail(String.format(
                                "INTERNAL room=%s classA=%d [%s -> %s] classB=%d [%s -> %s]",
                                roomIdentity,
                                current.classIndex(),
                                current.startDateTime(),
                                current.endDateTime(),
                                next.classIndex(),
                                next.startDateTime(),
                                next.endDateTime()
                        ));
                    }
                }
            }
        }
    }

    private void markPreviousPartitionConflicts(
            List<CurrentOccupation> currentOccupations,
            PreparedEvaluationData prepared,
            RoomConflictStats stats
    ) {
        Map<String, List<PreviousPartitionAssignmentsContext.ResolvedAssignment>> previousByRoom =
                prepared.getPreviousAssignmentsByRoom();

        if (previousByRoom == null || previousByRoom.isEmpty()) {
            return;
        }

        for (CurrentOccupation current : currentOccupations) {
            if (current.roomIdentity() == null || current.roomIdentity().isBlank()) {
                continue;
            }

            List<PreviousPartitionAssignmentsContext.ResolvedAssignment> previousAssignments =
                    previousByRoom.getOrDefault(current.roomIdentity(), List.of());

            for (PreviousPartitionAssignmentsContext.ResolvedAssignment previous : previousAssignments) {
                if (previous.overlaps(current.startDateTime(), current.endDateTime())) {
                    stats.previousConflictHits++;
                    stats.violatingCurrentClassIndexes.add(current.classIndex());
                    stats.roomConflictCount.merge(current.roomIdentity(), 1, Integer::sum);

                    stats.addDetail(String.format(
                            "PREVIOUS room=%s currentClass=%d [%s -> %s] previousClass=%d [%s -> %s] partition=%s",
                            current.roomIdentity(),
                            current.classIndex(),
                            current.startDateTime(),
                            current.endDateTime(),
                            previous.getClassIndex(),
                            previous.getStartDateTime(),
                            previous.getEndDateTime(),
                            previous.getPartitionKey()
                    ));

                    break;
                }
            }
        }
    }

    private int countPreviousAssignments(
            Map<String, List<PreviousPartitionAssignmentsContext.ResolvedAssignment>> previousByRoom
    ) {
        if (previousByRoom == null || previousByRoom.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (List<PreviousPartitionAssignmentsContext.ResolvedAssignment> list : previousByRoom.values()) {
            total += list.size();
        }
        return total;
    }

    private boolean overlaps(
            LocalDateTime startA,
            LocalDateTime endA,
            LocalDateTime startB,
            LocalDateTime endB
    ) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }

    private String normalizeRoomIdentity(String roomIdentity) {
        return roomIdentity == null ? "" : roomIdentity.trim().toLowerCase();
    }

    private record CurrentOccupation(
            int classIndex,
            int roomIndex,
            String roomIdentity,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {}

    private static class RoomConflictStats {
        private int currentOccupationCount;
        private int previousAssignmentCount;
        private int internalConflictPairs;
        private int previousConflictHits;

        private final Set<Integer> violatingCurrentClassIndexes = new HashSet<>();
        private final Map<String, Integer> roomConflictCount = new HashMap<>();
        private final List<String> detailedConflicts = new ArrayList<>();

        static RoomConflictStats empty() {
            return new RoomConflictStats();
        }

        void addDetail(String detail) {
            detailedConflicts.add(detail);
        }

        String topRoomSummary() {
            return roomConflictCount.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(10)
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
        }
    }
}