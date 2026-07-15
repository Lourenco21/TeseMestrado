package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.IncrementalConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;
import pt.lourenco.optimization.jmetal.constraints.rules.IncrementalConstraintRule;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncrementalConstraintEvaluationService {

    private final ConstraintRuleRegistry constraintRuleRegistry;

    public IncrementalConstraintEvaluationService(ConstraintRuleRegistry constraintRuleRegistry) {
        this.constraintRuleRegistry = constraintRuleRegistry;
    }

    public boolean isHardFeasible(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            List<UserConstraintSelection> hardConstraints
    ) {
        return checkHardConstraints(context, candidate, hardConstraints).satisfied();
    }

    public HardConstraintCheckResult checkHardConstraints(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            List<UserConstraintSelection> hardConstraints
    ) {
        List<String> violatedConstraintIds = new ArrayList<>();

        if (hardConstraints == null || hardConstraints.isEmpty()) {
            return new HardConstraintCheckResult(true, violatedConstraintIds);
        }

        for (UserConstraintSelection selection : hardConstraints) {
            if (selection == null || selection.getId() == null || selection.getId().isBlank()) {
                continue;
            }

            ConstraintRule rule = constraintRuleRegistry.getById(selection.getId());
            if (!(rule instanceof IncrementalConstraintRule incrementalRule)) {
                continue;
            }

            IncrementalConstraintResult result =
                    incrementalRule.evaluateIncrementally(context, candidate, selection);

            double rawViolation = result == null ? 0.0 : result.rawViolation();
            if (rawViolation > 0.0) {
                violatedConstraintIds.add(selection.getId());

            }
        }

        return new HardConstraintCheckResult(violatedConstraintIds.isEmpty(), violatedConstraintIds);
    }

    public record HardConstraintCheckResult(
            boolean satisfied,
            List<String> violatedConstraintIds
    ) {}
}
