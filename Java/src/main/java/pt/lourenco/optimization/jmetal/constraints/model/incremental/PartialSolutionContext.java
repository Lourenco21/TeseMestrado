package pt.lourenco.optimization.jmetal.constraints.model.incremental;

import lombok.Getter;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

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

    public PartialSolutionContext(PreparedEvaluationData preparedEvaluationData) {
        this.preparedEvaluationData = preparedEvaluationData;
    }

    public void addAssignment(ClassRoomAssignment assignment) {
        assignments.add(assignment);
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

    public record OccupiedRoomSlot(
            int classIndex,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {}
}