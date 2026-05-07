package pt.lourenco.optimization.utils;

import java.util.List;
import java.util.Map;


public class JSONGetters {

    private Long problem_id;
    private String name;
    private String problem_type;
    private String problem_subtype;
    private Long schedule_file_id;
    private Long rooms_file_id;
    private Map<String, Object> mapping_data;
    private Map<String, Object> rooms_mapping_data;
    private List<Object> objectives;
    private List<Object> constraints;

    public Long getProblem_id() {
        return problem_id;
    }

    public void setProblem_id(Long problem_id) {
        this.problem_id = problem_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProblem_type() {
        return problem_type;
    }

    public void setProblem_type(String problem_type) {
        this.problem_type = problem_type;
    }

    public String getProblem_subtype() {
        return problem_subtype;
    }

    public void setProblem_subtype(String problem_subtype) {
        this.problem_subtype = problem_subtype;
    }

    public Long getSchedule_file_id() {
        return schedule_file_id;
    }

    public void setSchedule_file_id(Long schedule_file_id) {
        this.schedule_file_id = schedule_file_id;
    }

    public Long getRooms_file_id() {
        return rooms_file_id;
    }

    public void setRooms_file_id(Long rooms_file_id) {
        this.rooms_file_id = rooms_file_id;
    }

    public Map<String, Object> getMapping_data() {
        return mapping_data;
    }

    public void setMapping_data(Map<String, Object> mapping_data) {
        this.mapping_data = mapping_data;
    }

    public Map<String, Object> getRooms_mapping_data() {
        return rooms_mapping_data;
    }

    public void setRooms_mapping_data(Map<String, Object> rooms_mapping_data) {
        this.rooms_mapping_data = rooms_mapping_data;
    }

    public List<Object> getObjectives() {
        return objectives;
    }

    public void setObjectives(List<Object> objectives) {
        this.objectives = objectives;
    }

    public List<Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Object> constraints) {
        this.constraints = constraints;
    }

}
