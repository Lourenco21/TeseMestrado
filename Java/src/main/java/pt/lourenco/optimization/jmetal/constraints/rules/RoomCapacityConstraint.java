package pt.lourenco.optimization.jmetal.constraints.rules;

import org.springframework.stereotype.Component;
import pt.lourenco.optimization.utils.NumericParsingUtils;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.problems.mapping.RoomsMappingUtils;
import pt.lourenco.optimization.jmetal.problems.mapping.ScheduleMappingUtils;
import pt.lourenco.optimization.jmetal.problems.model.ClassRoomAssignment;

import java.util.List;
import java.util.Map;

@Component
public class RoomCapacityConstraint implements ConstraintRule {

    private static final String CONSTRAINT_ID = "room_capacity_sufficiency";

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

    private double calculateRawViolation(SolutionContext context) {
        List<ClassRoomAssignment> assignments = context.getAssignments();

        if (assignments == null || assignments.isEmpty()) {
            return 0.0;
        }

        double totalViolation = 0.0;

        for (ClassRoomAssignment assignment : assignments) {
            Map<String, Object> classData = assignment.getClassData();
            Map<String, Object> roomData = assignment.getRoomData();

            String studentsRaw = ScheduleMappingUtils.getStudents(classData, context.getMappingData());
            String capacityRaw = RoomsMappingUtils.getCapacity(roomData, context.getRoomsMappingData());

            Integer students = NumericParsingUtils.parseIntegerSafely(studentsRaw);
            Integer capacity = NumericParsingUtils.parseIntegerSafely(capacityRaw);

            if (students == null || capacity == null) {
                totalViolation += 1.0;
                continue;
            }

            if (students > capacity) {
                totalViolation += 1.0;
            }
        }

        return totalViolation;
    }
}