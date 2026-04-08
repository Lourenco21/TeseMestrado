import { useMemo, useState } from "react";
import ScheduleToolbar from "../../components/schedule/ScheduleToolbar.jsx";
import WeeklySchedule from "../../components/schedule/WeeklySchedule.jsx";
import ScheduleViewSwitcher from "../../components/schedule/ScheduleViewSwitcher";
import { classOptions, schedulesByClass } from "../../data/mockScheduleData";

export default function ScheduleByClassPage() {
  const [selectedClass, setSelectedClass] = useState(classOptions[0]?.id || "");

  const events = useMemo(() => {
    if (!selectedClass) return [];
    return schedulesByClass[selectedClass] || [];
  }, [selectedClass]);

  return (
      <div className="schedule-page">
          <ScheduleViewSwitcher />
          <ScheduleToolbar
              title="Horário por turma"
              label="Selecionar turma"
              options={classOptions}
              value={selectedClass}
              onChange={setSelectedClass}
          />

          <WeeklySchedule events={events}/>
      </div>
  );
}