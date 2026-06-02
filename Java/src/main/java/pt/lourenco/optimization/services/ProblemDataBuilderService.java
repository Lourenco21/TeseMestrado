package pt.lourenco.optimization.services;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.utils.JSONGetters;

@Service
public class ProblemDataBuilderService {

    public String buildProblemData(JSONGetters request) {
        StringBuilder sb = new StringBuilder();

        sb.append("name: ").append(valueOrNull(request.getName())).append("\n");
        sb.append("problem_type: ").append(valueOrNull(request.getProblem_type())).append("\n");
        sb.append("problem_subtype: ").append(valueOrNull(request.getProblem_subtype())).append("\n");
        sb.append("resolution_scope: ").append(valueOrNull(request.getResolution_scope())).append("\n");
        sb.append("repeated_instance_strategy: ").append(valueOrNull(request.getRepeated_instance_strategy())).append("\n");
        sb.append("\n");

        sb.append("constraints_summary:\n");
        sb.append(valueOrNull(request.getConstraints_summary())).append("\n\n");

        sb.append("instance_characteristics:\n");
        sb.append(valueOrNull(request.getInstance_characteristics())).append("\n");

        return sb.toString();
    }

    public String buildExecutionProblemData(JSONGetters request) {
        StringBuilder sb = new StringBuilder();

        sb.append("name: ").append(valueOrNull(request.getName())).append("\n");
        sb.append("problem_type: ").append(valueOrNull(request.getProblem_type())).append("\n");
        sb.append("problem_subtype: ").append(valueOrNull(request.getProblem_subtype())).append("\n");
        sb.append("resolution_scope: ").append(valueOrNull(request.getResolution_scope())).append("\n");
        sb.append("repeated_instance_strategy: ").append(valueOrNull(request.getRepeated_instance_strategy())).append("\n");
        sb.append("selected_algorithm: ").append(valueOrNull(request.getSelected_algorithm())).append("\n");
        sb.append("\n");

        sb.append("constraints_summary:\n");
        sb.append(valueOrNull(request.getConstraints_summary())).append("\n\n");

        sb.append("selected_constraints:\n");
        sb.append(valueOrNull(request.getSelected_constraints())).append("\n\n");

        sb.append("instance_characteristics:\n");
        sb.append(valueOrNull(request.getInstance_characteristics())).append("\n\n");

        return sb.toString();
    }

    private String valueOrNull(Object value) {
        return value == null ? "null" : value.toString();
    }
}