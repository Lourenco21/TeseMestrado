package pt.lourenco.optimization.jmetal.constraints.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
public class PreparedRoomData {
    private final int roomIndex;
    private final Map<String, Object> roomData;

    private final String roomIdentity;
    private final String roomName;
    private final String building;
    private final Integer capacity;
    private final Set<String> characteristics;
}