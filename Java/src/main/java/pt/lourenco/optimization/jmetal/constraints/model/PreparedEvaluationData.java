package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pt.lourenco.optimization.jmetal.partitioning.PreviousPartitionAssignmentsContext;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class PreparedEvaluationData {
    private final List<PreparedClassData> classes;
    private final List<PreparedRoomData> rooms;
    private final Map<String, List<PreviousPartitionAssignmentsContext.ResolvedAssignment>> previousAssignmentsByRoom;
}