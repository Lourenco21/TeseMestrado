import { createContext, useContext, useMemo, useState } from "react";
import {
  createProblemDraft,
  getProblemDraft,
  updateProblemDraft,
} from "../services/problemsApi";

const ProblemWizardContext = createContext(null);

export function ProblemWizardProvider({ children }) {
  const [problemDraft, setProblemDraft] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function createDraft(initialData = {}) {
    try {
      setLoading(true);
      setError("");
      const createdDraft = await createProblemDraft(initialData);
      setProblemDraft(createdDraft);
      return createdDraft;
    } catch (err) {
      setError(err.message || "Não foi possível criar o problema.");
      throw err;
    } finally {
      setLoading(false);
    }
  }

  async function loadDraft(problemId) {
    try {
      setLoading(true);
      setError("");
      const draft = await getProblemDraft(problemId);
      setProblemDraft(draft);
      return draft;
    } catch (err) {
      setError(err.message || "Não foi possível carregar o problema.");
      throw err;
    } finally {
      setLoading(false);
    }
  }

  async function saveDraft(updates) {
    if (!problemDraft?.id) {
      throw new Error("Não existe nenhum draft carregado.");
    }

    try {
      setSaving(true);
      setError("");
      const updatedDraft = await updateProblemDraft(problemDraft.id, updates);
      setProblemDraft(updatedDraft);
      return updatedDraft;
    } catch (err) {
      setError(err.message || "Não foi possível guardar o problema.");
      throw err;
    } finally {
      setSaving(false);
    }
  }

  function clearDraft() {
    setProblemDraft(null);
    setError("");
  }

  const value = useMemo(
    () => ({
      problemDraft,
      setProblemDraft,
      loading,
      saving,
      error,
      createDraft,
      loadDraft,
      saveDraft,
      clearDraft,
    }),
    [problemDraft, loading, saving, error]
  );

  return (
    <ProblemWizardContext.Provider value={value}>
      {children}
    </ProblemWizardContext.Provider>
  );
}

export function useProblemWizard() {
  const context = useContext(ProblemWizardContext);

  if (!context) {
    throw new Error(
      "useProblemWizard must be used within a ProblemWizardProvider"
    );
  }

  return context;
}