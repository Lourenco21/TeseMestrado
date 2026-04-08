import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import ScheduleByClassPage from "./pages/schedules/ScheduleByClassPage.jsx";
import ScheduleBySubjectPage from "./pages/schedules/ScheduleBySubjectPage.jsx";
import ScheduleCompletePage from "./pages/schedules/ScheduleCompletePage.jsx";
import UploadSchedulePage from "./pages/schedules/UploadSchedulePage";
import ScheduleByRoomPage from "./pages/schedules/ScheduleByRoomPage.jsx";
import ReviewScheduleMappingPage from "./pages/schedules/ReviewScheduleMappingPage";
import SchedulesHomePage from "./pages/schedules/SchedulesHomePage";

export default function App() {
  return (
    <BrowserRouter>
      <div>
        <Routes>
          <Route path="/horario/turma" element={<ScheduleByClassPage />} />
          <Route path="/horario/cadeira" element={<ScheduleBySubjectPage />} />
          <Route path="/horario/completo" element={<ScheduleCompletePage />} />
          <Route path="/horario/upload" element={<UploadSchedulePage />} />
          <Route path="/horario/sala" element={<ScheduleByRoomPage />} />
          <Route path="/horario/upload/:scheduleId/review" element={<ReviewScheduleMappingPage />} />
          <Route path="/" element={<SchedulesHomePage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}