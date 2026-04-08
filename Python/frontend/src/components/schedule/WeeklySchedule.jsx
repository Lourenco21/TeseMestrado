import ScheduleEventCard from "./ScheduleEventCard";
import { generateTimeSlots, getEventPosition } from "../../utils/scheduleUtils";
import "./weeklySchedule.css";

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

export default function WeeklySchedule({
  events = [],
  startHour = 9,
  endHour = 21,
}) {
  const timeSlots = generateTimeSlots(startHour, endHour);

  return (
    <div className="weekly-schedule-wrapper">
      <div className="weekly-schedule-header">
        <div className="time-column-header" />
        {DAYS.map((day) => (
          <div key={day.id} className="day-column-header">
            {day.label}
          </div>
        ))}
      </div>

      <div className="weekly-schedule-body">
        <div className="time-column">
          {timeSlots.map((time) => (
            <div key={time} className="time-slot-label">
              {time}
            </div>
          ))}
        </div>

        <div className="days-grid">
          {DAYS.map((day) => (
            <div key={day.id} className="day-column">
              {timeSlots.map((time) => (
                <div key={`${day.id}-${time}`} className="grid-cell" />
              ))}

              <div className="events-layer">
                {events
                  .filter((event) => event.day === day.id)
                  .map((event) => {
                    const { top, height } = getEventPosition(
                      event.start,
                      event.end,
                      startHour,
                      SLOT_HEIGHT
                    );

                    return (
                      <ScheduleEventCard
                        key={event.id}
                        event={event}
                        style={{
                          top: `${top}px`,
                          height: `${height}px`,
                        }}
                      />
                    );
                  })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}