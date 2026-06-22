package pt.lourenco.optimization.jmetal.partitioning;

import lombok.Getter;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PreviousPartitionAssignmentsContext {

    private final Map<String, ResolvedAssignment> assignmentsByKey = new LinkedHashMap<>();

    public void add(ResolvedAssignment assignment) {
        if (assignment == null) {
            return;
        }

        String key = assignment.getUniqueKey();
        if (key == null || key.isBlank()) {
            return;
        }

        assignmentsByKey.put(key, assignment);
    }

    public void addAll(List<ResolvedAssignment> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (ResolvedAssignment item : items) {
            add(item);
        }
    }

    public void removeEndedBefore(LocalDateTime threshold) {
        if (threshold == null || assignmentsByKey.isEmpty()) {
            return;
        }

        assignmentsByKey.values().removeIf(assignment ->
                assignment.getEndDateTime() == null
                        || !assignment.getEndDateTime().isAfter(threshold));
    }

    public List<ResolvedAssignment> getAssignments() {
        return Collections.unmodifiableList(new ArrayList<>(assignmentsByKey.values()));
    }

    @Getter
    public static class ResolvedAssignment {
        private final String uniqueKey;
        private final int classIndex;
        private final int roomIndex;
        private final ClassRoomAssignment rawAssignment;
        private final LocalDateTime startDateTime;
        private final LocalDateTime endDateTime;
        private final String partitionKey;
        private final String roomIdentity;

        public ResolvedAssignment(
                String uniqueKey,
                int classIndex,
                int roomIndex,
                ClassRoomAssignment rawAssignment,
                LocalDateTime startDateTime,
                LocalDateTime endDateTime,
                String partitionKey,
                String roomIdentity
        ) {
            this.uniqueKey = uniqueKey;
            this.classIndex = classIndex;
            this.roomIndex = roomIndex;
            this.rawAssignment = rawAssignment;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
            this.partitionKey = partitionKey;
            this.roomIdentity = roomIdentity == null ? "" : roomIdentity.trim().toLowerCase();
        }

        public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
            if (otherStart == null || otherEnd == null || startDateTime == null || endDateTime == null) {
                return false;
            }

            return otherStart.isBefore(endDateTime) && otherEnd.isAfter(startDateTime);
        }
    }
}