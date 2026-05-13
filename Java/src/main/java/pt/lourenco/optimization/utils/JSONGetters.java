package pt.lourenco.optimization.utils;

import java.util.List;
import java.util.Map;

public class JSONGetters {

    private Integer problem_id;
    private String name;
    private String problem_type;
    private String problem_subtype;

    private Integer schedule_file_id;
    private Integer schedule_file_row_count;

    private Integer rooms_file_id;
    private Integer rooms_file_row_count;

    private Map<String, Object> mapping_data;
    private Map<String, Object> rooms_mapping_data;
    private List<Object> objectives;
    private List<Object> constraints;

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

    public Integer getSchedule_file_id() {
        return schedule_file_id;
    }

    public void setSchedule_file_id(Integer schedule_file_id) {
        this.schedule_file_id = schedule_file_id;
    }

    public Integer getSchedule_file_row_count() {
        return schedule_file_row_count;
    }

    public void setSchedule_file_row_count(Integer schedule_file_row_count) {
        this.schedule_file_row_count = schedule_file_row_count;
    }

    public Integer getRooms_file_id() {
        return rooms_file_id;
    }

    public void setRooms_file_id(Integer rooms_file_id) {
        this.rooms_file_id = rooms_file_id;
    }

    public Integer getRooms_file_row_count() {
        return rooms_file_row_count;
    }

    public void setRooms_file_row_count(Integer rooms_file_row_count) {
        this.rooms_file_row_count = rooms_file_row_count;
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

    public void setConstraints(List<Object> constraints) {
        this.constraints = constraints;
    }
}