package pt.lourenco.optimization.jmetal.constraints.rules;

import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.IncrementalConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;

public interface IncrementalConstraintRule {

    default boolean supportsIncrementalEvaluation() {
        return true;
    }

    IncrementalConstraintResult evaluateIncrementally(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            UserConstraintSelection selection
    );
}