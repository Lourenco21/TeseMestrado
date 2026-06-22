import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getProblemDraft } from "../../services/problemsApi";
import { listProblemSolutions } from "../../services/solutionsApi";

export default function ProblemDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [problem, setProblem] = useState(null);
  const [solutions, setSolutions] = useState([]);

  const [loadingProblem, setLoadingProblem] = useState(true);
  const [loadingSolutions, setLoadingSolutions] = useState(true);

  const [problemError, setProblemError] = useState("");
  const [solutionsError, setSolutionsError] = useState("");
  const [hoveredTile, setHoveredTile] = useState(null);

  useEffect(() => {
    async function fetchProblem() {
      try {
        setLoadingProblem(true);
        setProblemError("");

        const data = await getProblemDraft(id);
        setProblem(data);
      } catch (error) {
        setProblemError(error.message || "Erro ao carregar problema.");
      } finally {
        setLoadingProblem(false);
      }
    }

    if (id) {
      fetchProblem();
    }
  }, [id]);

  useEffect(() => {
    async function fetchSolutions() {
      try {
        setLoadingSolutions(true);
        setSolutionsError("");

        const data = await listProblemSolutions(id);
        setSolutions(Array.isArray(data) ? data : []);
      } catch (error) {
        setSolutionsError(error.message || "Erro ao carregar soluções.");
      } finally {
        setLoadingSolutions(false);
      }
    }

    if (id) {
      fetchSolutions();
    }
  }, [id]);

  const selectedConstraintsCount = Array.isArray(problem?.selected_constraints)
    ? problem.selected_constraints.length
    : 0;

  const selectedConstraintsLabel =
    selectedConstraintsCount === 1
      ? "1 selecionada"
      : `${selectedConstraintsCount} selecionadas`;

  const handleGoToExecute = () => {
    navigate(`/problems/${id}/execute`);
  };

  const handleOpenSolution = (solutionId) => {
    navigate(`/problems/${id}/solutions/${solutionId}/schedule/rooms`);
  };

  if (loadingProblem) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A carregar problema...</p>
        </div>
      </div>
    );
  }

  if (problemError) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <div style={styles.error}>{problemError}</div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Detalhe do problema</p>
        <h1 style={styles.title}>{problem?.name || `Problema #${id}`}</h1>
        <p style={styles.description}>
          Página de detalhe do problema e respetivas soluções.
        </p>

        <div style={styles.problemInfo}>
          <span style={styles.problemLabel}>Número de Soluções:</span>
          <span style={styles.problemName}>{solutions.length}</span>
        </div>

        <div style={styles.actions}>
          <button style={styles.secondaryButton} onClick={() => navigate("/problems")}>
            Voltar
          </button>

          <button style={styles.primaryButton} onClick={handleGoToExecute}>
            Executar problema
          </button>
        </div>

        <section style={styles.sectionCard}>
          <h2 style={styles.sectionTitle}>Informação geral</h2>
          <p style={styles.sectionDescription}>
            Dados principais do problema atualmente guardado.
          </p>

          <div style={styles.infoGrid}>
            <InfoCard label="Nome" value={problem?.name || "-"} />
            <InfoCard
              label="Tipo"
              value={problem?.problem_family || problem?.type || "-"}
            />
            <InfoCard
              label="Subtipo"
              value={problem?.problem_subtype || problem?.subtype || "-"}
            />
            <InfoCard
              label="Última atualização"
              value={
                problem?.updated_at
                  ? new Date(problem.updated_at).toLocaleString("pt-PT")
                  : "-"
              }
            />
          </div>
        </section>

        <section style={styles.sectionCard}>
          <h2 style={styles.sectionTitle}>Ficheiros e mapping</h2>
          <p style={styles.sectionDescription}>
            Estado dos ficheiros associados ao problema e respetivo mapping.
          </p>

          <div style={styles.infoGrid}>
            <EditableTile
              tileId="schedule-file"
              label="Ficheiro de horário"
              value={problem?.uploaded_schedule_name || "-"}
              onClick={() => navigate(`/problems/${id}/upload`)}
              hoveredTile={hoveredTile}
              setHoveredTile={setHoveredTile}
            />

            <EditableTile
              tileId="rooms-file"
              label="Ficheiro de salas"
              value={problem?.uploaded_rooms_file_name || "-"}
              onClick={() => navigate(`/problems/${id}/rooms-upload`)}
              hoveredTile={hoveredTile}
              setHoveredTile={setHoveredTile}
            />

            <EditableTile
              tileId="schedule-mapping"
              label="Mapping de horário"
              value={
                hasContent(problem?.mapping_data) ? "Configurado" : "Não configurado"
              }
              onClick={() => navigate(`/problems/${id}/mapping`)}
              hoveredTile={hoveredTile}
              setHoveredTile={setHoveredTile}
            />

            <EditableTile
              tileId="rooms-mapping"
              label="Mapping de salas"
              value={
                hasContent(problem?.rooms_mapping_data)
                  ? "Configurado"
                  : "Não configurado"
              }
              onClick={() => navigate(`/problems/${id}/rooms-mapping`)}
              hoveredTile={hoveredTile}
              setHoveredTile={setHoveredTile}
            />
          </div>
        </section>

        <section style={styles.sectionCard}>
          <h2 style={styles.sectionTitle}>Restrições</h2>
          <p style={styles.sectionDescription}>
            Restrições atualmente associadas ao problema.
          </p>

          <div style={styles.twoColumnSection}>
            <EditableTile
              tileId="constraints-list"
              label={`Lista de restrições`}
              value={
                selectedConstraintsCount > 0
                  ? selectedConstraintsLabel
                  : "Sem restrições selecionadas"
              }
              onClick={() => navigate(`/problems/${id}/constraints`)}
              hoveredTile={hoveredTile}
              setHoveredTile={setHoveredTile}
              fullWidth
              largeEditBadge
            >
              {selectedConstraintsCount > 0 ? (
                <ul style={styles.tagList}>
                  {problem.selected_constraints.map((item, index) => (
                    <li key={item?.id || item || index} style={styles.tag}>
                      {typeof item === "string"
                        ? item
                        : item?.label || item?.id || "-"}
                    </li>
                  ))}
                </ul>
              ) : (
                <p style={styles.emptyText}>Sem restrições selecionadas.</p>
              )}
            </EditableTile>
          </div>
        </section>

        <section style={styles.sectionCard}>
          <h2 style={styles.sectionTitle}>Soluções</h2>
          <p style={styles.sectionDescription}>
            Soluções já associadas a este problema.
          </p>

          <div style={styles.summaryBar}>
            <span style={styles.summaryItem}>
              {loadingSolutions ? "A carregar soluções..." : `${solutions.length} solução(ões)`}
            </span>
          </div>

          {solutionsError ? <div style={styles.error}>{solutionsError}</div> : null}

          {loadingSolutions && <p style={styles.message}>A carregar soluções...</p>}

          {!loadingSolutions && !solutionsError && solutions.length === 0 && (
            <div style={styles.warningBox}>
              <p style={styles.warningTitle}>Sem soluções disponíveis</p>
              <p style={styles.warningText}>
                Ainda não existem soluções para este problema.
              </p>

              <div style={styles.actionsRow}>
                <button style={styles.primaryButton} onClick={handleGoToExecute}>
                  Ir para execução
                </button>
              </div>
            </div>
          )}

          {!loadingSolutions && !solutionsError && solutions.length > 0 && (
            <div style={styles.solutionList}>
              {solutions.map((solution) => (
                <button
                  key={solution.id}
                  type="button"
                  onClick={() => handleOpenSolution(solution.id)}
                  style={styles.solutionCard}
                >
                  <div style={styles.solutionCardTop}>
                    <h3 style={styles.solutionTitle}>Solução #{solution.id}</h3>
                    <span style={styles.solutionStatus}>
                      {solution.status || "-"}
                    </span>
                  </div>

                  <div style={styles.solutionMeta}>
                    <span>
                      <strong>Algoritmo:</strong> {solution.algorithm_used || "-"}
                    </span>
                    <span>
                      <strong>Partição:</strong> {solution.partition_type || "-"}
                    </span>
                    <span>
                      <strong>Reuse:</strong> {solution.reuse_solution ? "Sim" : "Não"}
                    </span>
                  </div>

                  <div style={styles.solutionFooter}>
                    <span>
                      <strong>Criada em:</strong>{" "}
                      {solution.created_at
                        ? new Date(solution.created_at).toLocaleString("pt-PT")
                        : "-"}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function InfoCard({ label, value }) {
  return (
    <div style={styles.infoCard}>
      <span style={styles.infoLabel}>{label}</span>
      <span style={styles.infoValue}>{formatValue(value)}</span>
    </div>
  );
}

function EditableTile({
  tileId,
  label,
  value,
  onClick,
  hoveredTile,
  setHoveredTile,
  children,
  fullWidth = false,
  largeEditBadge = false,
}) {
  const isHovered = hoveredTile === tileId;

  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={() => setHoveredTile(tileId)}
      onMouseLeave={() => setHoveredTile(null)}
      style={{
        ...styles.editableTile,
        ...(fullWidth ? styles.editableTileFullWidth : {}),
        ...(isHovered ? styles.editableTileHover : {}),
      }}
    >
      <span
        style={{
          ...styles.editBadge,
          ...(largeEditBadge ? styles.editBadgeLarge : {}),
          ...(isHovered ? styles.editBadgeVisible : {}),
        }}
      >
        Editar
      </span>

      <span
        style={{
          ...styles.infoLabel,
          ...(largeEditBadge ? styles.infoLabelWithLargeBadge : {}),
        }}
      >
        {label}
      </span>
      <span style={styles.infoValue}>{formatValue(value)}</span>

      {children ? <div style={styles.tileContent}>{children}</div> : null}
    </button>
  );
}

function formatValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  if (typeof value === "boolean") {
    return value ? "Sim" : "Não";
  }

  if (typeof value === "object") {
    return JSON.stringify(value);
  }

  return String(value);
}

function hasContent(value) {
  if (!value) {
    return false;
  }

  if (Array.isArray(value)) {
    return value.length > 0;
  }

  if (typeof value === "object") {
    return Object.keys(value).length > 0;
  }

  return true;
}

const styles = {
  page: {
    minHeight: "100vh",
    backgroundColor: "#f8fafc",
    padding: "32px",
  },
  container: {
    maxWidth: "1200px",
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
    color: "#101828",
  },
  description: {
    margin: 0,
    marginBottom: "24px",
    maxWidth: "780px",
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
  message: {
    fontSize: "16px",
    color: "#475467",
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
  summaryBar: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
    marginBottom: "20px",
  },
  summaryItem: {
    padding: "10px 14px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "14px",
    fontWeight: 600,
  },
  sectionCard: {
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    border: "1px solid #eaecf0",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
    padding: "24px",
    marginBottom: "24px",
  },
  sectionTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "22px",
    fontWeight: 700,
    color: "#101828",
  },
  sectionDescription: {
    margin: 0,
    marginBottom: "18px",
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  infoGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
    gap: "16px",
  },
  infoCard: {
    padding: "16px",
    border: "1px solid #eaecf0",
    borderRadius: "14px",
    backgroundColor: "#f9fafb",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  editableTile: {
    position: "relative",
    textAlign: "left",
    padding: "16px",
    border: "1px solid #eaecf0",
    borderRadius: "14px",
    backgroundColor: "#f9fafb",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    cursor: "pointer",
    transition: "all 0.18s ease",
  },
  editableTileFullWidth: {
    width: "100%",
    minHeight: "145px",
  },
  editableTileHover: {
    border: "1px solid #b2ddff",
    backgroundColor: "#f5faff",
    boxShadow: "0 8px 20px rgba(16, 24, 40, 0.08)",
    transform: "translateY(-1px)",
  },
  editBadge: {
    position: "absolute",
    top: "12px",
    right: "12px",
    padding: "4px 8px",
    borderRadius: "999px",
    backgroundColor: "#eff8ff",
    color: "#175cd3",
    fontSize: "14px",
    fontWeight: 700,
    opacity: 0,
    pointerEvents: "none",
    transition: "opacity 0.18s ease",
  },
  editBadgeLarge: {
    padding: "7px 12px",
    fontSize: "17px",
    fontWeight: 700,
    letterSpacing: "0.01em",
    boxShadow: "0 2px 8px rgba(23, 92, 211, 0.12)",
  },
  editBadgeVisible: {
    opacity: 1,
  },
  infoLabel: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#667085",
    paddingRight: "52px",
  },
  infoLabelWithLargeBadge: {
    paddingRight: "72px",
  },
  infoValue: {
    fontSize: "16px",
    fontWeight: 700,
    color: "#101828",
    wordBreak: "break-word",
  },
  tileContent: {
    marginTop: "10px",
  },
  actions: {
    display: "flex",
    justifyContent: "space-between",
    gap: "16px",
    flexWrap: "wrap",
    marginBottom: "24px",
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
  twoColumnSection: {
    marginTop: "8px",
    display: "grid",
    gridTemplateColumns: "1fr",
    gap: "16px",
  },
  tagList: {
    listStyle: "none",
    padding: 0,
    margin: 0,
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
  },
  tag: {
    display: "inline-flex",
    alignItems: "center",
    padding: "6px 10px",
    borderRadius: "999px",
    backgroundColor: "#eef4ff",
    border: "1px solid #c7d7fe",
    color: "#3538cd",
    fontSize: "13px",
    fontWeight: 600,
  },
  emptyText: {
    margin: 0,
    fontSize: "14px",
    color: "#667085",
  },
  warningBox: {
    marginTop: "14px",
    padding: "14px 16px",
    borderRadius: "12px",
    backgroundColor: "#fffaeb",
    border: "1px solid #fedf89",
  },
  warningTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#b54708",
  },
  warningText: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#93370d",
  },
  solutionList: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    gap: "16px",
  },
  solutionCard: {
    textAlign: "left",
    padding: "18px",
    borderRadius: "14px",
    border: "1px solid #eaecf0",
    backgroundColor: "#ffffff",
    cursor: "pointer",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
    display: "flex",
    flexDirection: "column",
    gap: "14px",
  },
  solutionCardTop: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "12px",
    flexWrap: "wrap",
  },
  solutionTitle: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#101828",
  },
  solutionStatus: {
    display: "inline-flex",
    alignItems: "center",
    padding: "6px 10px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "13px",
    fontWeight: 700,
  },
  solutionMeta: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
    color: "#475467",
    fontSize: "14px",
    lineHeight: 1.5,
  },
  solutionFooter: {
    fontSize: "13px",
    color: "#667085",
  },
};