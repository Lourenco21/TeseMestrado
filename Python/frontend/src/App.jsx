import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProblemWizardProvider } from "./contexts/ProblemWizardContext";
import ProblemHomePage from "./pages/problems/ProblemHomePage";
import ProblemWizardStartPage from "./pages/problems/ProblemWizardStartPage";
import ProblemTypeStepPage from "./pages/problems/ProblemTypeStepPage";
import ProblemSubtypeStepPage from "./pages/problems/ProblmeSubtypeStepPage.jsx";
import ProblemUploadStepPage from "./pages/problems/ProblemUploadStepPage.jsx";
import ProblemMappingStepPage from "./pages/problems/ProblemMappingStepPage.jsx";
import ProblemObjectivesStepPage from "./pages/problems/ProblemObjectivesStepPage.jsx";
import ProblemConstraintsStepPage from "./pages/problems/ProblemConstraintsStepPage.jsx";

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
          <Route path="/problems/:id/objectives" element={<ProblemObjectivesStepPage />} />
          <Route path="/problems/:id/constraints" element={<ProblemConstraintsStepPage />} />
        </Routes>
      </ProblemWizardProvider>
    </BrowserRouter>
  );
}