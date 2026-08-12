package pt.lourenco.optimization.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class JSONGetters {

    private Integer schedule_id;
    private Integer rooms_id;

    private List<Object> objectives;
    private List<Object> constraints;

    private Integer schedule_file_id;
    private Integer schedule_file_row_count;

    private Integer rooms_file_id;
    private Integer rooms_file_row_count;

    @JsonProperty("problem_id")
    private Integer problem_id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("problem_type")
    private String problem_type;

    @JsonProperty("problem_subtype")
    private String problem_subtype;

    @JsonProperty("selected_algorithm")
    private String selected_algorithm;

    @JsonProperty("resolution_scope")
    private String resolution_scope;

    @JsonProperty("repeated_instance_strategy")
    private String repeated_instance_strategy;

    @JsonProperty("total_rooms")
    private String total_rooms;

    @JsonProperty("mapping_data")
    private Map<String, Object> mapping_data;

    @JsonProperty("rooms_mapping_data")
    private Map<String, Object> rooms_mapping_data;

    @JsonProperty("room_feature_resolution")
    private Map<String, Object> room_feature_resolution;

    @JsonProperty("resolved_requested_room_features")
    private List<Object> resolved_requested_room_features;

    @JsonProperty("constraints_summary")
    private Map<String, Object> constraints_summary;

    @JsonProperty("instance_characteristics")
    private Map<String, Object> instance_characteristics;

    @JsonProperty("selected_constraints")
    private List<Map<String, Object>> selected_constraints;

    @JsonProperty("schedule_data")
    private Map<String, Object> schedule_data;

    @JsonProperty("rooms_data")
    private Map<String, Object> rooms_data;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}