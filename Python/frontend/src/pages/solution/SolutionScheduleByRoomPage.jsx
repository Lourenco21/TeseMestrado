import { useEffect, useMemo, useState } from "react";
import { NavLink, useNavigate, useParams } from "react-router-dom";
import Papa from "papaparse";
import { format, parse, startOfWeek } from "date-fns";
import { getProblemDraft } from "../../services/problemsApi";
import { getProblemSolutionDetail } from "../../services/solutionsApi";
import SearchableSelect from "./SearchableSelect.jsx";

const DAYS = [
  { id: 1, label: "Segunda-feira" },
  { id: 2, label: "Terça-feira" },
  { id: 3, label: "Quarta-feira" },
  { id: 4, label: "Quinta-feira" },
  { id: 5, label: "Sexta-feira" },
  { id: 6, label: "Sábado" },
  { id: 7, label: "Domingo" },
];

const SLOT_HEIGHT = 60;
const START_HOUR = 8;
const END_HOUR = 22;

export default function SolutionScheduleByRoomPage() {
  const { id, solutionId } = useParams();
  const navigate = useNavigate();

  const [problem, setProblem] = useState(null);
  const [solution, setSolution] = useState(null);
  const [rows, setRows] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [selectedRoom, setSelectedRoom] = useState("");
  const [selectedWeek, setSelectedWeek] = useState("");
  const [hoveredEventId, setHoveredEventId] = useState(null);

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        setError("");

        const [problemData, solutionData] = await Promise.all([
          getProblemDraft(id),
          getProblemSolutionDetail(id, solutionId),
        ]);

        setProblem(problemData);
        setSolution(solutionData);

        if (!solutionData?.schedule_file_url) {
          throw new Error("A solução não tem ficheiro de horário disponível.");
        }

        const csvResponse = await fetch(solutionData.schedule_file_url);
        if (!csvResponse.ok) {
          throw new Error("Não foi possível carregar o CSV da solução.");
        }

        const csvText = await csvResponse.text();

        const parsed = Papa.parse(csvText, {
          header: true,
          skipEmptyLines: true,
        });

        if (parsed.errors?.length) {
          console.error(parsed.errors);
        }

        setRows(Array.isArray(parsed.data) ? parsed.data : []);
      } catch (err) {
        console.error(err);
        setError(err.message || "Erro ao carregar horário da solução.");
      } finally {
        setLoading(false);
      }
    }

    if (id && solutionId) {
      loadData();
    }
  }, [id, solutionId]);

  const mapping = problem?.mapping_data?.mapping || {};

  const detectedFields = useMemo(() => {
    if (!rows.length) {
      return null;
    }

    const headers = Object.keys(rows[0] || {});
    return detectScheduleFields(headers, mapping);
  }, [rows, mapping]);

  const isWeekCalculatedAutomatically = useMemo(() => {
    return !detectedFields?.week;
  }, [detectedFields]);

  const normalizedEvents = useMemo(() => {
    if (!rows.length || !detectedFields) {
      return [];
    }

    const baseEvents = rows
      .map((row, index) => normalizeScheduleRow(row, detectedFields, index))
      .filter((event) => event && event.weekKey);

    return annotateOverlaps(baseEvents);
  }, [rows, detectedFields]);

  const overlapSummary = useMemo(() => {
    return buildOverlapSummary(normalizedEvents);
  }, [normalizedEvents]);

  const roomOptions = useMemo(() => {
    const uniqueRooms = [...new Set(normalizedEvents.map((event) => event.room).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, "pt"));

    return uniqueRooms.map((room) => {
      const totalOverlaps = overlapSummary.roomTotals.get(room) || 0;

      return {
        id: room,
        searchText: room,
        label:
          totalOverlaps > 0
            ? `${room} (${totalOverlaps} sobreposição${totalOverlaps === 1 ? "" : "ões"})`
            : room,
      };
    });
  }, [normalizedEvents, overlapSummary]);

  const weekOptions = useMemo(() => {
    const uniqueWeeks = [...new Set(normalizedEvents.map((event) => event.weekKey).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, "pt"));

    return uniqueWeeks.map((weekKey) => {
      const roomWeekKey = `${selectedRoom}__${weekKey}`;
      const overlapsForRoomWeek = selectedRoom
        ? overlapSummary.roomWeekTotals.get(roomWeekKey) || 0
        : 0;

      const baseLabel = formatWeekLabel(weekKey);

      return {
        id: weekKey,
        label:
          selectedRoom && overlapsForRoomWeek > 0
            ? `${baseLabel} (${overlapsForRoomWeek} sobreposição${overlapsForRoomWeek === 1 ? "" : "ões"})`
            : baseLabel,
      };
    });
  }, [normalizedEvents, overlapSummary, selectedRoom]);

  useEffect(() => {
    if (!selectedRoom && roomOptions.length > 0) {
      setSelectedRoom(roomOptions[0].id);
    }
  }, [roomOptions, selectedRoom]);

  useEffect(() => {
    if (selectedRoom && !roomOptions.some((room) => room.id === selectedRoom)) {
      setSelectedRoom(roomOptions[0]?.id || "");
    }
  }, [roomOptions, selectedRoom]);

  useEffect(() => {
    if (!selectedWeek && weekOptions.length > 0) {
      setSelectedWeek(weekOptions[0].id);
    }
  }, [weekOptions, selectedWeek]);

  useEffect(() => {
    if (selectedWeek && !weekOptions.some((week) => week.id === selectedWeek)) {
      setSelectedWeek(weekOptions[0]?.id || "");
    }
  }, [weekOptions, selectedWeek]);

  const filteredEvents = useMemo(() => {
    return normalizedEvents.filter((event) => {
      const matchesRoom = selectedRoom ? event.room === selectedRoom : true;
      const matchesWeek = selectedWeek ? event.weekKey === selectedWeek : true;
      return matchesRoom && matchesWeek;
    });
  }, [normalizedEvents, selectedRoom, selectedWeek]);

  const hoveredEvent = useMemo(() => {
    return filteredEvents.find((event) => event.id === hoveredEventId) || null;
  }, [filteredEvents, hoveredEventId]);

  const overlapCount = useMemo(() => {
    return filteredEvents.filter((event) => event.hasOverlap).length;
  }, [filteredEvents]);

  const selectedWeekIndex = weekOptions.findIndex((item) => item.id === selectedWeek);

  function handlePreviousWeek() {
    if (selectedWeekIndex > 0) {
      setSelectedWeek(weekOptions[selectedWeekIndex - 1].id);
    }
  }

  function handleNextWeek() {
    if (selectedWeekIndex >= 0 && selectedWeekIndex < weekOptions.length - 1) {
      setSelectedWeek(weekOptions[selectedWeekIndex + 1].id);
    }
  }

  if (loading) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A carregar horário da solução...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <div style={styles.error}>{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Solução</p>
        <h1 style={styles.title}>Horário por sala</h1>
        <p style={styles.description}>
          Visualização semanal do horário da solução, com deteção de sobreposições por sala.
        </p>

        <div style={styles.switcher}>
          <NavLink
            to={`/problems/${id}/solutions/${solutionId}/metrics`}
            style={styles.switcherButton}
          >
            Dados
          </NavLink>
          <NavLink
            to={`/problems/${id}/solutions/${solutionId}/schedule/full`}
            style={styles.switcherButton}
          >
            Horário Completo
          </NavLink>

          <NavLink
            to={`/problems/${id}/solutions/${solutionId}/schedule/rooms`}
            style={{ ...styles.switcherButton, ...styles.switcherButtonActive }}
          >
            Horário por sala
          </NavLink>
        </div>

        <div style={styles.toolbarCard}>
          <div>
            <h2 style={styles.toolbarTitle}>
              {selectedRoom ? `Sala: ${selectedRoom}` : "Selecionar sala"}
            </h2>

            <p style={styles.toolbarHint}>
              Solução #{solution?.id} · {filteredEvents.length} aula(s) visíveis
              {overlapCount > 0 ? ` · ${overlapCount} com sobreposição` : ""}
            </p>

            <div style={styles.badgesRow}>
              {isWeekCalculatedAutomatically ? (
                <div style={styles.reviewedBadge}>
                  <span style={styles.reviewedBadgeIcon}>i</span>
                  Semana calculada automaticamente
                </div>
              ) : null}

              {overlapCount > 0 ? (
                <div style={styles.conflictBadge}>
                  <span style={styles.conflictBadgeIcon}>!</span>
                  Sobreposições detetadas
                </div>
              ) : null}
            </div>
          </div>

          <div style={styles.toolbarControls}>
            <SearchableSelect
                inputId="room-select"
                label="Sala"
                value={selectedRoom}
                onChange={setSelectedRoom}
                options={roomOptions}
                placeholder="Pesquisar sala..."
                noResultsText="Nenhuma sala encontrada"
              />
            <div style={styles.weekNavigator}>
              <button
                type="button"
                onClick={handlePreviousWeek}
                disabled={selectedWeekIndex <= 0}
                style={{
                  ...styles.arrowButton,
                  ...(selectedWeekIndex <= 0 ? styles.arrowButtonDisabled : {}),
                }}
              >
                ←
              </button>

              <select
                value={selectedWeek}
                onChange={(e) => setSelectedWeek(e.target.value)}
                style={styles.weekSelect}
              >
                <option value="">Selecionar semana...</option>
                {weekOptions.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={handleNextWeek}
                disabled={
                  selectedWeekIndex === -1 || selectedWeekIndex >= weekOptions.length - 1
                }
                style={{
                  ...styles.arrowButton,
                  ...((selectedWeekIndex === -1 ||
                    selectedWeekIndex >= weekOptions.length - 1)
                    ? styles.arrowButtonDisabled
                    : {}),
                }}
              >
                →
              </button>
            </div>
          </div>
        </div>

        <div style={styles.legendRow}>
          <div style={styles.legendItem}>
            <span style={styles.legendSwatchNormal} />
            Aula sem conflito
          </div>

          <div style={styles.legendItem}>
            <span style={styles.legendSwatchOverlap} />
            Aula com sobreposição
          </div>
        </div>

        <div style={styles.scheduleWrapperOuter}>
          <div style={styles.scheduleWrapper}>
            <div style={styles.scheduleHeader}>
              <div style={styles.timeColumnHeader} />
              {DAYS.map((day) => (
                <div key={day.id} style={styles.dayColumnHeader}>
                  {day.label}
                </div>
              ))}
            </div>

            <div style={styles.scheduleBody}>
              <div style={styles.timeColumn}>
                {generateTimeSlots(START_HOUR, END_HOUR).map((time) => (
                  <div key={time} style={styles.timeSlotLabel}>
                    {time}
                  </div>
                ))}
              </div>

              <div style={styles.daysGrid}>
                {DAYS.map((day) => (
                  <div key={day.id} style={styles.dayColumn}>
                    {generateTimeSlots(START_HOUR, END_HOUR).map((time) => (
                      <div key={`${day.id}-${time}`} style={styles.gridCell} />
                    ))}

                    <div style={styles.eventsLayer}>
                      {filteredEvents
                        .filter((event) => event.day === day.id)
                        .map((event) => {
                          const { top, height } = getEventPosition(
                            event.start,
                            event.end,
                            START_HOUR,
                            SLOT_HEIGHT
                          );

                          const isHovered = hoveredEventId === event.id;

                          return (
                            <div
                              key={event.id}
                              onMouseEnter={() => {
                                if (event.hasOverlap) {
                                  setHoveredEventId(event.id);
                                }
                              }}
                              onMouseLeave={() => {
                                if (hoveredEventId === event.id) {
                                  setHoveredEventId(null);
                                }
                              }}
                              style={{
                                ...styles.eventCard,
                                ...(event.hasOverlap ? styles.eventCardOverlap : {}),
                                ...(isHovered ? styles.eventCardHovered : {}),
                                top: `${top}px`,
                                height: `${height}px`,
                                zIndex: isHovered ? 20 : event.hasOverlap ? 10 : 1,
                              }}
                            >
                              <div style={styles.eventTime}>
                                {event.start} - {event.end}
                              </div>

                              <div style={styles.eventTitle}>
                                {event.title || "Aula"}
                              </div>

                              <div style={styles.eventSubtitle}>
                                {event.subtitle || "-"}
                              </div>

                              {event.hasOverlap ? (
                                <div style={styles.overlapBadge}>Sobreposição</div>
                              ) : null}

                              {event.hasOverlap && isHovered && event.overlappingWith?.length > 0 ? (
                                <div style={styles.tooltip}>
                                  <div style={styles.tooltipTitle}>
                                    Aulas sobrepostas
                                  </div>

                                  <div style={styles.tooltipCurrent}>
                                    <div style={styles.tooltipLabel}>Aula atual</div>
                                    <div style={styles.tooltipLine}>
                                      {event.start} - {event.end}
                                    </div>
                                    <div style={styles.tooltipLineStrong}>
                                      {event.title || "Aula"}
                                    </div>
                                    <div style={styles.tooltipLine}>
                                      {event.subtitle || "-"}
                                    </div>
                                  </div>

                                  <div style={styles.tooltipDivider} />

                                  <div style={styles.tooltipList}>
                                    {event.overlappingWith.map((conflict) => (
                                      <div key={conflict.id} style={styles.tooltipItem}>
                                        <div style={styles.tooltipLine}>
                                          {conflict.start} - {conflict.end}
                                        </div>
                                        <div style={styles.tooltipLineStrong}>
                                          {conflict.title || "Aula"}
                                        </div>
                                        <div style={styles.tooltipLine}>
                                          {conflict.subtitle || "-"}
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              ) : null}
                            </div>
                          );
                        })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {hoveredEvent && hoveredEvent.hasOverlap ? (
          <div style={styles.hoverInfo}>
            A visualizar detalhes de sobreposição para {hoveredEvent.title || "aula"} ({hoveredEvent.start} - {hoveredEvent.end}).
          </div>
        ) : null}

        <div style={styles.bottomActions}>
          <button
            type="button"
            onClick={() => navigate(`/problems/${id}/solutions/${solutionId}`)}
            style={styles.secondaryButton}
          >
            Voltar à solução
          </button>
        </div>
      </div>
    </div>
  );
}

function detectScheduleFields(headers, mapping) {
  const normalizedHeaders = headers.map((header) => ({
    original: header,
    normalized: normalizeText(header),
  }));

  function findMappedField(mappingValue, fallbackCandidates = []) {
    const candidates = [mappingValue, ...fallbackCandidates]
      .filter(Boolean)
      .map(normalizeText);

    const exactMatch = normalizedHeaders.find((header) =>
      candidates.includes(header.normalized)
    );

    if (exactMatch) {
      return exactMatch.original;
    }

    const containsMatch = normalizedHeaders.find((header) =>
      candidates.some((candidate) => header.normalized.includes(candidate))
    );

    return containsMatch?.original || "";
  }

  function findDateField() {
    const preferred = [mapping?.dia, "data", "date", "dia"]
      .filter(Boolean)
      .map(normalizeText);

    const forbidden = [
      mapping?.dia_da_semana,
      "dia da semana",
      "weekday",
      "day of week",
    ]
      .filter(Boolean)
      .map(normalizeText);

    const exact = normalizedHeaders.find(
      (header) =>
        preferred.includes(header.normalized) &&
        !forbidden.includes(header.normalized)
    );

    if (exact) {
      return exact.original;
    }

    const relaxed = normalizedHeaders.find((header) => {
      const normalized = header.normalized;
      const matchesPreferred = preferred.some(
        (candidate) => normalized === candidate || normalized.includes(candidate)
      );
      const looksLikeWeekday = forbidden.some(
        (candidate) => normalized === candidate || normalized.includes(candidate)
      );

      return matchesPreferred && !looksLikeWeekday;
    });

    return relaxed?.original || "";
  }

  return {
    room: findMappedField(mapping?.sala, ["sala", "room", "sala da aula"]),
    dayOfWeek: findMappedField(mapping?.dia_da_semana, ["dia da semana", "weekday"]),
    date: findDateField(),
    startTime: findMappedField(mapping?.hora_inicio, [
      "início",
      "inicio",
      "start",
      "hora inicio",
    ]),
    endTime: findMappedField(mapping?.hora_fim, ["fim", "end", "hora fim"]),
    className: findMappedField(mapping?.turma, ["turma", "class"]),
    courseName: findMappedField(mapping?.unidade_curricular, [
      "unidade curricular",
      "uc",
    ]),
    week: findMappedField(mapping?.semana, ["semana", "week"]),
  };
}

function normalizeScheduleRow(row, fields, index) {
  const room = getCellValue(row, fields.room);
  const start = normalizeTimeValue(getCellValue(row, fields.startTime));
  const end = normalizeTimeValue(getCellValue(row, fields.endTime));
  const dateRaw = getCellValue(row, fields.date);
  const weekRaw = getCellValue(row, fields.week);
  const dayOfWeekRaw = getCellValue(row, fields.dayOfWeek);
  const className = getCellValue(row, fields.className);
  const courseName = getCellValue(row, fields.courseName);

  if (!room || !start || !end) {
    return null;
  }

  const parsedDate = parseDateValue(dateRaw);
  const weekKey = normalizeWeekValue(weekRaw, parsedDate);

  const day = parsedDate
    ? jsDayToScheduleDay(parsedDate.getDay())
    : parseDayOfWeek(dayOfWeekRaw);

  if (!day || !weekKey) {
    return null;
  }

  return {
    id: `${room}-${weekKey}-${day}-${start}-${end}-${index}`,
    room,
    day,
    start,
    end,
    weekKey,
    title: courseName || "Aula",
    subtitle: className || "",
    date: dateRaw || "",
  };
}

function annotateOverlaps(events) {
  const groups = new Map();

  for (const event of events) {
    const key = `${event.weekKey}__${event.room}__${event.day}`;
    if (!groups.has(key)) {
      groups.set(key, []);
    }
    groups.get(key).push(event);
  }

  const overlapMap = new Map();

  for (const groupEvents of groups.values()) {
    const sorted = [...groupEvents].sort((a, b) => {
      const startDiff = convertTimeToMinutes(a.start) - convertTimeToMinutes(b.start);
      if (startDiff !== 0) {
        return startDiff;
      }
      return convertTimeToMinutes(a.end) - convertTimeToMinutes(b.end);
    });

    for (let i = 0; i < sorted.length; i += 1) {
      const current = sorted[i];
      const currentStart = convertTimeToMinutes(current.start);
      const currentEnd = convertTimeToMinutes(current.end);

      for (let j = i + 1; j < sorted.length; j += 1) {
        const next = sorted[j];
        const nextStart = convertTimeToMinutes(next.start);
        const nextEnd = convertTimeToMinutes(next.end);

        if (nextStart >= currentEnd) {
          break;
        }

        if (currentStart < nextEnd && nextStart < currentEnd) {
          //if (!overlapMap.has(current.id)) {
            //overlapMap.set(current.id, []);
          //}
          if (!overlapMap.has(next.id)) {
            overlapMap.set(next.id, []);
          }

          /*overlapMap.get(current.id).push({
            id: next.id,
            start: next.start,
            end: next.end,
            title: next.title,
            subtitle: next.subtitle,
          });*/

          overlapMap.get(next.id).push({
            id: current.id,
            start: current.start,
            end: current.end,
            title: current.title,
            subtitle: current.subtitle,
          });
        }
      }
    }
  }

  return events.map((event) => ({
    ...event,
    hasOverlap: overlapMap.has(event.id),
    overlappingWith: overlapMap.get(event.id) || [],
  }));
}

function buildOverlapSummary(events) {
  const roomTotals = new Map();
  const roomWeekTotals = new Map();

  for (const event of events) {
    if (!event.hasOverlap) {
      continue;
    }

    roomTotals.set(event.room, (roomTotals.get(event.room) || 0) + 1);

    const roomWeekKey = `${event.room}__${event.weekKey}`;
    roomWeekTotals.set(roomWeekKey, (roomWeekTotals.get(roomWeekKey) || 0) + 1);
  }

  return {
    roomTotals,
    roomWeekTotals,
  };
}

function getCellValue(row, key) {
  if (!row || !key) {
    return "";
  }

  return String(row[key] ?? "").trim();
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();
}

function normalizeTimeValue(value) {
  if (!value) {
    return "";
  }

  return String(value).slice(0, 5);
}

function parseDateValue(value) {
  if (!value) {
    return null;
  }

  const trimmed = String(value).trim();
  const formats = ["dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"];

  for (const formatString of formats) {
    const parsed = parse(trimmed, formatString, new Date());
    if (!Number.isNaN(parsed.getTime())) {
      return parsed;
    }
  }

  return null;
}

function parseDayOfWeek(value) {
  const normalized = normalizeText(value);

  const map = {
    seg: 1,
    segunda: 1,
    "segunda-feira": 1,
    ter: 2,
    terca: 2,
    "terca-feira": 2,
    terça: 2,
    "terça-feira": 2,
    qua: 3,
    quarta: 3,
    "quarta-feira": 3,
    qui: 4,
    quinta: 4,
    "quinta-feira": 4,
    sex: 5,
    sexta: 5,
    "sexta-feira": 5,
    sab: 6,
    sabado: 6,
    sábado: 6,
    dom: 7,
    domingo: 7,
  };

  return map[normalized] || null;
}

function jsDayToScheduleDay(jsDay) {
  return jsDay === 0 ? 7 : jsDay;
}

function normalizeWeekValue(weekRaw, parsedDate) {
  const trimmed = String(weekRaw || "").trim();

  if (trimmed) {
    const parsedWeekDate = parseDateValue(trimmed);

    if (parsedWeekDate) {
      return format(startOfWeek(parsedWeekDate, { weekStartsOn: 1 }), "yyyy-MM-dd");
    }

    const numericWeek = Number(trimmed);
    if (!Number.isNaN(numericWeek) && numericWeek > 0) {
      return `week-${numericWeek}`;
    }
  }

  if (parsedDate) {
    return format(startOfWeek(parsedDate, { weekStartsOn: 1 }), "yyyy-MM-dd");
  }

  return "";
}

function formatWeekLabel(weekKey) {
  if (!weekKey) {
    return "Semana";
  }

  if (weekKey.startsWith("week-")) {
    return `Semana ${weekKey.replace("week-", "")}`;
  }

  const parsedDate = parseDateValue(weekKey);

  if (!parsedDate) {
    return weekKey;
  }

  const end = new Date(parsedDate);
  end.setDate(parsedDate.getDate() + 6);

  return `${format(parsedDate, "dd/MM/yyyy")} - ${format(end, "dd/MM/yyyy")}`;
}

function generateTimeSlots(startHour, endHour) {
  const slots = [];

  for (let hour = startHour; hour <= endHour; hour += 1) {
    slots.push(`${String(hour).padStart(2, "0")}:00`);
  }

  return slots;
}

function getEventPosition(start, end, startHour, slotHeight) {
  const startMinutes = convertTimeToMinutes(start);
  const endMinutes = convertTimeToMinutes(end);
  const baseMinutes = startHour * 60;

  const top = ((startMinutes - baseMinutes) / 60) * slotHeight;
  const height = ((endMinutes - startMinutes) / 60) * slotHeight;

  return {
    top: Math.max(top, 0),
    height: Math.max(height, 24),
  };
}

function convertTimeToMinutes(time) {
  const [hours, minutes] = String(time).split(":").map(Number);
  return hours * 60 + minutes;
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "#f3f4f6",
    padding: "24px",
  },
  container: {
    maxWidth: "1400px",
    margin: "0 auto",
  },
  step: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#475467",
  },
  title: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "36px",
    lineHeight: 1.1,
    color: "#0f172a",
  },
  description: {
    margin: 0,
    marginBottom: "20px",
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
  },
  message: {
    fontSize: "16px",
    color: "#475467",
  },
  error: {
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#fef2f2",
    border: "1px solid #fecaca",
    color: "#b42318",
    fontSize: "14px",
  },
  switcher: {
    display: "flex",
    gap: "8px",
    alignItems: "center",
    marginBottom: "16px",
    flexWrap: "wrap",
  },
  switcherButton: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    minHeight: "42px",
    padding: "0 14px",
    border: "1px solid #d1d5db",
    borderRadius: "10px",
    background: "#ffffff",
    color: "#111827",
    fontSize: "14px",
    fontWeight: 600,
    textDecoration: "none",
  },
  switcherButtonActive: {
    background: "#111827",
    color: "#ffffff",
    borderColor: "#111827",
  },
  toolbarCard: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-end",
    gap: "16px",
    marginBottom: "16px",
    padding: "18px",
    background: "#ffffff",
    border: "1px solid #e5e7eb",
    borderRadius: "14px",
    flexWrap: "wrap",
  },
  toolbarTitle: {
    margin: 0,
    fontSize: "24px",
    color: "#0f172a",
  },
  toolbarHint: {
    margin: "6px 0 10px 0",
    fontSize: "14px",
    color: "#6b7280",
  },
  badgesRow: {
    display: "flex",
    gap: "8px",
    flexWrap: "wrap",
  },
  reviewedBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    minHeight: "32px",
    padding: "6px 10px",
    borderRadius: "999px",
    fontSize: "13px",
    fontWeight: 600,
    whiteSpace: "nowrap",
    background: "#eff6ff",
    color: "#1d4ed8",
    border: "1px solid #bfdbfe",
  },
  reviewedBadgeIcon: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: "18px",
    height: "18px",
    borderRadius: "999px",
    background: "#1d4ed8",
    color: "#ffffff",
    fontSize: "11px",
    lineHeight: 1,
  },
  conflictBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    minHeight: "32px",
    padding: "6px 10px",
    borderRadius: "999px",
    fontSize: "13px",
    fontWeight: 600,
    whiteSpace: "nowrap",
    background: "#fef2f2",
    color: "#b42318",
    border: "1px solid #fecaca",
  },
  conflictBadgeIcon: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: "18px",
    height: "18px",
    borderRadius: "999px",
    background: "#b42318",
    color: "#ffffff",
    fontSize: "11px",
    lineHeight: 1,
  },
  toolbarControls: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
    alignItems: "flex-end",
  },
  filterBlock: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    minWidth: "260px",
  },
  label: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#475467",
  },
  select: {
    height: "42px",
    padding: "0 12px",
    border: "1px solid #cbd5e1",
    borderRadius: "8px",
    background: "#ffffff",
    color: "#111827",
    fontSize: "14px",
  },
  weekNavigator: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
  },
  arrowButton: {
    width: "42px",
    height: "42px",
    borderRadius: "10px",
    border: "1px solid #d1d5db",
    background: "#ffffff",
    color: "#111827",
    fontSize: "18px",
    fontWeight: 700,
    cursor: "pointer",
  },
  arrowButtonDisabled: {
    opacity: 0.45,
    cursor: "not-allowed",
  },
  weekSelect: {
    minWidth: "300px",
    height: "42px",
    padding: "0 12px",
    border: "1px solid #cbd5e1",
    borderRadius: "8px",
    background: "#ffffff",
    color: "#111827",
    fontSize: "14px",
  },
  legendRow: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
    alignItems: "center",
    marginBottom: "12px",
    padding: "0 4px",
  },
  legendItem: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    fontSize: "13px",
    color: "#475467",
    fontWeight: 600,
  },
  legendSwatchNormal: {
    width: "18px",
    height: "12px",
    borderRadius: "999px",
    background: "#cfeaf6",
    border: "1px solid #b7d9e8",
  },
  legendSwatchOverlap: {
    width: "18px",
    height: "12px",
    borderRadius: "999px",
    background: "#fee2e2",
    border: "1px solid #fca5a5",
  },
  scheduleWrapperOuter: {
    overflowX: "auto",
  },
  scheduleWrapper: {
    background: "#ffffff",
    border: "1px solid #d9dde3",
    borderRadius: "14px",
    overflow: "hidden",
    minWidth: "980px",
  },
  scheduleHeader: {
    display: "grid",
    gridTemplateColumns: "72px repeat(7, 1fr)",
    background: "#f7f7f8",
    borderBottom: "1px solid #d9dde3",
  },
  timeColumnHeader: {
    height: "52px",
    borderRight: "1px solid #d9dde3",
  },
  dayColumnHeader: {
    height: "52px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "0 8px",
    textAlign: "center",
    fontSize: "14px",
    fontWeight: 500,
    color: "#374151",
    borderRight: "1px solid #e7eaee",
  },
  scheduleBody: {
    display: "grid",
    gridTemplateColumns: "72px 1fr",
  },
  timeColumn: {
    background: "#fafafa",
    borderRight: "1px solid #d9dde3",
  },
  timeSlotLabel: {
    height: `${SLOT_HEIGHT}px`,
    boxSizing: "border-box",
    padding: "6px 8px 0 0",
    textAlign: "right",
    fontSize: "12px",
    color: "#6b7280",
    borderBottom: "1px dashed #e7eaee",
  },
  daysGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(7, 1fr)",
  },
  dayColumn: {
    position: "relative",
    height: `${(END_HOUR - START_HOUR + 1) * SLOT_HEIGHT}px`,
    borderRight: "1px solid #e7eaee",
    background: "#fff",
  },
  gridCell: {
    height: `${SLOT_HEIGHT}px`,
    boxSizing: "border-box",
    borderBottom: "1px dashed #e7eaee",
  },
  eventsLayer: {
    position: "absolute",
    inset: 0,
  },
  eventCard: {
    position: "absolute",
    left: "4px",
    right: "4px",
    background: "#cfeaf6",
    border: "1px solid #b7d9e8",
    padding: "8px 8px 6px",
    overflow: "visible",
    color: "#4e8eb1",
    fontSize: "12px",
    lineHeight: 1.2,
    boxSizing: "border-box",
    borderRadius: "8px",
  },
  eventCardOverlap: {
    background: "#fee2e2",
    border: "1px solid #fca5a5",
    color: "#991b1b",
    boxShadow: "inset 0 0 0 1px rgba(239,68,68,0.08)",
  },
  eventCardHovered: {
    boxShadow: "0 8px 18px rgba(15, 23, 42, 0.16)",
  },
  eventTime: {
    fontWeight: 500,
    marginBottom: "4px",
  },
  eventTitle: {
    fontWeight: 700,
    marginBottom: "4px",
  },
  eventSubtitle: {
    fontWeight: 400,
  },
  overlapBadge: {
    marginTop: "6px",
    display: "inline-flex",
    alignItems: "center",
    padding: "2px 6px",
    borderRadius: "999px",
    background: "#ffffff",
    border: "1px solid #fecaca",
    color: "#b42318",
    fontSize: "11px",
    fontWeight: 700,
  },
  tooltip: {
    position: "absolute",
    left: "calc(100% + 8px)",
    top: "0",
    width: "260px",
    padding: "12px",
    borderRadius: "12px",
    background: "#111827",
    color: "#f9fafb",
    border: "1px solid rgba(255,255,255,0.08)",
    boxShadow: "0 16px 40px rgba(0,0,0,0.24)",
    zIndex: 50,
  },
  tooltipTitle: {
    fontSize: "12px",
    fontWeight: 700,
    marginBottom: "10px",
    color: "#ffffff",
  },
  tooltipCurrent: {
    padding: "8px",
    borderRadius: "8px",
    background: "rgba(255,255,255,0.06)",
  },
  tooltipLabel: {
    fontSize: "10px",
    fontWeight: 700,
    textTransform: "uppercase",
    letterSpacing: "0.04em",
    color: "#d1d5db",
    marginBottom: "6px",
  },
  tooltipDivider: {
    height: "1px",
    background: "rgba(255,255,255,0.12)",
    margin: "10px 0",
  },
  tooltipList: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  tooltipItem: {
    padding: "8px",
    borderRadius: "8px",
    background: "rgba(255,255,255,0.04)",
  },
  tooltipLine: {
    fontSize: "12px",
    color: "#d1d5db",
    marginBottom: "2px",
  },
  tooltipLineStrong: {
    fontSize: "12px",
    color: "#ffffff",
    fontWeight: 700,
    marginBottom: "2px",
  },
  hoverInfo: {
    marginTop: "12px",
    padding: "10px 12px",
    borderRadius: "10px",
    background: "#fff7ed",
    border: "1px solid #fed7aa",
    color: "#9a3412",
    fontSize: "13px",
    fontWeight: 600,
  },
  bottomActions: {
    marginTop: "18px",
    display: "flex",
    justifyContent: "flex-start",
  },
  secondaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#101828",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
  },
};

