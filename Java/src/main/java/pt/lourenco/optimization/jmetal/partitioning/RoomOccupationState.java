package pt.lourenco.optimization.jmetal.partitioning;

import java.time.LocalDateTime;
import java.util.*;

public class RoomOccupationState {

    private final Map<Integer, List<OccupiedInterval>> occupiedByRoom = new HashMap<>();

    public void occupy(int roomIndex, LocalDateTime start, LocalDateTime end, int classIndex) {
        occupiedByRoom
                .computeIfAbsent(roomIndex, key -> new ArrayList<>())
                .add(new OccupiedInterval(start, end, classIndex));
    }

    public boolean isOccupied(int roomIndex, LocalDateTime start, LocalDateTime end) {
        List<OccupiedInterval> intervals = occupiedByRoom.get(roomIndex);
        if (intervals == null || intervals.isEmpty()) {
            return false;
        }

        for (OccupiedInterval interval : intervals) {
            boolean overlaps = start.isBefore(interval.end()) && end.isAfter(interval.start());
            if (overlaps) {
                return true;
            }
        }

        return false;
    }

    public List<OccupiedInterval> getIntervals(int roomIndex) {
        return occupiedByRoom.getOrDefault(roomIndex, List.of());
    }

    public record OccupiedInterval(LocalDateTime start, LocalDateTime end, int classIndex) { }
}