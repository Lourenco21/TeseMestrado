import { useEffect, useMemo, useState, useCallback, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemCatalog } from "../../services/problemsApi";

export default function ProblemConstraintsStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [constraintLibrary, setConstraintLibrary] = useState({});
  const [selectedConstraints, setSelectedConstraints] = useState([]);
  const [localError, setLocalError] = useState("");
  const [catalogLoaded, setCatalogLoaded] = useState(false);
  const hasSyncedRef = useRef(false);

  const loadCatalog = useCallback(async () => {
    if (catalogLoaded) return;

    try {
      const catalog = await getProblemCatalog();
      setConstraintLibrary(catalog.constraint_library || {});
      setCatalogLoaded(true);
    } catch (err) {
      console.error("Erro ao carregar catálogo:", err);
      setLocalError("Não foi possível carregar o catálogo de constraints.");
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
    hasSyncedRef.current = false;
  }, [problemDraft?.id]);

  useEffect(() => {
    if (!problemDraft || hasSyncedRef.current) return;

    const draftConstraints = problemDraft.selected_constraints || [];
    const normalizedConstraints = draftConstraints.map((item) => ({
      ...item,
      goal: item.goal || "hard",
    }));

    setSelectedConstraints(normalizedConstraints);
    hasSyncedRef.current = true;
  }, [problemDraft]);

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

  const availableConstraints = useMemo(() => {
    const family = problemDraft?.problem_family;
    if (!family || !catalogLoaded) return [];
    return constraintLibrary[family] || [];
  }, [constraintLibrary, problemDraft?.problem_family, catalogLoaded]);

  const enabledConstraints = useMemo(() => {
    return selectedConstraints.filter((item) => item.enabled);
  }, [selectedConstraints]);

  const toggleConstraint = useCallback((constraintId) => {
    setSelectedConstraints((prev) => {
      const exists = prev.some((item) => item.id === constraintId);

      if (exists) {
        return prev.filter((item) => item.id !== constraintId);
      }

      return [
        ...prev,
        {
          id: constraintId,
          enabled: true,
          goal: "soft",
        },
      ];
    });
  }, []);

  const updateGoal = useCallback((constraintId, goal) => {
    setSelectedConstraints((prev) =>
      prev.map((item) =>
        item.id === constraintId ? { ...item, goal } : item
      )
    );
  }, []);

  const handleContinue = useCallback(async () => {
    const validConstraints = selectedConstraints
      .filter((item) => item.enabled)
      .map((item) => ({
        ...item,
        goal: item.goal || "soft",
      }));

    if (validConstraints.length === 0) {
      setLocalError("Seleciona pelo menos uma constraint antes de continuar.");
      return;
    }

    try {
      setLocalError("");

      await saveDraft({
        status: "constraints_selected",
        selected_constraints: validConstraints,
        current_step: 7,
        last_completed_step: 6,
      });

      navigate(`/problems/${id}/review`);
    } catch (err) {
      console.error("Erro ao guardar constraints:", err);
      setLocalError(err.message || "Não foi possível guardar as constraints.");
    }
  }, [selectedConstraints, id, saveDraft, navigate]);

  const handleBack = useCallback(() => {
    navigate(`/problems/${id}/objectives`);
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
          <p style={styles.step}>Passo 6 de 7</p>
          <h1 style={styles.title}>Selecionar constraints</h1>
          <p style={styles.description}>
            Escolhe as regras a aplicar ao problema e define se cada uma é obrigatória ou preferencial.
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

        {availableConstraints.length > 0 ? (
          <>
            <div style={styles.summaryBar}>
              <span style={styles.summaryItem}>
                Constraints selecionadas: {enabledConstraints.length}
              </span>
            </div>

            <div style={styles.list}>
              {availableConstraints.map((constraint) => {
                const selected = selectedConstraints.some(
                  (item) => item.id === constraint.id && item.enabled
                );

                const current = selectedConstraints.find(
                  (item) => item.id === constraint.id
                );

                return (
                  <div
                    key={constraint.id}
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
                          onChange={() => toggleConstraint(constraint.id)}
                          style={styles.checkbox}
                        />

                        <div style={styles.constraintContent}>
                          <div style={styles.constraintTitleRow}>
                            <h2 style={styles.constraintTitle}>
                              {constraint.label}
                            </h2>

                            <span
                              style={{
                                ...styles.statusBadge,
                                ...(selected
                                  ? styles.statusBadgeActive
                                  : styles.statusBadgeInactive),
                              }}
                            >
                              {selected ? "Selecionada" : "Não selecionada"}
                            </span>
                          </div>

                          <p style={styles.constraintDescription}>
                            {constraint.description || "Sem descrição disponível."}
                          </p>
                        </div>
                      </label>
                    </div>

                    {selected ? (
                      <div style={styles.constraintControls}>
                        <div style={styles.controlGroup}>
                          <span style={styles.controlLabel}>Importância</span>

                          <div style={styles.radioGroup}>
                            <label style={styles.radioLabel}>
                              <input
                                type="radio"
                                name={`goal-${constraint.id}`}
                                checked={(current?.goal || "soft") === "hard"}
                                onChange={() => updateGoal(constraint.id, "hard")}
                              />
                              <span>Obrigatória</span>
                            </label>

                            <label style={styles.radioLabel}>
                              <input
                                type="radio"
                                name={`goal-${constraint.id}`}
                                checked={current?.goal === "soft"}
                                onChange={() => updateGoal(constraint.id, "soft")}
                              />
                              <span>Preferencial</span>
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
                : "Não existem constraints disponíveis para esta família."}
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
  constraintContent: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  constraintTitleRow: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexWrap: "wrap",
  },
  constraintTitle: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#101828",
  },
  constraintDescription: {
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
  constraintControls: {
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