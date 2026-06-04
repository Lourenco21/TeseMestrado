package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintEvaluationResult;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintImportance;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintResult;
import pt.lourenco.optimization.jmetal.constraints.model.SolutionContext;
import pt.lourenco.optimization.jmetal.constraints.rules.ConstraintRule;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConstraintEvaluationService {

    private final ConstraintRuleRegistry constraintRuleRegistry;

    public ConstraintEvaluationService(ConstraintRuleRegistry constraintRuleRegistry) {
        this.constraintRuleRegistry = constraintRuleRegistry;
    }

    public ConstraintEvaluationResult evaluate(
            SolutionContext context,
            List<UserConstraintSelection> selectedConstraints
    ) {
        List<ConstraintResult> results = new ArrayList<>();

        double hardScore = 0.0;
        double softScore = 0.0;

        if (selectedConstraints == null || selectedConstraints.isEmpty()) {
            return new ConstraintEvaluationResult(hardScore, softScore, results);
        }

        for (UserConstraintSelection selection : selectedConstraints) {
            if (selection == null || selection.getId() == null || selection.getId().isBlank()) {
                continue;
            }

            ConstraintRule rule = constraintRuleRegistry.getById(selection.getId());
            if (rule == null) {
                continue;
            }

            ConstraintResult result = rule.evaluate(context, selection);
            double rawViolation = result.getViolationScore() == null ? 0.0 : result.getViolationScore();
            double weightedScore = applyImportanceWeight(rawViolation, selection.getImportance());

            result.setWeightedScore(weightedScore);
            results.add(result);

            if (selection.getGoal() == ConstraintGoal.HARD) {
                hardScore += weightedScore;
            } else {
                softScore += weightedScore;
            }
        }

        return new ConstraintEvaluationResult(hardScore, softScore, results);
    }

    private double applyImportanceWeight(double rawViolation, ConstraintImportance importance) {
        if (importance == null) {
            return rawViolation;
        }

        return rawViolation * importance.getWeight();
    }
}