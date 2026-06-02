package pt.lourenco.optimization.llm.dto;

import java.util.List;
import java.util.Map;

public class ProblemExecutionPayloadDto {

    private ProblemDto problem;
    private ExecutionDto execution;
    private ConstraintsDto constraints;
    private InstanceCharacteristicsDto instanceCharacteristics;
    private DraftContextDto draftContext;

    public ProblemDto getProblem() {
        return problem;
    }

    public void setProblem(ProblemDto problem) {
        this.problem = problem;
    }

    public ExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(ExecutionDto execution) {
        this.execution = execution;
    }

    public ConstraintsDto getConstraints() {
        return constraints;
    }

    public void setConstraints(ConstraintsDto constraints) {
        this.constraints = constraints;
    }

    public InstanceCharacteristicsDto getInstanceCharacteristics() {
        return instanceCharacteristics;
    }

    public void setInstanceCharacteristics(InstanceCharacteristicsDto instanceCharacteristics) {
        this.instanceCharacteristics = instanceCharacteristics;
    }

    public DraftContextDto getDraftContext() {
        return draftContext;
    }

    public void setDraftContext(DraftContextDto draftContext) {
        this.draftContext = draftContext;
    }

    public static class ProblemDto {
        private Long id;
        private String name;
        private String type;
        private String subtype;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSubtype() {
            return subtype;
        }

        public void setSubtype(String subtype) {
            this.subtype = subtype;
        }
    }

    public static class ExecutionDto {
        private String resolutionScope;
        private String repeatedInstanceStrategy;
        private String selectedAlgorithm;

        public String getResolutionScope() {
            return resolutionScope;
        }

        public void setResolutionScope(String resolutionScope) {
            this.resolutionScope = resolutionScope;
        }

        public String getRepeatedInstanceStrategy() {
            return repeatedInstanceStrategy;
        }

        public void setRepeatedInstanceStrategy(String repeatedInstanceStrategy) {
            this.repeatedInstanceStrategy = repeatedInstanceStrategy;
        }

        public String getSelectedAlgorithm() {
            return selectedAlgorithm;
        }

        public void setSelectedAlgorithm(String selectedAlgorithm) {
            this.selectedAlgorithm = selectedAlgorithm;
        }
    }

    public static class ConstraintsDto {
        private String summary;
        private List<String> selected;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getSelected() {
            return selected;
        }

        public void setSelected(List<String> selected) {
            this.selected = selected;
        }
    }

    public static class InstanceCharacteristicsDto {
        private Integer totalClasses;
        private Map<String, Object> selectedPartitionStatistics;

        public Integer getTotalClasses() {
            return totalClasses;
        }

        public void setTotalClasses(Integer totalClasses) {
            this.totalClasses = totalClasses;
        }

        public Map<String, Object> getSelectedPartitionStatistics() {
            return selectedPartitionStatistics;
        }

        public void setSelectedPartitionStatistics(Map<String, Object> selectedPartitionStatistics) {
            this.selectedPartitionStatistics = selectedPartitionStatistics;
        }
    }

    public static class DraftContextDto {
        private Long scheduleId;
        private Long roomsId;
        private Map<String, Object> mappingData;
        private Map<String, Object> roomsMappingData;

        public Long getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(Long scheduleId) {
            this.scheduleId = scheduleId;
        }

        public Long getRoomsId() {
            return roomsId;
        }

        public void setRoomsId(Long roomsId) {
            this.roomsId = roomsId;
        }

        public Map<String, Object> getMappingData() {
            return mappingData;
        }

        public void setMappingData(Map<String, Object> mappingData) {
            this.mappingData = mappingData;
        }

        public Map<String, Object> getRoomsMappingData() {
            return roomsMappingData;
        }

        public void setRoomsMappingData(Map<String, Object> roomsMappingData) {
            this.roomsMappingData = roomsMappingData;
        }
    }
}