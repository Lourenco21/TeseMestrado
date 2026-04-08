import { NavLink } from "react-router-dom";

export default function ScheduleViewSwitcher() {
  const baseClass = ({ isActive }) =>
    `schedule-switcher-button ${isActive ? "active" : ""}`;

  return (
      <div className="schedule-switcher" role="tablist" aria-label="Alternar vista do horário">
          <NavLink to="/horario/completo" className={baseClass} role="tab">
              Horário Completo
          </NavLink>

          <NavLink to="/horario/sala" className={baseClass} role="tab">
              Horário por sala
          </NavLink>

          <NavLink to="/horario/turma" className={baseClass} role="tab">
              Horário por turma
          </NavLink>

          <NavLink to="/horario/cadeira" className={baseClass} role="tab">
              Horário por cadeira
          </NavLink>
      </div>
  );
}