package pt.lourenco.optimization.django.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DjangoProblemDataResponse {

    private Map<String, Object> schedule_data;
    private Map<String, Object> rooms_data;

    public Map<String, Object> getScheduleData() {
        return schedule_data;
    }

    public Map<String, Object> getRoomsData() {
        return rooms_data;
    }
}