/*package pt.lourenco.optimization.jmetal.problems.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.service.ConstraintEvaluationService;
import pt.lourenco.optimization.jmetal.constraints.service.SolutionContextBuilderService;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;
import pt.lourenco.optimization.jmetal.problems.problems.ScheduleOptimizationProblem;

@Service
public class ProblemFactory {

    private final ProblemInputValidator problemInputValidator;
    private final SolutionContextBuilderService solutionContextBuilderService;
    private final ConstraintEvaluationService constraintEvaluationService;

    public ProblemFactory(
            ProblemInputValidator problemInputValidator,
            SolutionContextBuilderService solutionContextBuilderService,
            ConstraintEvaluationService constraintEvaluationService
    ) {
        this.problemInputValidator = problemInputValidator;
        this.solutionContextBuilderService = solutionContextBuilderService;
        this.constraintEvaluationService = constraintEvaluationService;
    }

    public ScheduleOptimizationProblem create(ProblemInputData inputData) {
        problemInputValidator.validate(inputData);

        return new ScheduleOptimizationProblem(
                inputData,
                solutionContextBuilderService,
                constraintEvaluationService
        );
    }
}*/