package pt.lourenco.optimization.jmetal.partitioning;

import org.springframework.stereotype.Service;
import pt.lourenco.optimization.jmetal.problems.model.ProblemInputData;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulePartitionService {

    @SuppressWarnings("unchecked")
    public List<PartitionedProblemInputData> buildPartitions(
            ProblemInputData originalInput,
            PartitionType partitionType
    ) {

        if (originalInput == null) {
            throw new IllegalArgumentException("ProblemInputData is null.");
        }

        if (originalInput.getScheduleData() == null) {
            throw new IllegalArgumentException("ProblemInputData.scheduleData is null.");
        }

        if (originalInput.getRoomsData() == null) {
            throw new IllegalArgumentException("ProblemInputData.roomsData is null.");
        }

        Object classesObject = originalInput.getScheduleData().get("classes");
        Object roomsObject = originalInput.getRoomsData().get("rooms");

        System.out.println("DEBUG buildPartitions scheduleData keys = " + originalInput.getScheduleData().keySet());
        System.out.println("DEBUG buildPartitions roomsData keys = " + originalInput.getRoomsData().keySet());
        System.out.println("DEBUG buildPartitions classes size = " + (classesObject instanceof List<?> list ? list.size() : null));
        System.out.println("DEBUG buildPartitions rooms size = " + (roomsObject instanceof List<?> list ? list.size() : null));

        if (!(classesObject instanceof List<?> rawClasses)) {
            throw new IllegalArgumentException("schedule_data.classes is missing or is not a list.");
        }

        if (!(roomsObject instanceof List<?> rawRooms)) {
            throw new IllegalArgumentException("rooms_data.rooms is missing or is not a list.");
        }

        //Object classesObject = originalInput.getScheduleData().get("classes");
        //if (!(classesObject instanceof List<?> rawClasses)) {
        //    return List.of();
        //}

        List<Map<String, Object>> classes = rawClasses.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();

        if (classes.isEmpty()) {
            return List.of();
        }

        if (partitionType == PartitionType.SEMESTER) {
            ProblemInputData cloned = cloneWithClasses(originalInput, classes);
            return List.of(new PartitionedProblemInputData(
                    "semester",
                    PartitionType.SEMESTER,
                    0,
                    cloned,
                    classes
            ));
        }

        Map<String, List<Map<String, Object>>> grouped = classes.stream()
                .collect(Collectors.groupingBy(c -> buildPartitionKey(c, originalInput, partitionType)));

        List<Map.Entry<String, List<Map<String, Object>>>> orderedEntries = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        List<PartitionedProblemInputData> result = new ArrayList<>();
        for (int i = 0; i < orderedEntries.size(); i++) {
            Map.Entry<String, List<Map<String, Object>>> entry = orderedEntries.get(i);
            ProblemInputData cloned = cloneWithClasses(originalInput, entry.getValue());

            result.add(new PartitionedProblemInputData(
                    entry.getKey(),
                    partitionType,
                    i,
                    cloned,
                    entry.getValue()
            ));
        }

        return result;
    }

    public String buildPartitionKey(
            Map<String, Object> classData,
            ProblemInputData inputData,
            PartitionType partitionType
    ) {
        return switch (partitionType) {
            case SEMESTER -> "semester";
            case WEEK -> buildWeekPartitionKey(classData, inputData);
            case DAY -> buildDayPartitionKey(classData, inputData);
            case START_HALF_HOUR -> buildDayTimePartitionKey(classData, inputData);
        };
    }

    public String buildWeekPartitionKey(Map<String, Object> classData, ProblemInputData inputData) {
        Map<String, Object> mapping = inputData.getMappingData();

        String weekColumn = getMappedColumn(mapping, "semana");
        String dayColumn = getMappedColumn(mapping, "dia");

        Object weekValue = getValue(classData, weekColumn);
        if (weekValue != null) {
            String normalized = weekValue.toString().trim();
            if (!normalized.isBlank() && !looksLikeWeekdayValue(normalized)) {
                return normalized;
            }
        }

        LocalDate parsedDay = parseDateValue(getValue(classData, dayColumn));
        if (parsedDay == null) {
            return "unknown_week";
        }

        LocalDate semesterStart = resolveSemesterStart(inputData);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(semesterStart, parsedDay);
        long weekNumber = (daysBetween / 7) + 1;

        return "week_" + weekNumber;
    }

    public String buildDayPartitionKey(Map<String, Object> classData, ProblemInputData inputData) {
        Map<String, Object> mapping = inputData.getMappingData();

        String dayColumn = getMappedColumn(mapping, "dia");
        LocalDate parsedDay = parseDateValue(getValue(classData, dayColumn));

        return parsedDay == null ? "unknown_day" : parsedDay.toString();
    }

    public String buildDayTimePartitionKey(Map<String, Object> classData, ProblemInputData inputData) {
        Map<String, Object> mapping = inputData.getMappingData();

        String dayColumn = getMappedColumn(mapping, "dia");
        String startColumn = getMappedColumn(mapping, "hora_inicio");

        LocalDate parsedDay = parseDateValue(getValue(classData, dayColumn));
        LocalTime parsedStartTime = coerceToLocalTime(getValue(classData, startColumn));

        if (parsedDay == null || parsedStartTime == null) {
            return "unknown_start_time";
        }

        int minute = parsedStartTime.getMinute() >= 30 ? 30 : 0;
        return parsedDay + " " + String.format("%02d:%02d", parsedStartTime.getHour(), minute);
    }

    private LocalDate resolveSemesterStart(ProblemInputData inputData) {
        Object classesObject = inputData.getScheduleData().get("classes");
        if (!(classesObject instanceof List<?> rawClasses)) {
            return LocalDate.now();
        }

        Map<String, Object> mapping = inputData.getMappingData();
        String dayColumn = getMappedColumn(mapping, "dia");

        return rawClasses.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .map(row -> parseDateValue(getValue(row, dayColumn)))
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    @SuppressWarnings("unchecked")
    public String getMappedColumn(Map<String, Object> mappingWrapper, String logicalKey) {
        if (mappingWrapper == null) {
            return null;
        }

        Object directValue = mappingWrapper.get(logicalKey);
        if (directValue != null) {
            String normalized = directValue.toString().trim();
            return normalized.isBlank() ? null : normalized;
        }

        Object nestedMappingObject = mappingWrapper.get("mapping");
        if (nestedMappingObject instanceof Map<?, ?> nestedMapping) {
            Object nestedValue = nestedMapping.get(logicalKey);
            if (nestedValue != null) {
                String normalized = nestedValue.toString().trim();
                return normalized.isBlank() ? null : normalized;
            }
        }

        return null;
    }

    public Object getValue(Map<String, Object> row, String columnName) {
        if (row == null || columnName == null) {
            return null;
        }
        return row.get(columnName);
    }

    private boolean looksLikeWeekdayValue(String value) {
        Set<String> weekdayLikeValues = Set.of(
                "seg", "segunda", "segunda-feira",
                "ter", "terça", "terca", "terça-feira", "terca-feira",
                "qua", "quarta", "quarta-feira",
                "qui", "quinta", "quinta-feira",
                "sex", "sexta", "sexta-feira",
                "sáb", "sab", "sábado", "sabado",
                "dom", "domingo",
                "mon", "monday", "tue", "tuesday", "wed", "wednesday",
                "thu", "thursday", "fri", "friday", "sat", "saturday",
                "sun", "sunday"
        );
        return weekdayLikeValues.contains(value.trim().toLowerCase());
    }

    public LocalDate parseDateValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                String pattern = formatter.toString();
                if (pattern.contains("HourOfDay")) {
                    return LocalDateTime.parse(text, formatter).toLocalDate();
                }
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return OffsetDateTime.parse(text).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    public LocalTime coerceToLocalTime(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalTime localTime) {
            return localTime;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalTime();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        }
        if (value instanceof Number number) {
            double raw = number.doubleValue();
            int totalSeconds = (int) Math.round(raw * 24 * 60 * 60);
            int hours = (totalSeconds / 3600) % 24;
            int minutes = (totalSeconds % 3600) / 60;
            return LocalTime.of(hours, minutes);
        }

        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("HH.mm")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDateTime.parse(text).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(text).toLocalTime();
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private ProblemInputData cloneWithClasses(
            ProblemInputData original,
            List<Map<String, Object>> classesForPartition
    ) {
        Map<String, Object> newScheduleData = new LinkedHashMap<>(original.getScheduleData());
        newScheduleData.put("classes", classesForPartition);

        ProblemInputData copy = new ProblemInputData();
        copy.setProblemId(original.getProblemId());
        copy.setProblemName(original.getProblemName());
        copy.setProblemType(original.getProblemType());
        copy.setProblemSubtype(original.getProblemSubtype());
        copy.setSelectedAlgorithm(original.getSelectedAlgorithm());
        copy.setResolutionScope(original.getResolutionScope());
        copy.setRepeatedInstanceStrategy(original.getRepeatedInstanceStrategy());
        copy.setSelectedConstraints(original.getSelectedConstraints());
        copy.setScheduleData(newScheduleData);
        copy.setRoomsData(original.getRoomsData());
        copy.setMetadata(original.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(original.getMetadata()));
        copy.setMappingData(original.getMappingData());
        copy.setRoomsMappingData(original.getRoomsMappingData());
        copy.setConstraintsSummary(original.getConstraintsSummary());
        copy.setInstanceCharacteristics(original.getInstanceCharacteristics());
        return copy;
    }
}