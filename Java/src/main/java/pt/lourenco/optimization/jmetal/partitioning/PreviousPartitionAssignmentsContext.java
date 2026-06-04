package pt.lourenco.optimization.jmetal.partitioning;

import lombok.Getter;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class PreviousPartitionAssignmentsContext {

    private final List<ResolvedAssignment> assignments = new ArrayList<>();

    public void add(ResolvedAssignment assignment) {
        if (assignment != null) {
            assignments.add(assignment);
        }
    }

    public void addAll(List<ResolvedAssignment> items) {
        if (items != null) {
            assignments.addAll(items);
        }
    }

    public List<ResolvedAssignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    @Getter
    public static class ResolvedAssignment {
        private final int classIndex;
        private final int roomIndex;
        private final ClassRoomAssignment rawAssignment;
        private final LocalDateTime startDateTime;
        private final LocalDateTime endDateTime;
        private final String partitionKey;
        private final String roomIdentity;

        public ResolvedAssignment(
                int classIndex,
                int roomIndex,
                ClassRoomAssignment rawAssignment,
                LocalDateTime startDateTime,
                LocalDateTime endDateTime,
                String partitionKey,
                String roomIdentity
        ) {
            this.classIndex = classIndex;
            this.roomIndex = roomIndex;
            this.rawAssignment = rawAssignment;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
            this.partitionKey = partitionKey;
            this.roomIdentity = roomIdentity;
        }

        public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
            return otherStart.isBefore(endDateTime) && otherEnd.isAfter(startDateTime);
        }
    }
}