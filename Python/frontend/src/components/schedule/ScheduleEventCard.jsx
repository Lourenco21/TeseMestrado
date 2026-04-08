export default function ScheduleEventCard({ event, style }) {
  return (
    <div className="schedule-event-card" style={style}>
      <div className="schedule-event-time">{event.start}</div>
      <div className="schedule-event-title">{event.title}</div>
      <div className="schedule-event-subtitle">{event.subtitle}</div>
    </div>
  );
}