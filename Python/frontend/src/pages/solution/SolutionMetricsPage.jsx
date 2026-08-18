import { useEffect, useMemo, useState } from "react";
import { NavLink, useNavigate, useParams } from "react-router-dom";
import { getProblemSolutionMetrics } from "../../services/solutionsApi";
import { getProblemDraft } from "../../services/problemsApi"; // ajusta ao nome real do teu service

const METRIC_LABELS = {
  room_capacity_sufficiency: {
    title: "Capacidade da sala",
    description: "Aulas atribuídas a salas com capacidade insuficiente para o número de inscritos.",
  },
  room_exclusivity: {
    title: "Exclusividade da sala",
    description: "Sobreposições detetadas em que a mesma sala é usada por duas aulas em simultâneo.",
  },
  capacity_waste: {
    title: "Desperdício de capacidade",
    description: "Lugares vagos nas salas atribuídas, penalizados por aula.",
  },
  room_feature_mismatch: {
    title: "Características da sala",
    description: "Características pedidas para a aula que a sala atribuída não possui.",
  },
  consecutive_room_change: {
    title: "Mudança consecutiva de sala",
    description: "Trocas de sala entre aulas consecutivas da mesma turma, no mesmo dia.",
  },
  student_relocation: {
    title: "Deslocação de estudantes",
    description: "Mudanças de edifício entre aulas consecutivas do mesmo grupo de alunos.",
  },
};

const METRIC_VALUE_FIELDS = {
  room_capacity_sufficiency: [{ key: "total_violations", label: "Violações totais" }],
  room_exclusivity: [{ key: "total_overlaps", label: "Sobreposições totais" }],
  capacity_waste: [{ key: "total_waste", label: "Desperdício total" }],
  room_feature_mismatch: [{ key: "total_mismatches", label: "Incompatibilidades totais" }],
  consecutive_room_change: [{ key: "room_changes_detected", label: "Trocas detetadas" },],
  student_relocation: [{ key: "relocations_detected", label: "Deslocações detetadas" },],
};

export default function SolutionMetricsPage() {
  const { id, solutionId } = useParams();
  const navigate = useNavigate();

  const [solution, setSolution] = useState(null);
  const [baselineMetrics, setBaselineMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedConstraints, setSelectedConstraints] = useState([]);

  useEffect(() => {
    async function loadMetrics() {
      try {
        setLoading(true);
        setError("");

        const [solutionData, problemDraft] = await Promise.all([
          getProblemSolutionMetrics(id, solutionId),
          getProblemDraft(id),
        ]);

        setSolution(solutionData);
        setBaselineMetrics(problemDraft?.baseline_metrics || null);
        setSelectedConstraints(problemDraft?.selected_constraints || []);
      } catch (err) {
        console.error(err);
        setError(err.message || "Erro ao carregar métricas da solução.");
      } finally {
        setLoading(false);
      }
    }

    if (id && solutionId) {
      loadMetrics();
    }
  }, [id, solutionId]);

  const constraintGoalMap = useMemo(() => {
    const map = {};
    for (const constraint of selectedConstraints) {
      if (constraint?.id) {
        map[constraint.id] = constraint.goal;
      }
    }
    return map;
  }, [selectedConstraints]);

  const metricEntries = useMemo(() => {
    if (!solution?.metrics) {
      return [];
    }

    return Object.entries(solution.metrics)
      .filter(([, value]) => value && !value.error)
      .map(([constraintId, value]) => {
        const baselineValue = baselineMetrics?.[constraintId];
        const baselineHasError = baselineValue && baselineValue.error;

        return {
          id: constraintId,
          label: METRIC_LABELS[constraintId]?.title || constraintId,
          description: METRIC_LABELS[constraintId]?.description || "",
          fields: METRIC_VALUE_FIELDS[constraintId] || [],
          values: value,
          baselineValues: baselineHasError ? null : baselineValue || null,
          goal: constraintGoalMap[constraintId] || null,
        };
      });
  }, [solution, baselineMetrics, constraintGoalMap]);

  const errorEntries = useMemo(() => {
    if (!solution?.metrics) {
      return [];
    }

    return Object.entries(solution.metrics)
      .filter(([, value]) => value && value.error)
      .map(([constraintId, value]) => ({
        id: constraintId,
        label: METRIC_LABELS[constraintId]?.title || constraintId,
        error: value.error,
      }));
  }, [solution]);

  const hasBaseline = useMemo(() => {
    return Boolean(baselineMetrics) && Object.keys(baselineMetrics || {}).length > 0;
  }, [baselineMetrics]);

  if (loading) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A calcular métricas da solução...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <div style={styles.error}>{error}</div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Solução</p>
        <h1 style={styles.title}>Métricas de qualidade</h1>
        <p style={styles.description}>
          Comparação entre a linha de base (dados originais, antes da otimização) e os
          resultados obtidos por esta solução, para cada restrição selecionada.
        </p>

        <div style={styles.switcher}>
          <NavLink
            to={`/problems/${id}/solutions/${solutionId}/metrics`}
            style={{ ...styles.switcherButton, ...styles.switcherButtonActive }}
          >
            Dados
          </NavLink>
          
          <NavLink
            to={`/problems/${id}/solutions/${solutionId}/schedule/rooms`}
            style={styles.switcherButton}
          >
            Horário por sala
          </NavLink>
        </div>

        <div style={styles.toolbarCard}>
          <div>
            <h2 style={styles.toolbarTitle}>Solução #{solution?.id}</h2>
            <p style={styles.toolbarHint}>
              Algoritmo: {solution?.algorithm_used || "Algoritmo desconhecido"}
            </p>
            <p style={styles.toolbarHint}>
              Tempo de execução: {formatExecutionTime(solution?.execution_time_seconds)}
            </p>
          </div>
        </div>

        {!hasBaseline ? (
          <div style={styles.infoBanner}>
            Não existe linha de base calculada para este problema. A comparar apenas os
            valores da solução atual.
          </div>
        ) : null}

        {errorEntries.length > 0 ? (
          <div style={styles.errorBanner}>
            Algumas métricas não puderam ser calculadas:{" "}
            {errorEntries.map((entry) => entry.label).join(", ")}.
          </div>
        ) : null}

        <div style={styles.metricsGrid}>
          {metricEntries.map((entry) => (
            <div key={entry.id} style={styles.metricCard}>
              {entry.goal ? (
                <div
                  style={{
                    ...styles.goalBadge,
                    ...(entry.goal === "hard" ? styles.goalBadgeHard : styles.goalBadgeSoft),
                  }}
                >
                  {entry.goal === "hard" ? "Obrigatória" : "Preferencial"}
                </div>
              ) : null}

              <h3 style={styles.metricCardTitle}>{entry.label}</h3>
              <p style={styles.metricCardDescription}>{entry.description}</p>

              <div style={styles.comparisonList}>
                {entry.fields.map((field) => {
                  const currentValue = entry.values[field.key];
                  const baselineValue = entry.baselineValues?.[field.key];

                  return (
                    <div key={field.key} style={styles.comparisonRow}>
                      <div style={styles.comparisonLabel}>{field.label}</div>

                      <div style={styles.comparisonValues}>
                        <div style={styles.comparisonBlock}>
                          <div style={styles.comparisonBlockLabel}>Original</div>
                          <div style={styles.comparisonBlockValueMuted}>
                            {formatMetricValue(baselineValue)}
                          </div>
                        </div>

                        <div style={styles.comparisonArrow}>→</div>

                        <div style={styles.comparisonBlock}>
                          <div style={styles.comparisonBlockLabel}>Solução</div>
                          <div style={styles.comparisonBlockValue}>
                            {formatMetricValue(currentValue)}
                          </div>
                        </div>

                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}

          {metricEntries.length === 0 ? (
            <div style={styles.emptyState}>
              Nenhuma métrica disponível para as restrições selecionadas neste problema.
            </div>
          ) : null}
        </div>

        <div style={styles.bottomActions}>
          <button
            type="button"
            onClick={() => navigate(`/problems/${id}/detail`)}
            style={styles.secondaryButton}
          >
            Voltar ao problema
          </button>
        </div>
      </div>
    </div>
  );
}




function formatExecutionTime(seconds) {
  if (seconds === undefined || seconds === null) {
    return "-";
  }

  if (seconds < 60) {
    return `${seconds.toFixed(2)}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.round(seconds % 60);
  return `${minutes}m ${remainingSeconds}s`;
}

function formatMetricValue(value) {
  if (value === undefined || value === null) {
    return "-";
  }

  if (typeof value === "number") {
    return Number.isInteger(value) ? value.toString() : value.toFixed(2);
  }

  return String(value);
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "#f3f4f6",
    padding: "24px",
  },
  container: {
    maxWidth: "1400px",
    margin: "0 auto",
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
    lineHeight: 1.1,
    color: "#0f172a",
  },
  description: {
    margin: 0,
    marginBottom: "20px",
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
  },
  message: {
    fontSize: "16px",
    color: "#475467",
  },
  error: {
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#fef2f2",
    border: "1px solid #fecaca",
    color: "#b42318",
    fontSize: "14px",
  },
  errorBanner: {
    marginBottom: "16px",
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#fff7ed",
    border: "1px solid #fed7aa",
    color: "#9a3412",
    fontSize: "13px",
    fontWeight: 600,
  },
  infoBanner: {
    marginBottom: "16px",
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#eff6ff",
    border: "1px solid #bfdbfe",
    color: "#1d4ed8",
    fontSize: "13px",
    fontWeight: 600,
  },
  switcher: {
    display: "flex",
    gap: "8px",
    alignItems: "center",
    marginBottom: "16px",
    flexWrap: "wrap",
  },
  switcherButton: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    minHeight: "42px",
    padding: "0 14px",
    border: "1px solid #d1d5db",
    borderRadius: "10px",
    background: "#ffffff",
    color: "#111827",
    fontSize: "14px",
    fontWeight: 600,
    textDecoration: "none",
  },
  switcherButtonActive: {
    background: "#111827",
    color: "#ffffff",
    borderColor: "#111827",
  },
  toolbarCard: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-end",
    gap: "16px",
    marginBottom: "16px",
    padding: "18px",
    background: "#ffffff",
    border: "1px solid #e5e7eb",
    borderRadius: "14px",
    flexWrap: "wrap",
  },
  toolbarTitle: {
    margin: 0,
    fontSize: "24px",
    color: "#0f172a",
  },
  toolbarHint: {
    margin: "6px 0 10px 0",
    fontSize: "16px",
    color: "#6b7280",
  },
  metricsGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))",
    gap: "16px",
    marginBottom: "18px",
  },
  metricCard: {
  position: "relative",
  background: "#ffffff",
  border: "1px solid #e5e7eb",
  borderRadius: "14px",
  padding: "18px",
  },
  goalBadge: {
    position: "absolute",
    top: "14px",
    right: "14px",
    padding: "4px 10px",
    borderRadius: "999px",
    fontSize: "11px",
    fontWeight: 700,
    textTransform: "uppercase",
    letterSpacing: "0.02em",
    whiteSpace: "nowrap",
  },
  goalBadgeHard: {
    background: "#fef2f2",
    color: "#b42318",
    border: "1px solid #fecaca",
  },
  goalBadgeSoft: {
    background: "#eff6ff",
    color: "#1d4ed8",
    border: "1px solid #bfdbfe",
  },
  metricCardTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "16px",
    color: "#0f172a",
  },
  metricCardDescription: {
    margin: 0,
    marginBottom: "14px",
    fontSize: "13px",
    color: "#6b7280",
    lineHeight: 1.5,
  },
  comparisonList: {
    display: "flex",
    flexDirection: "column",
    gap: "14px",
  },
  comparisonRow: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    paddingTop: "10px",
    borderTop: "1px solid #f1f5f9",
  },
  comparisonLabel: {
    fontSize: "12px",
    color: "#6b7280",
    fontWeight: 600,
  },
  comparisonValues: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexWrap: "wrap",
  },
  comparisonBlock: {
    display: "flex",
    flexDirection: "column",
    minWidth: "56px",
  },
  comparisonBlockLabel: {
    fontSize: "11px",
    color: "#9ca3af",
    fontWeight: 600,
  },
  comparisonBlockValue: {
    fontSize: "22px",
    fontWeight: 700,
    color: "#111827",
  },
  comparisonBlockValueMuted: {
    fontSize: "22px",
    fontWeight: 700,
    color: "#9ca3af",
  },
  comparisonArrow: {
    fontSize: "16px",
    color: "#9ca3af",
  },
  deltaBadge: {
    display: "inline-flex",
    alignItems: "center",
    gap: "4px",
    padding: "4px 10px",
    borderRadius: "999px",
    fontSize: "12px",
    fontWeight: 700,
    whiteSpace: "nowrap",
  },
  deltaBadgeBetter: {
    background: "#ecfdf5",
    color: "#047857",
    border: "1px solid #a7f3d0",
  },
  deltaBadgeWorse: {
    background: "#fef2f2",
    color: "#b42318",
    border: "1px solid #fecaca",
  },
  deltaBadgeNeutral: {
    background: "#f3f4f6",
    color: "#6b7280",
    border: "1px solid #e5e7eb",
  },
  emptyState: {
    gridColumn: "1 / -1",
    padding: "20px",
    textAlign: "center",
    color: "#6b7280",
    fontSize: "14px",
    background: "#ffffff",
    border: "1px dashed #d1d5db",
    borderRadius: "14px",
  },
  bottomActions: {
    marginTop: "18px",
    display: "flex",
    justifyContent: "flex-start",
  },
  secondaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#101828",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
  },
};