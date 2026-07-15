package pt.lourenco.optimization.jmetal.constraints.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedClassData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedEvaluationData;
import pt.lourenco.optimization.jmetal.constraints.model.PreparedRoomData;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.IncrementalConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RoomExclusivityConstraint implements ConstraintRule, IncrementalConstraintRule {

    private static final String CONSTRAINT_ID = "room_exclusivity";

    @Override
    public String getConstraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public ConstraintResult evaluate(SolutionContext context, UserConstraintSelection selection) {
        double rawViolation = countRealOverlaps(context);
        return new ConstraintResult(CONSTRAINT_ID, ConstraintGoal.HARD, rawViolation, rawViolation);
    }

    private double countRealOverlaps(SolutionContext context) {
        if (context.getAssignments() == null || context.getAssignments().isEmpty()) {
            return 0.0;
        }

        PreparedEvaluationData prepared = context.getPreparedEvaluationData();
        List<PreparedClassData> classes = prepared.getClasses();
        List<PreparedRoomData> rooms = prepared.getRooms();

        LocalDateTime origin = classes.stream()
                .map(PreparedClassData::getStartDateTime)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        Map<Integer, List<long[]>> intervalsByRoom = new HashMap<>();

        for (ClassRoomAssignment a : context.getAssignments()) {
            int classIndex = a.getClassIndex();
            int roomIndex = a.getRoomIndex();
            if (classIndex < 0 || classIndex >= classes.size()) continue;
            if (roomIndex < 0 || roomIndex >= rooms.size()) continue;

            PreparedClassData c = classes.get(classIndex);
            if (c.getStartDateTime() == null || c.getEndDateTime() == null
                    || !c.getEndDateTime().isAfter(c.getStartDateTime())) continue;

            intervalsByRoom.computeIfAbsent(roomIndex, k -> new ArrayList<>())
                    .add(new long[]{
                            java.time.temporal.ChronoUnit.MINUTES.between(origin, c.getStartDateTime()),
                            java.time.temporal.ChronoUnit.MINUTES.between(origin, c.getEndDateTime())
                    });
        }

        double violations = 0.0;
        for (List<long[]> intervals : intervalsByRoom.values()) {
            intervals.sort((x, y) -> Long.compare(x[0], y[0]));
            for (int i = 1; i < intervals.size(); i++) {
                if (intervals.get(i)[0] < intervals.get(i - 1)[1]) {
                    violations++;
                }
            }
        }

        return violations;
    }

    @Override
    public IncrementalConstraintResult evaluateIncrementally(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            UserConstraintSelection selection
    ) {
        PreparedEvaluationData prepared = context.getPreparedEvaluationData();
        PreparedClassData preparedClass = prepared.getClasses().get(candidate.classIndex());

        if (preparedClass.getStartDateTime() == null
                || preparedClass.getEndDateTime() == null
                || !preparedClass.getEndDateTime().isAfter(preparedClass.getStartDateTime())) {
            return new IncrementalConstraintResult(1.0);
        }

        if (!context.isRoomAvailable(
                candidate.roomIndex(),
                preparedClass.getStartDateTime(),
                preparedClass.getEndDateTime()
        )) {
            return new IncrementalConstraintResult(1.0);
        }

        return IncrementalConstraintResult.zero();
    }

}
