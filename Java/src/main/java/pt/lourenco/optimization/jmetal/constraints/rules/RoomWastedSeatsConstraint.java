package pt.lourenco.optimization.jmetal.constraints.rules;

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

import java.util.List;

@Component
public class RoomWastedSeatsConstraint implements ConstraintRule, IncrementalConstraintRule {

    private static final String CONSTRAINT_ID = "capacity_waste";

    @Override
    public String getConstraintId() {
        return CONSTRAINT_ID;
    }

    @Override
    public ConstraintResult evaluate(SolutionContext context, UserConstraintSelection selection) {
        double rawViolation = calculateRawViolation(context);

        return new ConstraintResult(
                CONSTRAINT_ID,
                ConstraintGoal.SOFT,
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

        double rawViolation = calculateWastedSeats(
                preparedClass.getStudents(),
                preparedRoom.getCapacity()
        );

        return new IncrementalConstraintResult(rawViolation);
    }

    private double calculateRawViolation(SolutionContext context) {
        PreparedEvaluationData prepared = context.getPreparedEvaluationData();

        if (prepared == null || context.getAssignments() == null || context.getAssignments().isEmpty()) {
            return 0.0;
        }

        List<ClassRoomAssignment> assignments = context.getAssignments();
        List<PreparedClassData> preparedClasses = prepared.getClasses();
        List<PreparedRoomData> preparedRooms = prepared.getRooms();

        double totalViolation = 0.0;

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

            totalViolation += calculateWastedSeats(
                    preparedClass.getStudents(),
                    preparedRoom.getCapacity()
            );
        }

        return totalViolation;
    }

    private double calculateWastedSeats(Integer students, Integer roomCapacity) {
        if (students == null || roomCapacity == null) {
            return 0.0;
        }

        if (students < 0 || roomCapacity < 0) {
            return 0.0;
        }

        return Math.max(0.0, (roomCapacity - students + 10) * 0.01);
    }
}