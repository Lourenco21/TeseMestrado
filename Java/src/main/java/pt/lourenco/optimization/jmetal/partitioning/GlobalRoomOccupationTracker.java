package pt.lourenco.optimization.jmetal.partitioning;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Single shared room-occupation structure for an ENTIRE execution (all partitions),
 * replacing PreviousPartitionAssignmentsContext for room_exclusivity purposes.
 *
 * One Map<slot, occupantCount> per room, indexed by a global slot number computed
 * from a single scheduleOriginDateTime shared across all partitions. This is the
 * single source of truth for room availability, across the whole semester/instance,
 * not just within one partition.
 *
 * Uses a count per slot (instead of a single bit) so that removing one class's
 * occupation never accidentally clears a slot still occupied by another class.
 */
public class GlobalRoomOccupationTracker {

    private static final int SLOT_MINUTES = 30;

    private final Map<Integer, Integer>[] roomOccupationCounts;
    private final LocalDateTime scheduleOriginDateTime;

    @SuppressWarnings("unchecked")
    public GlobalRoomOccupationTracker(int numberOfRooms, LocalDateTime scheduleOriginDateTime) {
        this.scheduleOriginDateTime = scheduleOriginDateTime;
        this.roomOccupationCounts = new Map[numberOfRooms];
        for (int i = 0; i < numberOfRooms; i++) {
            this.roomOccupationCounts[i] = new HashMap<>();
        }
    }

    public int toSlotIndex(LocalDateTime dateTime) {
        long minutesBetween = ChronoUnit.MINUTES.between(scheduleOriginDateTime, dateTime);
        return (int) (minutesBetween / SLOT_MINUTES);
    }

    public boolean isRoomAvailable(int roomIndex, LocalDateTime start, LocalDateTime end) {
        if (roomIndex < 0 || roomIndex >= roomOccupationCounts.length
                || start == null || end == null || !start.isBefore(end)) {
            return false;
        }

        int fromSlot = toSlotIndex(start);
        int toSlotExclusive = toSlotIndex(end);

        Map<Integer, Integer> roomCounts = roomOccupationCounts[roomIndex];
        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            if (roomCounts.getOrDefault(slot, 0) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Permanently commits a room occupation once a partition has finished resolving.
     * This is called ONCE per finalized assignment, after a partition completes --
     * never speculatively during search within a partition.
     */
    public void commitOccupation(int roomIndex, LocalDateTime start, LocalDateTime end) {
        if (roomIndex < 0 || roomIndex >= roomOccupationCounts.length
                || start == null || end == null || !start.isBefore(end)) {
            return;
        }

        int fromSlot = toSlotIndex(start);
        int toSlotExclusive = toSlotIndex(end);

        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            roomOccupationCounts[roomIndex].merge(slot, 1, Integer::sum);
        }
    }

    /**
     * Point lookup of how many already-committed classes occupy a given slot in a
     * given room. Used by PartialSolutionContext to check availability against the
     * global state WITHOUT copying the entire accumulated map for that room.
     */
    public int getOccupationCount(int roomIndex, int slot) {
        if (roomIndex < 0 || roomIndex >= roomOccupationCounts.length) {
            return 0;
        }
        return roomOccupationCounts[roomIndex].getOrDefault(slot, 0);
    }

    public int getNumberOfRooms() {
        return roomOccupationCounts.length;
    }

    public LocalDateTime getScheduleOriginDateTime() {
        return scheduleOriginDateTime;
    }
}