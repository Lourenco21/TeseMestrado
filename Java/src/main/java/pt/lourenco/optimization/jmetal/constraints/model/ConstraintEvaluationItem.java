package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConstraintEvaluationItem {
    private String constraintId;
    private ConstraintGoal goal;
    private ConstraintImportance importance;
    private double rawViolation;
    private double weightedViolation;
    private boolean violated;
}
