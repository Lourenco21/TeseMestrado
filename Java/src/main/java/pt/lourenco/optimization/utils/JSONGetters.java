package pt.lourenco.optimization.utils;

import java.util.List;
import java.util.Map;

public class JSONGetters {

    private Integer problem_id;
    private String name;
    private String problem_type;
    private String problem_subtype;
    private Map<String, Object> mapping_data;
    private Map<String, Object> rooms_mapping_data;
    private List<Object> objectives;
    private List<Object> constraints;

    private String resolution_scope;
    private String repeated_instance_strategy;

    private Integer schedule_file_id;
    private Integer schedule_file_row_count;

    private Integer rooms_file_id;
    private Integer rooms_file_row_count;

    private Map<String, Object> constraints_summary;
    private Map<String, Object> instance_characteristics;

    public Integer getProblem_id() {
        return problem_id;
    }

    public void setProblem_id(Integer problem_id) {
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

    public String getResolution_scope() {
        return resolution_scope;
    }

    public void setResolution_scope(String resolution_scope) {
        this.resolution_scope = resolution_scope;
    }

    public String getRepeated_instance_strategy() {
        return repeated_instance_strategy;
    }

    public void setRepeated_instance_strategy(String repeated_instance_strategy) {
        this.repeated_instance_strategy = repeated_instance_strategy;
    }

    public Map<String, Object> getConstraints_summary() {
        return constraints_summary;
    }

    public void setConstraints_summary(Map<String, Object> constraints_summary) {
        this.constraints_summary = constraints_summary;
    }

    public Map<String, Object> getInstance_characteristics() {
        return instance_characteristics;
    }

    public void setInstance_characteristics(Map<String, Object> instance_characteristics) {
        this.instance_characteristics = instance_characteristics;
    }

    public Integer getSchedule_file_id() {
        return schedule_file_id;
    }

    public Integer getSchedule_file_row_count() {
        return schedule_file_row_count;
    }

    public Integer getRooms_file_id() {
        return rooms_file_id;
    }

    public Integer getRooms_file_row_count() {
        return rooms_file_row_count;
    }

    public int getObjectivesCount(){
        return objectives.size();
    }

    public void setObjectives(List<Object> objectives) {
        this.objectives = objectives;
    }

    public List<Object> getConstraints() {
        return constraints;
    }

    public int getConstraintCount(){
        return constraints.size();
    }

    public Map<String, Object> getMapping_data() {
        return mapping_data;
    }

    public Map<String, Object> getRooms_mapping_data() {
        return rooms_mapping_data;
    }

    public List<Object> getObjectives() {
        return objectives;
    }

}