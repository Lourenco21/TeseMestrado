package pt.lourenco.optimization.services;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.utils.JSONGetters;

@Service
public class ProblemDataBuilderService {

    public String buildProblemData(JSONGetters request) {
        StringBuilder sb = new StringBuilder();

        //sb.append("problem_id: ").append(valueOrNull(request.getProblem_id())).append("\n");
        sb.append("name: ").append(valueOrNull(request.getName())).append("\n");
        sb.append("problem_type: ").append(valueOrNull(request.getProblem_type())).append("\n");
        sb.append("problem_subtype: ").append(valueOrNull(request.getProblem_subtype())).append("\n");
        //sb.append("schedule_file_id: ").append(valueOrNull(request.getSchedule_file_id())).append("\n");
        sb.append("number_of_variables: ").append(valueOrNull(request.getSchedule_file_row_count())).append("\n");
        //sb.append("rooms_file_id: ").append(valueOrNull(request.getRooms_file_id())).append("\n");
        sb.append("values_variable_can_assume: ").append(valueOrNull(request.getRooms_file_row_count())).append("\n");
        sb.append("\n");

        //sb.append("mapping_data:\n");
        //sb.append(valueOrNull(request.getMapping_data())).append("\n\n");

        //sb.append("rooms_mapping_data:\n");
        //sb.append(valueOrNull(request.getRooms_mapping_data())).append("\n\n");

        sb.append("number_of_objectives: ").append(valueOrNull(request.getObjectivesCount())).append("\n");
        sb.append("objectives:\n");
        sb.append(valueOrNull(request.getObjectives())).append("\n\n");

        sb.append("number_of_constraints: ").append(valueOrNull(request.getConstraintCount())).append("\n");
        sb.append("constraints:\n");
        sb.append(valueOrNull(request.getConstraints())).append("\n");

        return sb.toString();
    }

    private String valueOrNull(Object value) {
        return value == null ? "null" : value.toString();
    }
}