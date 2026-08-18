package pt.lourenco.optimization.jmetal.constraints.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.constraints.dto.UserConstraintSelection;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintGoal;
import pt.lourenco.optimization.jmetal.constraints.model.ConstraintImportance;
import pt.lourenco.optimization.utils.JSONGetters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConstraintSelectionMapper {

    public List<UserConstraintSelection> mapFromRequest(JSONGetters request) {
        if (request == null) {
            return new ArrayList<>();
        }

        return mapFromConstraintsSummary(request.getConstraints_summary());
    }

    public List<UserConstraintSelection> mapFromConstraintsSummary(Map<String, Object> constraintsSummary) {
        List<UserConstraintSelection> result = new ArrayList<>();

        if (constraintsSummary == null) {
            return result;
        }

        Object selectedObject = constraintsSummary.get("selected");
        if (!(selectedObject instanceof List<?> selectedList)) {
            return result;
        }

        for (Object item : selectedList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }

            String id = rawMap.get("id") == null ? null : String.valueOf(rawMap.get("id"));
            String goal = rawMap.get("goal") == null ? null : String.valueOf(rawMap.get("goal"));
            String importance = rawMap.get("importance") == null ? null : String.valueOf(rawMap.get("importance"));

            if(ConstraintGoal.fromString(goal)==ConstraintGoal.HARD){
                UserConstraintSelection selection = new UserConstraintSelection(
                        id,
                        ConstraintGoal.fromString(goal),
                        null
                );
                result.add(selection);
            }else{
                UserConstraintSelection selection = new UserConstraintSelection(
                        id,
                        ConstraintGoal.fromString(goal),
                        ConstraintImportance.fromString(importance)
                );
                result.add(selection);
            }
        }

        return result;
    }
}
