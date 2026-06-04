package pt.lourenco.optimization.jmetal.constraints.rules;

import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;

public interface ConstraintRule {

    String getConstraintId();

    ConstraintResult evaluate(SolutionContext context, UserConstraintSelection selection);
}