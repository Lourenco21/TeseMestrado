import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  requestProblemAlgorithms,
  executeProblemWithAlgorithm,
} from "../../services/problemsApi";

const RESOLUTION_OPTIONS = [
  {
    value: "semester",
    label: "Por semestre",
    description:
      "Tende a exigir mais tempo de execução, mas permite procurar soluções mais globais e potencialmente de maior qualidade.",
  },
  {
    value: "week",
    label: "Por semana",
    description:
      "Reduz o tempo de execução face ao semestre e mantém um bom equilíbrio entre qualidade da solução e dimensão do problema.",
  },
  {
    value: "day",
    label: "Por dia",
    description:
      "Normalmente executa mais rápido, mas pode perder alguma qualidade global por resolver o problema de forma mais fragmentada.",
  },
  {
    value: "start_half_hour",
    label: "Por cada meia hora de começo",
    description:
      "É a divisão mais fina, com execução potencialmente mais rápida por bloco, mas com maior risco de sacrificar consistência e qualidade global.",
  },
];


function extractAlgorithms(result) {
  const candidates = [
    result?.algorithms,
    result?.recommended_algorithms,
    result?.java_response?.algorithms,
    result?.java_response?.recommended_algorithms,
  ];

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate;
    }
  }

  return [];
}

function formatAlgorithmParameters(parameters) {
  if (!parameters || typeof parameters !== "object" || Array.isArray(parameters)) {
    return [];
  }

  return Object.entries(parameters);
}

function extractKeyPoints(algorithm) {
  const candidates = [
    algorithm?.key_points,
    algorithm?.keyPoints,
    algorithm?.points,
  ];

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate;
    }
  }

  return [];
}

function extractAlgorithmName(algorithm) {
  return algorithm?.name || algorithm?.algorithm_name || "";
}

function CollapsibleSection({
  title,
  subtitle,
  badge,
  isCollapsed,
  onToggle,
  children,
}) {
  return (
    <div style={styles.collapsibleCard}>
      <button
        type="button"
        onClick={onToggle}
        style={styles.collapsibleHeader}
        aria-expanded={!isCollapsed}
      >
        <div style={styles.collapsibleHeaderLeft}>
          <div>
            <h2 style={styles.collapsibleTitle}>{title}</h2>
            {subtitle ? (
              <p style={styles.collapsibleSubtitle}>{subtitle}</p>
            ) : null}
          </div>
        </div>

        <div style={styles.collapsibleHeaderRight}>
          {badge ? <span style={styles.collapsibleBadge}>{badge}</span> : null}
          <span style={styles.chevron}>{isCollapsed ? "◂" : "▾"}</span>
        </div>
      </button>

      {!isCollapsed ? <div style={styles.collapsibleBody}>{children}</div> : null}
    </div>
  );
}

export default function ProblemSendToJavaPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [sending, setSending] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [localError, setLocalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [executionMessage, setExecutionMessage] = useState("");
  const [responseData, setResponseData] = useState(null);
  const [executionResponseData, setExecutionResponseData] = useState(null);
  const [resolutionScope] = useState("");
  const [repeatedInstanceStrategy] = useState("");
  const [selectedAlgorithmIndex, setSelectedAlgorithmIndex] = useState(null);
  const [collapsedSections, setCollapsedSections] = useState({
    requestConfig: false,
    algorithms: false,
    execution: false,
  });
  const [expandedAlgorithms, setExpandedAlgorithms] = useState({});

  const requiresRepeatedStrategy = useMemo(() => {
    return ["week", "day", "start_half_hour"].includes(resolutionScope);
  }, [resolutionScope]);

  const recommendedAlgorithms = useMemo(() => {
    return extractAlgorithms(responseData);
  }, [responseData]);

  const manuallySelectedAlgorithm = useMemo(() => {
    if (
      selectedAlgorithmIndex === null ||
      selectedAlgorithmIndex < 0 ||
      selectedAlgorithmIndex >= recommendedAlgorithms.length
    ) {
      return null;
    }

    return recommendedAlgorithms[selectedAlgorithmIndex];
  }, [recommendedAlgorithms, selectedAlgorithmIndex]);

  const defaultRecommendedAlgorithm = useMemo(() => {
    return recommendedAlgorithms.length > 0 ? recommendedAlgorithms[0] : null;
  }, [recommendedAlgorithms]);

  const effectiveSelectedAlgorithm = useMemo(() => {
    return manuallySelectedAlgorithm || defaultRecommendedAlgorithm;
  }, [manuallySelectedAlgorithm, defaultRecommendedAlgorithm]);

  const effectiveSelectedAlgorithmName = useMemo(() => {
    return extractAlgorithmName(effectiveSelectedAlgorithm);
  }, [effectiveSelectedAlgorithm]);

  function toggleSection(sectionKey) {
    setCollapsedSections((prev) => ({
      ...prev,
      [sectionKey]: !prev[sectionKey],
    }));
  }
  function toggleSelectedAlgorithm(index) {
    setSelectedAlgorithmIndex((prev) => (prev === index ? null : index));
    setLocalError("");
    setExecutionMessage("");
    setExecutionResponseData(null);
  }

  async function handleSend() {

    try {
      setSending(true);
      setLocalError("");
      setSuccessMessage("");
      setExecutionMessage("");
      setExecutionResponseData(null);
      setSelectedAlgorithmIndex(null);
      setExpandedAlgorithms({});
      setResponseData(null);

      const data = await requestProblemAlgorithms(id);

      setSuccessMessage("Recomendação de algoritmos recebida com sucesso.");
      setResponseData(data);
      setCollapsedSections((prev) => ({
        ...prev,
        algorithms: false,
        execution: false,
      }));
    } catch (err) {
      console.error("Erro ao enviar para Java:", err);
      setLocalError(
        err.message || "Não foi possível obter a recomendação de algoritmos."
      );
    } finally {
      setSending(false);
    }
  }

  async function handleExecute() {
    if (!resolutionScope) {
      setLocalError("Selecione primeiro o nível de resolução.");
      return;
    }

    if (requiresRepeatedStrategy && !repeatedInstanceStrategy) {
      setLocalError(
        "Indique o tratamento das instâncias equivalentes antes de executar."
      );
      return;
    }

    if (!effectiveSelectedAlgorithmName) {
      setLocalError("Não foi possível determinar um algoritmo para executar.");
      return;
    }

    try {
      setExecuting(true);
      setLocalError("");
      setExecutionMessage("");
      setExecutionResponseData(null);

      const data = await executeProblemWithAlgorithm(id, {
        resolution_scope: resolutionScope,
        repeated_instance_strategy: requiresRepeatedStrategy
          ? repeatedInstanceStrategy
          : null,
        selected_algorithm: effectiveSelectedAlgorithmName,
      });
      console.log(data)
      setExecutionMessage(
        data?.solution?.id
          ? `Execução concluída e solução #${data.solution.id} guardada com sucesso.`
          : "Execução enviada com sucesso para o backend Java."
      );
      setExecutionResponseData(data);
      setCollapsedSections((prev) => ({
        ...prev,
        execution: false,
      }));
    } catch (err) {
      console.error("Erro ao executar problema:", err);
      setLocalError(
        err.message || "Não foi possível enviar a execução do problema."
      );
    } finally {
      setExecuting(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Execução</p>
        <h1 style={styles.title}>Execução do problema</h1>
        <p style={styles.description}>
          Configure a forma de resolução do problema e selecione depois o algoritmo a executar.
        </p>



          <CollapsibleSection
            title="Algoritmos recomendados"
            subtitle="Lista de candidatos sugeridos."
            badge={`${recommendedAlgorithms.length} encontrados`}
            isCollapsed={collapsedSections.algorithms}
            onToggle={() => toggleSection("algorithms")}
          >
            {recommendedAlgorithms.length > 0 ? (
              <div style={styles.algorithmGrid}>
                {recommendedAlgorithms.map((algorithm, index) => {
                  const keyPoints = extractKeyPoints(algorithm);
                  const parameters = formatAlgorithmParameters(
                    algorithm.parameters || algorithm.configuration
                  );
                  const expanded = !!expandedAlgorithms[index];
                  const previewPoints = keyPoints.slice(0, 4);
                  const hiddenCount = Math.max(keyPoints.length - 4, 0);
                  const algorithmName = extractAlgorithmName(algorithm);
                  const isManuallySelected = selectedAlgorithmIndex === index;
                  const isDefaultRecommended =
                    selectedAlgorithmIndex === null && index === 0;

                  return (
                    <div
                      key={`${algorithmName || "algorithm"}-${index}`}
                      style={{
                        ...styles.algorithmCompactCard,
                        ...(isManuallySelected
                          ? styles.algorithmCompactCardSelected
                          : {}),
                      }}
                    >
                      <div style={styles.algorithmCompactHeader}>
                        <div style={styles.algorithmCompactTitleWrap}>
                          <span style={styles.rankBadge}>#{index + 1}</span>
                          <h3 style={styles.algorithmCompactName}>
                            {algorithmName || "Sem nome"}
                          </h3>
                          {isDefaultRecommended ? (
                            <span style={styles.recommendedBadge}>
                              Recomendado
                            </span>
                          ) : null}
                        </div>

                        <div style={styles.algorithmHeaderActions}>
                          <button
                            type="button"
                            onClick={() => toggleSelectedAlgorithm(index)}
                            style={{
                              ...styles.selectButton,
                              ...(isManuallySelected
                                ? styles.selectButtonActive
                                : {}),
                            }}
                          >
                            {isManuallySelected
                              ? "Remover seleção"
                              : "Escolher"}
                          </button>

                          
                        </div>
                      </div>

                      <div style={styles.tagsRow}>
                        {previewPoints.map((item, pointIndex) => (
                          <span
                            key={`${algorithmName || "algorithm"}-preview-${pointIndex}`}
                            style={styles.keyTag}
                          >
                            {typeof item === "string"
                              ? item
                              : item?.point || "Ponto"}
                          </span>
                        ))}

                        {!expanded && hiddenCount > 0 ? (
                          <span style={styles.moreTag}>+{hiddenCount}</span>
                        ) : null}
                      </div>

                      {expanded ? (
                        <div style={styles.algorithmExpandedBlock}>
                          {keyPoints.length > 0 ? (
                            <div style={styles.expandedSection}>
                              <p style={styles.expandedSectionTitle}>Pontos-chave</p>
                              <div style={styles.keyPointsListCompact}>
                                {keyPoints.map((item, pointIndex) => (
                                  <div
                                    key={`${algorithmName || "algorithm"}-point-${pointIndex}`}
                                    style={styles.keyPointCompactItem}
                                  >
                                    <span style={styles.keyPointBullet}>•</span>
                                    <span style={styles.keyPointText}>
                                      {typeof item === "string"
                                        ? item
                                        : item?.point || "Ponto não disponível"}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ) : null}

                          {parameters.length > 0 ? (
                            <div style={styles.expandedSection}>
                              <p style={styles.expandedSectionTitle}>
                                Parâmetros sugeridos
                              </p>
                              <div style={styles.parametersGrid}>
                                {parameters.map(([key, value]) => (
                                  <div key={key} style={styles.parameterItem}>
                                    <span style={styles.parameterKey}>{key}</span>
                                    <span style={styles.parameterValue}>
                                      {typeof value === "object"
                                        ? JSON.stringify(value)
                                        : String(value)}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ) : null}
                        </div>
                      ) : null}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={styles.emptyResults}>
                <p style={styles.emptyResultsTitle}>
                  Não foi encontrada uma lista de algoritmos na resposta.
                </p>
                <p style={styles.emptyResultsText}>
                  O pedido foi concluído, mas a resposta ainda não veio no formato
                  esperado pela interface.
                </p>
              </div>
            )}
            <div style={styles.actions}>
              <button
                type="button"
                onClick={() => navigate(`/problems/${id}/detail`)}
                style={styles.secondaryButton}
              >
                Voltar
              </button>

              <button
                type="button"
                onClick={handleSend}
                disabled={sending}
                style={{
                  ...styles.primaryButton,
                  ...(sending ? styles.primaryButtonDisabled : {}),
                }}
              >
                {sending ? "A analisar..." : "Obter recomendação"}
              </button>
            </div>

          </CollapsibleSection>

        {responseData && recommendedAlgorithms.length > 0 ? (
          <CollapsibleSection
            title="Execução"
            subtitle="Seleciona um dos algoritmos recomendados e envia a execução final."
            badge={
              effectiveSelectedAlgorithmName
                ? effectiveSelectedAlgorithmName
                : "Sem algoritmo disponível"
            }
            isCollapsed={collapsedSections.execution}
            onToggle={() => toggleSection("execution")}
          >
            <div style={styles.executionPanel}>
              <div style={styles.executionSummaryCard}>
                <p style={styles.executionSummaryLabel}>Algoritmo a executar</p>
                <p style={styles.executionSummaryValue}>
                  {effectiveSelectedAlgorithmName || "Ainda não disponível"}
                </p>
                <p style={styles.executionSummaryHint}>
                  Se não escolheres manualmente um algoritmo, será usado
                  automaticamente o primeiro algoritmo recomendado.
                </p>
              </div>

              <div style={styles.actions}>
                <button
                  type="button"
                  onClick={() => navigate(`/problems/${id}/detail`)}
                  style={styles.secondaryButton}
                >
                  Voltar ao problema
                </button>

                <button
                  type="button"
                  onClick={handleExecute}
                  disabled={executing || !effectiveSelectedAlgorithmName}
                  style={{
                    ...styles.executeButton,
                    ...((executing || !effectiveSelectedAlgorithmName)
                      ? styles.primaryButtonDisabled
                      : {}),
                  }}
                >
                  {executing ? "A executar..." : "Executar algoritmo"}
                </button>
              </div>

              {executionMessage ? (
                <p style={styles.success}>{executionMessage}</p>
              ) : null}

              {executionResponseData ? (
                <div style={styles.executionResponseBox}>
                  <p style={styles.executionResponseTitle}>Resposta da execução</p>
                  <pre style={styles.responsePre}>
                    {/*{JSON.stringify(executionResponseData, null, 2)}*/}
                  </pre>
                </div>
              ) : null}
            </div>
          </CollapsibleSection>
        ) : null}
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
    maxWidth: "1080px",
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
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
    maxWidth: "760px",
  },
  collapsibleCard: {
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    padding: "24px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.06)",
    marginBottom: "24px",
  },
  collapsibleHeader: {
    width: "100%",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "16px",
    background: "transparent",
    border: "none",
    padding: 0,
    cursor: "pointer",
    textAlign: "left",
  },
  collapsibleHeaderLeft: {
    flex: 1,
    minWidth: 0,
  },
  collapsibleHeaderRight: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    flexShrink: 0,
  },
  collapsibleTitle: {
    margin: 0,
    fontSize: "22px",
    fontWeight: 700,
    color: "#101828",
  },
  collapsibleSubtitle: {
    margin: "6px 0 0 0",
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
    maxWidth: "720px",
  },
  collapsibleBadge: {
    display: "inline-flex",
    alignItems: "center",
    padding: "8px 12px",
    borderRadius: "999px",
    backgroundColor: "#eef2ff",
    color: "#4338ca",
    fontSize: "13px",
    fontWeight: 700,
  },
  chevron: {
    width: "32px",
    height: "32px",
    borderRadius: "999px",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f2f4f7",
    color: "#475467",
    fontSize: "18px",
    fontWeight: 700,
    lineHeight: 1,
  },
  collapsibleBody: {
    marginTop: "20px",
    paddingTop: "20px",
    borderTop: "1px solid #eaecf0",
  },
  section: {
    marginBottom: "24px",
  },
  sectionTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "16px",
    fontWeight: 700,
    color: "#101828",
  },
  optionGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
    gap: "12px",
  },
  optionCard: {
    padding: "16px",
    borderRadius: "14px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    textAlign: "left",
    cursor: "pointer",
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  optionCardActive: {
    borderColor: "#175cd3",
    backgroundColor: "#eff6ff",
    boxShadow: "0 0 0 1px #175cd3 inset",
  },
  optionTitle: {
    fontSize: "15px",
    fontWeight: 700,
    color: "#101828",
  },
  optionDescription: {
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  actions: {
    display: "flex",
    gap: "12px",
    justifyContent: "space-between",
    flexWrap: "wrap",
    marginTop: "20px",
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
  primaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "none",
    backgroundColor: "#175cd3",
    color: "#ffffff",
    fontSize: "15px",
    fontWeight: 700,
    cursor: "pointer",
  },
  executeButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "none",
    backgroundColor: "#067647",
    color: "#ffffff",
    fontSize: "15px",
    fontWeight: 700,
    cursor: "pointer",
  },
  primaryButtonDisabled: {
    opacity: 0.7,
    cursor: "not-allowed",
  },
  error: {
    marginTop: "16px",
    marginBottom: 0,
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#fef3f2",
    border: "1px solid #fecdca",
    color: "#b42318",
    fontSize: "14px",
  },
  success: {
    marginTop: "16px",
    marginBottom: 0,
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#ecfdf3",
    border: "1px solid #abefc6",
    color: "#067647",
    fontSize: "14px",
  },
  algorithmGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    gap: "14px",
  },
  algorithmCompactCard: {
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    padding: "16px",
    boxShadow: "0 4px 14px rgba(16, 24, 40, 0.04)",
    display: "flex",
    flexDirection: "column",
    gap: "14px",
  },
  algorithmCompactCardSelected: {
    borderColor: "#175cd3",
    backgroundColor: "#f5f9ff",
    boxShadow: "0 0 0 1px #175cd3 inset",
  },
  algorithmCompactHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "12px",
  },
  algorithmCompactTitleWrap: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    minWidth: 0,
    flexWrap: "wrap",
  },
  algorithmCompactName: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#101828",
    lineHeight: 1.3,
  },
  algorithmHeaderActions: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    flexWrap: "wrap",
    justifyContent: "flex-end",
  },
  detailsButton: {
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#344054",
    borderRadius: "999px",
    padding: "8px 12px",
    fontSize: "13px",
    fontWeight: 700,
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  selectButton: {
    border: "1px solid #175cd3",
    backgroundColor: "#ffffff",
    color: "#175cd3",
    borderRadius: "999px",
    padding: "8px 12px",
    fontSize: "13px",
    fontWeight: 700,
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  selectButtonActive: {
    backgroundColor: "#175cd3",
    color: "#ffffff",
  },
  recommendedBadge: {
    display: "inline-flex",
    alignItems: "center",
    padding: "6px 10px",
    borderRadius: "999px",
    backgroundColor: "#eef2ff",
    color: "#4338ca",
    fontSize: "12px",
    fontWeight: 700,
    whiteSpace: "nowrap",
  },
  rankBadge: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    minWidth: "34px",
    height: "34px",
    padding: "0 10px",
    borderRadius: "999px",
    backgroundColor: "#dbeafe",
    color: "#1d4ed8",
    fontSize: "13px",
    fontWeight: 700,
    flexShrink: 0,
  },
  tagsRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
  },
  keyTag: {
    display: "inline-flex",
    alignItems: "center",
    padding: "7px 10px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "12px",
    fontWeight: 600,
    lineHeight: 1.3,
  },
  moreTag: {
    display: "inline-flex",
    alignItems: "center",
    padding: "7px 10px",
    borderRadius: "999px",
    backgroundColor: "#eef2ff",
    color: "#4338ca",
    fontSize: "12px",
    fontWeight: 700,
    lineHeight: 1.3,
  },
  algorithmExpandedBlock: {
    paddingTop: "14px",
    borderTop: "1px solid #eaecf0",
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  expandedSection: {
    display: "flex",
    flexDirection: "column",
    gap: "10px",
  },
  expandedSectionTitle: {
    margin: 0,
    fontSize: "13px",
    fontWeight: 700,
    color: "#101828",
    textTransform: "uppercase",
    letterSpacing: "0.03em",
  },
  keyPointsListCompact: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  keyPointCompactItem: {
    display: "flex",
    alignItems: "flex-start",
    gap: "8px",
  },
  keyPointBullet: {
    color: "#175cd3",
    fontSize: "16px",
    lineHeight: 1.4,
    fontWeight: 700,
  },
  keyPointText: {
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#475467",
  },
  parametersGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
    gap: "10px",
  },
  parameterItem: {
    padding: "12px",
    borderRadius: "12px",
    backgroundColor: "#f8fafc",
    border: "1px solid #eaecf0",
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  parameterKey: {
    fontSize: "12px",
    fontWeight: 700,
    textTransform: "uppercase",
    letterSpacing: "0.04em",
    color: "#667085",
  },
  parameterValue: {
    fontSize: "14px",
    color: "#101828",
    wordBreak: "break-word",
  },
  emptyResults: {
    backgroundColor: "#ffffff",
    border: "1px dashed #d0d5dd",
    borderRadius: "16px",
    padding: "24px",
  },
  emptyResultsTitle: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "16px",
    fontWeight: 700,
    color: "#101828",
  },
  emptyResultsText: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  executionPanel: {
    display: "flex",
    flexDirection: "column",
    gap: "18px",
  },
  executionSummaryCard: {
    padding: "18px",
    borderRadius: "14px",
    backgroundColor: "#f8fafc",
    border: "1px solid #eaecf0",
  },
  executionSummaryLabel: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "13px",
    fontWeight: 700,
    color: "#667085",
    textTransform: "uppercase",
    letterSpacing: "0.04em",
  },
  executionSummaryValue: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "20px",
    fontWeight: 700,
    color: "#101828",
  },
  executionSummaryHint: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#475467",
  },
  executionResponseBox: {
    padding: "16px",
    borderRadius: "14px",
    backgroundColor: "#0f172a",
    color: "#e2e8f0",
    overflowX: "auto",
  },
  executionResponseTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#cbd5e1",
  },
  responsePre: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.6,
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
    fontFamily:
      "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, monospace",
  },
};