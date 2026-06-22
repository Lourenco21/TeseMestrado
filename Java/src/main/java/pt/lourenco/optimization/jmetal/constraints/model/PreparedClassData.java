package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
public class PreparedClassData {
    private final int classIndex;
    private final Map<String, Object> classData;

    private final String course;
    private final String classType;
    private final String teacher;
    private final String classGroup;
    private final String week;

    private final LocalDate day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;

    private final Integer students;
    private final String requestedRoomName;
    private final Set<String> requestedCharacteristics;
}