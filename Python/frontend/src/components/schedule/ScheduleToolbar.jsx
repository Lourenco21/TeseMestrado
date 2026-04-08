export default function ScheduleToolbar({
  title,
  label,
  options,
  value,
  onChange,
}) {
  return (
    <div className="schedule-toolbar">
      <div>
        <h1 className="schedule-title">{title}</h1>
      </div>

      <div className="schedule-filter">
        <label htmlFor="schedule-select">{label}</label>
        <select
          id="schedule-select"
          value={value}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">Selecionar...</option>
          {options.map((option) => (
            <option key={option.id} value={option.id}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}