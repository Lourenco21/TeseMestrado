import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProblemWizardProvider } from "./contexts/ProblemWizardContext";
import ProblemHomePage from "./pages/problems/ProblemHomePage";
import ProblemWizardStartPage from "./pages/problems/ProblemWizardStartPage";
import ProblemTypeStepPage from "./pages/problems/ProblemTypeStepPage";
import ProblemSubtypeStepPage from "./pages/problems/ProblemSubtypeStepPage.jsx";
import ProblemUploadStepPage from "./pages/problems/ProblemUploadStepPage.jsx";
import ProblemMappingStepPage from "./pages/problems/ProblemMappingStepPage.jsx";
//import ProblemObjectivesStepPage from "./pages/storage/ProblemObjectivesStepPage.jsx";
//import ProblemConstraintsStepPage from "./pages/storage/ProblemConstraintsStepPage.jsx";
import ProblemRoomsUploadStepPage from "./pages/problems/ProblemRoomsUploadStepPage.jsx";
import ProblemRoomsMappingStepPage from "./pages/problems/ProblemRoomsMappingStepPage.jsx";
import ProblemExecutePage from "./pages/problems/ProblemExecutePage.jsx";
import ProblemConstraintsStepPage from "./pages/problems/ProblemConstraintsStepPage.jsx";
import ProblemDetailPage from "./pages/problems/ProblemDetailPage.jsx"
import SolutionScheduleByRoomPage from "./pages/solution/SolutionScheduleByRoomPage.jsx"
import ProblemRoomFeatureResolutionStepPage from "./pages/problems/ProblemRoomFeatureResolutionStepPage.jsx";
import ProblemUploadEditPage from "./pages/problems/editSteps/ProblemUploadEditPage.jsx";
import ProblemRoomsUploadEditPage from "./pages/problems/editSteps/ProblemRoomsUploadEditPage.jsx"
import ProblemMappingEditPage from "./pages/problems/editSteps/ProblemMappingEditPage.jsx"
import ProblemRoomsMappingEditPage from "./pages/problems/editSteps/ProblemRoomsMappingEditPage.jsx"
import ProblemRoomFeatureResolutionEditPage from "./pages/problems/editSteps/ProblemRoomFeatureResolutionEditPage.jsx"
import ProblemConstraintsEditPage from "./pages/problems/editSteps/ProblemConstraintEditPage.jsx"
import SolutionMetricsPage from "./pages/solution/SolutionMetricsPage.jsx";

export default function App() {
  return (
    <BrowserRouter>
      <ProblemWizardProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/problems" replace />} />
          <Route path="/problems" element={<ProblemHomePage />} />
          <Route path="/problems/new" element={<ProblemWizardStartPage />} />
          <Route path="/problems/:id/type" element={<ProblemTypeStepPage />} />
          <Route path="/problems/:id/subtype" element={<ProblemSubtypeStepPage />} />
          <Route path="/problems/:id/upload" element={<ProblemUploadStepPage />} />
          <Route path="/problems/:id/mapping" element={<ProblemMappingStepPage />} />
          {/*<Route path="/problems/:id/objectives" element={<ProblemObjectivesStepPage />} />*/}
          <Route path="/problems/:id/constraints" element={<ProblemConstraintsStepPage />} />
          <Route path="/problems/:id/rooms-upload" element={<ProblemRoomsUploadStepPage />} />
          <Route path="/problems/:id/rooms-mapping" element={<ProblemRoomsMappingStepPage />} />
          <Route path="/problems/:id/room-features" element={<ProblemRoomFeatureResolutionStepPage />} />
          <Route path="/problems/:id/execute" element={<ProblemExecutePage />} />
          <Route path="/problems/:id/detail" element={<ProblemDetailPage />} />
          {/*<Route path="/problems/:problemId/solutions/:solutionId" element={<SolutionDetailPage />} />*/}
          <Route path="/problems/:id/solutions/:solutionId/schedule/rooms" element={<SolutionScheduleByRoomPage />} />
          <Route path="/problems/:id/edit/upload" element={<ProblemUploadEditPage />}/>
          <Route path="/problems/:id/edit/rooms-upload" element={<ProblemRoomsUploadEditPage />}/>
          <Route path="/problems/:id/edit/mapping" element={<ProblemMappingEditPage />}/>
          <Route path="/problems/:id/edit/rooms-mapping" element={<ProblemRoomsMappingEditPage />}/>
          <Route path="/problems/:id/edit/room-features" element={<ProblemRoomFeatureResolutionEditPage />}/>
          <Route path="/problems/:id/edit/constraints" element={<ProblemConstraintsEditPage />}/>
          <Route path="/problems/:id/solutions/:solutionId/metrics" element={<SolutionMetricsPage />}/>
        </Routes>
      </ProblemWizardProvider>
    </BrowserRouter>
  );
}