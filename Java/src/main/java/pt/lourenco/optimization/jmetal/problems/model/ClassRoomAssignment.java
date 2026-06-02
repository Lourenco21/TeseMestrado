package pt.lourenco.optimization.jmetal.problems.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ClassRoomAssignment {
    private int classIndex;
    private Map<String, Object> classData;
    private int roomIndex;
    private Map<String, Object> roomData;
}
