import { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemCatalog } from "../../services/problemsApi";

export default function ProblemObjectivesStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [objectiveLibrary, setObjectiveLibrary] = useState({});
  const [selectedObjectives, setSelectedObjectives] = useState([]);
  const [localError, setLocalError] = useState("");
  const [catalogLoaded, setCatalogLoaded] = useState(false);

  const loadCatalog = useCallback(async () => {
    if (catalogLoaded) return;

    try {
      const catalog = await getProblemCatalog();
      setObjectiveLibrary(catalog.objective_library || {});
      setCatalogLoaded(true);
    } catch (err) {
      console.error("Erro ao carregar catálogo:", err);
      setLocalError("Não foi possível carregar o catálogo de objetivos.");
    }
  }, [catalogLoaded]);

  const loadDraftOnce = useCallback(async () => {
    if (problemDraft) return;

    try {
      await loadDraft(id);
    } catch (err) {
      console.error("Erro ao carregar draft:", err);
      setLocalError("Não foi possível carregar o problema.");
    }
  }, [id, problemDraft, loadDraft]);

  useEffect(() => {
    if (problemDraft?.selected_objectives) {
      setSelectedObjectives(problemDraft.selected_objectives);
    }
  }, [problemDraft?.selected_objectives]);

  useEffect(() => {
    let cancelled = false;

    async function init() {
      try {
        setLocalError("");
        await loadDraftOnce();

        if (!cancelled) {
          await loadCatalog();
        }
      } catch (err) {
        if (!cancelled) {
          setLocalError(err.message || "Erro ao inicializar página.");
        }
      }
    }

    init();

    return () => {
      cancelled = true;
    };
  }, [loadDraftOnce, loadCatalog]);

  const availableObjectives = useMemo(() => {
    const family = problemDraft?.problem_family;
    if (!family || !catalogLoaded) return [];
    return objectiveLibrary[family] || [];
  }, [objectiveLibrary, problemDraft?.problem_family, catalogLoaded]);

  const enabledObjectives = useMemo(() => {
    return selectedObjectives.filter((item) => item.enabled);
  }, [selectedObjectives]);

  const toggleObjective = useCallback((objectiveId) => {
    setSelectedObjectives((prev) => {
      const exists = prev.some((item) => item.id === objectiveId);

      if (exists) {
        return prev.filter((item) => item.id !== objectiveId);
      }

      return [
        ...prev,
        {
          id: objectiveId,
          enabled: true,
          goal: "minimize",
        },
      ];
    });
  }, []);

  const updateGoal = useCallback((objectiveId, goal) => {
    setSelectedObjectives((prev) =>
      prev.map((item) =>
        item.id === objectiveId ? { ...item, goal } : item
      )
    );
  }, []);

  const handleContinue = useCallback(async () => {
    const validObjectives = selectedObjectives
      .filter((item) => item.enabled)
      .map((item) => ({
        ...item,
        goal: item.goal || "minimize",
      }));

    if (validObjectives.length === 0) {
      setLocalError("Seleciona pelo menos um objetivo antes de continuar.");
      return;
    }

    try {
      setLocalError("");

      await saveDraft({
        status: "objectives_selected",
        selected_objectives: validObjectives,
        current_step: 6,
        last_completed_step: 5,
      });

      navigate(`/problems/${id}/constraints`);
    } catch (err) {
      console.error("Erro ao guardar objetivos:", err);
      setLocalError(err.message || "Não foi possível guardar os objetivos.");
    }
  }, [selectedObjectives, id, saveDraft, navigate]);

  const handleBack = useCallback(() => {
    navigate(`/problems/${id}/mapping`);
  }, [id, navigate]);

  if (loading && !problemDraft) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A carregar problema...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <div style={styles.header}>
          <p style={styles.step}>Passo 5 de 7</p>
          <h1 style={styles.title}>Selecionar objetivos</h1>
          <p style={styles.description}>
            Escolhe os objetivos a otimizar e define, para cada um, se o algoritmo
            deve minimizar ou maximizar.
          </p>
        </div>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
        {localError ? <p style={styles.error}>{localError}</p> : null}

        {availableObjectives.length > 0 ? (
          <>
            <div style={styles.summaryBar}>
              <span style={styles.summaryItem}>
                Objetivos selecionados: {enabledObjectives.length}
              </span>
            </div>

            <div style={styles.list}>
              {availableObjectives.map((objective) => {
                const selected = selectedObjectives.some(
                  (item) => item.id === objective.id && item.enabled
                );

                const current = selectedObjectives.find(
                  (item) => item.id === objective.id
                );

                return (
                  <div
                    key={objective.id}
                    style={{
                      ...styles.listItem,
                      ...(selected ? styles.listItemActive : {}),
                    }}
                  >
                    <div style={styles.listMain}>
                      <label style={styles.checkboxLabel}>
                        <input
                          type="checkbox"
                          checked={selected}
                          onChange={() => toggleObjective(objective.id)}
                          style={styles.checkbox}
                        />

                        <div style={styles.objectiveContent}>
                          <div style={styles.objectiveTitleRow}>
                            <h2 style={styles.objectiveTitle}>{objective.label}</h2>
                            <span
                              style={{
                                ...styles.statusBadge,
                                ...(selected
                                  ? styles.statusBadgeActive
                                  : styles.statusBadgeInactive),
                              }}
                            >
                              {selected ? "Selecionado" : "Não selecionado"}
                            </span>
                          </div>

                          <p style={styles.objectiveDescription}>
                            {objective.description || "Sem descrição disponível."}
                          </p>
                        </div>
                      </label>
                    </div>

                    {selected ? (
                      <div style={styles.objectiveControls}>
                        <div style={styles.controlGroup}>
                          <span style={styles.controlLabel}>Objetivo</span>

                          <div style={styles.radioGroup}>
                            <label style={styles.radioLabel}>
                              <input
                                type="radio"
                                name={`goal-${objective.id}`}
                                checked={(current?.goal || "minimize") === "minimize"}
                                onChange={() => updateGoal(objective.id, "minimize")}
                              />
                              <span>Minimizar</span>
                            </label>

                            <label style={styles.radioLabel}>
                              <input
                                type="radio"
                                name={`goal-${objective.id}`}
                                checked={current?.goal === "maximize"}
                                onChange={() => updateGoal(objective.id, "maximize")}
                              />
                              <span>Maximizar</span>
                            </label>
                          </div>
                        </div>
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </>
        ) : (
          <div style={styles.emptyState}>
            <p style={styles.message}>
              {!catalogLoaded
                ? "A carregar catálogo..."
                : "Não existem objetivos disponíveis para esta família."}
            </p>
          </div>
        )}

        <div style={styles.actions}>
          <button
            type="button"
            onClick={handleBack}
            style={styles.secondaryButton}
          >
            Voltar
          </button>

          <button
            type="button"
            onClick={handleContinue}
            disabled={saving || loading}
            style={{
              ...styles.primaryButton,
              ...(saving || loading ? styles.primaryButtonDisabled : {}),
            }}
          >
            {saving ? "A guardar..." : "Continuar"}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    backgroundColor: "#f8fafc",
    padding: "32px",
  },
  container: {
    maxWidth: "1100px",
    margin: "0 auto",
  },
  header: {
    marginBottom: "24px",
  },
  step: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#475467",
  },
  title: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "36px",
    color: "#101828",
  },
  description: {
    margin: 0,
    marginBottom: "24px",
    maxWidth: "760px",
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
  },
  problemInfo: {
    display: "inline-flex",
    gap: "8px",
    alignItems: "center",
    marginBottom: "24px",
    padding: "10px 14px",
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "10px",
  },
  problemLabel: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#667085",
  },
  problemName: {
    fontSize: "14px",
    fontWeight: 700,
    color: "#101828",
  },
  error: {
    marginBottom: "16px",
    padding: "12px 14px",
    borderRadius: "12px",
    border: "1px solid #fecaca",
    backgroundColor: "#fef2f2",
    fontSize: "15px",
    color: "#b42318",
  },
  message: {
    fontSize: "16px",
    color: "#475467",
  },
  summaryBar: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
    marginBottom: "16px",
  },
  summaryItem: {
    padding: "10px 14px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "14px",
    fontWeight: 600,
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "14px",
    marginTop: "20px",
  },
  listItem: {
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    backgroundColor: "#ffffff",
    padding: "18px",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "20px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.04)",
    flexWrap: "wrap",
  },
  listItemActive: {
    borderColor: "#175cd3",
    backgroundColor: "#f8fbff",
  },
  listMain: {
    flex: 1,
    minWidth: "280px",
  },
  checkboxLabel: {
    display: "flex",
    alignItems: "flex-start",
    gap: "12px",
    cursor: "pointer",
  },
  checkbox: {
    marginTop: "4px",
    width: "16px",
    height: "16px",
    accentColor: "#175cd3",
    cursor: "pointer",
  },
  objectiveContent: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  objectiveTitleRow: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexWrap: "wrap",
  },
  objectiveTitle: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#101828",
  },
  objectiveDescription: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  statusBadge: {
    display: "inline-flex",
    alignItems: "center",
    padding: "4px 10px",
    borderRadius: "999px",
    fontSize: "12px",
    fontWeight: 700,
  },
  statusBadgeActive: {
    backgroundColor: "#dbeafe",
    color: "#1d4ed8",
  },
  statusBadgeInactive: {
    backgroundColor: "#f2f4f7",
    color: "#667085",
  },
  objectiveControls: {
    minWidth: "280px",
    display: "flex",
    flexDirection: "column",
    gap: "14px",
    paddingLeft: "8px",
  },
  controlGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  controlLabel: {
    fontSize: "13px",
    fontWeight: 700,
    color: "#344054",
  },
  radioGroup: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
  },
  radioLabel: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    fontSize: "14px",
    color: "#344054",
    cursor: "pointer",
  },
  emptyState: {
    padding: "24px",
    border: "1px dashed #d0d5dd",
    borderRadius: "16px",
    backgroundColor: "#ffffff",
    marginTop: "20px",
  },
  actions: {
    display: "flex",
    justifyContent: "space-between",
    gap: "16px",
    flexWrap: "wrap",
    marginTop: "32px",
  },
  secondaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#101828",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
  },
  primaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "none",
    backgroundColor: "#0f62fe",
    color: "#ffffff",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
  },
  primaryButtonDisabled: {
    opacity: 0.7,
    cursor: "not-allowed",
  },
};