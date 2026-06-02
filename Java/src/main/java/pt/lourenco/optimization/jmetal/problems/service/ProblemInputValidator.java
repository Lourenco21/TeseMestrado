package pt.lourenco.optimization.jmetal.problems.service;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.util.List;
import java.util.Map;

@Service
public class ProblemInputValidator {

    public void validate(ProblemInputData inputData) {
        if (inputData == null) {
            throw new IllegalArgumentException("Problem input data cannot be null.");
        }

        if (inputData.getProblemId() == null) {
            throw new IllegalArgumentException("Problem id is required.");
        }

        validateScheduleData(inputData.getScheduleData());
        validateRoomsData(inputData.getRoomsData());
    }

    private void validateScheduleData(Map<String, Object> scheduleData) {
        if (scheduleData == null || scheduleData.isEmpty()) {
            throw new IllegalArgumentException("Schedule data is required.");
        }

        Object classesObject = scheduleData.get("classes");
        if (!(classesObject instanceof List<?> classes) || classes.isEmpty()) {
            throw new IllegalArgumentException("Schedule data must contain a non-empty 'classes' list.");
        }
    }

    private void validateRoomsData(Map<String, Object> roomsData) {
        if (roomsData == null || roomsData.isEmpty()) {
            throw new IllegalArgumentException("Rooms data is required.");
        }

        Object roomsObject = roomsData.get("rooms");
        if (!(roomsObject instanceof List<?> rooms) || rooms.isEmpty()) {
            throw new IllegalArgumentException("Rooms data must contain a non-empty 'rooms' list.");
        }
    }
}
