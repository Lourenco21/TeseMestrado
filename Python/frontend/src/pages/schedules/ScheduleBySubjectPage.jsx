import { useMemo, useState } from "react";
import ScheduleToolbar from "../../components/schedule/ScheduleToolbar.jsx";
import WeeklySchedule from "../../components/schedule/WeeklySchedule.jsx";
import ScheduleViewSwitcher from "../../components/schedule/ScheduleViewSwitcher";
import { subjectOptions, schedulesBySubject } from "../../data/mockScheduleData";

export default function ScheduleBySubjectPage() {
  const [selectedSubject, setSelectedSubject] = useState(
    subjectOptions[0]?.id || ""
  );

  const events = useMemo(() => {
    if (!selectedSubject) return [];
    return schedulesBySubject[selectedSubject] || [];
  }, [selectedSubject]);

  return (
      <div className="schedule-page">
          <ScheduleViewSwitcher />
          <ScheduleToolbar
              title="Horário por cadeira"
              label="Selecionar cadeira"
              options={subjectOptions}
              value={selectedSubject}
              onChange={setSelectedSubject}
          />

          <WeeklySchedule events={events}/>
      </div>
  );
}