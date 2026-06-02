package pt.lourenco.optimization.jmetal.problems.mapping;

import pt.lourenco.optimization.utils.NestedMapUtils;

import java.util.Map;

public final class RoomLinkingUtils {

    private RoomLinkingUtils() {
    }

    public static String getScheduleRoomValue(
            Map<String, Object> classRow,
            Map<String, Object> roomsMappingData
    ) {
        String scheduleRoomColumn = RoomsMappingUtils.getScheduleRoomColumn(roomsMappingData);

        if (!NestedMapUtils.hasText(scheduleRoomColumn) || classRow == null) {
            return null;
        }

        Object value = classRow.get(scheduleRoomColumn);
        return value == null ? null : String.valueOf(value);
    }

    public static String getRoomsFileRoomValue(
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        String roomsFileRoomColumn = RoomsMappingUtils.getRoomsFileRoomColumn(roomsMappingData);

        if (!NestedMapUtils.hasText(roomsFileRoomColumn) || roomRow == null) {
            return null;
        }

        Object value = roomRow.get(roomsFileRoomColumn);
        return value == null ? null : String.valueOf(value);
    }

    public static boolean roomsMatch(
            Map<String, Object> classRow,
            Map<String, Object> roomRow,
            Map<String, Object> roomsMappingData
    ) {
        String classRoom = getScheduleRoomValue(classRow, roomsMappingData);
        String roomName = getRoomsFileRoomValue(roomRow, roomsMappingData);

        if (!NestedMapUtils.hasText(classRoom) || !NestedMapUtils.hasText(roomName)) {
            return false;
        }

        return classRoom.trim().equalsIgnoreCase(roomName.trim());
    }
}
