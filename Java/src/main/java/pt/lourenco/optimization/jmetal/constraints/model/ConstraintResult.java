package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintResult {
    private String constraintId;
    private ConstraintGoal goal;
    private Double violationScore;
    private Double weightedScore;
}
