package pt.lourenco.optimization.jmetal.constraints.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintImportance;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserConstraintSelection {
    private String id;
    private ConstraintGoal goal;
    private ConstraintImportance importance;
}
