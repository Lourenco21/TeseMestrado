package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintImportance;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.CandidateAssignment;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.IncrementalConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.incremental.PartialSolutionContext;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;
import pt.lourenco.optimization.jmetal.constraints.rules.IncrementalConstraintRule;

import java.util.List;

@Service
public class IncrementalConstraintEvaluationService {

    private final ConstraintRuleRegistry constraintRuleRegistry;

    public IncrementalConstraintEvaluationService(ConstraintRuleRegistry constraintRuleRegistry) {
        this.constraintRuleRegistry = constraintRuleRegistry;
    }

    public CandidateScore evaluateCandidate(
            PartialSolutionContext context,
            CandidateAssignment candidate,
            List<UserConstraintSelection> selectedConstraints
    ) {
        double hardScore = 0.0;
        double softScore = 0.0;

        if (selectedConstraints == null || selectedConstraints.isEmpty()) {
            return new CandidateScore(0.0, 0.0);
        }

        for (UserConstraintSelection selection : selectedConstraints) {
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
            double weightedViolation = applyImportanceWeight(rawViolation, selection.getImportance());

            if (selection.getGoal() == ConstraintGoal.HARD) {
                hardScore += weightedViolation;
            } else {
                softScore += weightedViolation;
            }
        }

        return new CandidateScore(hardScore, softScore);
    }

    private double applyImportanceWeight(double rawViolation, ConstraintImportance importance) {
        if (importance == null) {
            return rawViolation;
        }
        return rawViolation * importance.getWeight();
    }

    public record CandidateScore(
            double hardScore,
            double softScore
    ) {}
}