import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {requestProblemAlgorithms} from "../../services/problemsApi";

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

function getRepeatedInstanceOptions(resolutionScope) {
  switch (resolutionScope) {
    case "week":
      return [
        {
          value: "reuse_solution",
          label: "Guardar a mesma solução para semanas iguais",
          description:
            "Reutiliza a mesma solução sempre que forem detetadas semanas equivalentes.",
        },
        {
          value: "generate_new",
          label: "Gerar nova solução para cada semana",
          description:
            "Volta a gerar a solução em cada semana, mesmo quando o padrão é idêntico.",
        },
      ];

    case "day":
      return [
        {
          value: "reuse_solution",
          label: "Guardar a mesma solução para dias iguais",
          description:
            "Reutiliza a mesma solução sempre que forem detetados dias equivalentes.",
        },
        {
          value: "generate_new",
          label: "Gerar nova solução para cada dia",
          description:
            "Volta a gerar a solução em cada dia, mesmo quando o padrão é idêntico.",
        },
      ];

    case "start_half_hour":
      return [
        {
          value: "reuse_solution",
          label: "Guardar a mesma solução para blocos iguais",
          description:
            "Reutiliza a mesma solução para blocos de meia hora com o mesmo padrão.",
        },
        {
          value: "generate_new",
          label: "Gerar nova solução para cada bloco",
          description:
            "Volta a gerar a solução para cada bloco de meia hora, mesmo quando o padrão é idêntico.",
        },
      ];

    default:
      return [];
  }
}

function getRepeatedStrategySectionTitle(resolutionScope) {
  switch (resolutionScope) {
    case "week":
      return "Tratamento de semanas iguais";
    case "day":
      return "Tratamento de dias iguais";
    case "start_half_hour":
      return "Tratamento de blocos de meia hora iguais";
    default:
      return "";
  }
}

function extractAlgorithms(result) {
  const javaResponse = result?.java_response || {};
  const candidates = [
    javaResponse?.recommended_algorithms,
    javaResponse?.algorithms,
    result?.recommended_algorithms,
    result?.algorithms,
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

export default function ProblemSendToJavaPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [sending, setSending] = useState(false);
  const [localError, setLocalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [responseData, setResponseData] = useState(null);
  const [resolutionScope, setResolutionScope] = useState("");
  const [repeatedInstanceStrategy, setRepeatedInstanceStrategy] = useState("");

  const requiresRepeatedStrategy = useMemo(() => {
    return ["week", "day", "start_half_hour"].includes(resolutionScope);
  }, [resolutionScope]);

  const repeatedInstanceOptions = useMemo(() => {
    return getRepeatedInstanceOptions(resolutionScope);
  }, [resolutionScope]);

  const repeatedStrategyTitle = useMemo(() => {
    return getRepeatedStrategySectionTitle(resolutionScope);
  }, [resolutionScope]);

  const recommendedAlgorithms = useMemo(() => {
    return extractAlgorithms(responseData);
  }, [responseData]);

  async function handleSend() {
    if (!resolutionScope) {
      setLocalError("Seleciona primeiro como queres tentar resolver o problema.");
      return;
    }

    if (requiresRepeatedStrategy && !repeatedInstanceStrategy) {
      setLocalError(
        "Indica o que fazer quando existirem semanas, dias ou blocos equivalentes."
      );
      return;
    }

    try {
      setSending(true);
      setLocalError("");
      setSuccessMessage("");
      setResponseData(null);

      const data = await requestProblemAlgorithms(id, {
        resolution_scope: resolutionScope,
        repeated_instance_strategy: requiresRepeatedStrategy
          ? repeatedInstanceStrategy
          : null,
      });
      console.log(data.java_response.algorithms);
      console.log(data.java_response.justification);

      setSuccessMessage("Recomendação de algoritmos recebida com sucesso.");
      setResponseData(data);
    } catch (err) {
      console.error("Erro ao enviar para Java:", err);
      setLocalError(
        err.message || "Não foi possível obter a recomendação de algoritmos."
      );
    } finally {
      setSending(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Execução</p>
        <h1 style={styles.title}>Recomendar algoritmos</h1>
        <p style={styles.description}>
          Configura a forma de execução do problema e pede ao backend Java uma
          lista dos algoritmos mais adequados para o resolver.
        </p>

        <div style={styles.card}>
          <div style={styles.section}>
            <p style={styles.sectionTitle}>Nível de resolução</p>
            <div style={styles.optionGrid}>
              {RESOLUTION_OPTIONS.map((option) => {
                const selected = resolutionScope === option.value;

                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => {
                      setResolutionScope(option.value);
                      setLocalError("");
                      setSuccessMessage("");
                      setResponseData(null);

                      if (option.value === "semester") {
                        setRepeatedInstanceStrategy("");
                      }
                    }}
                    style={{
                      ...styles.optionCard,
                      ...(selected ? styles.optionCardActive : {}),
                    }}
                  >
                    <span style={styles.optionTitle}>{option.label}</span>
                    <span style={styles.optionDescription}>
                      {option.description}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {requiresRepeatedStrategy ? (
            <div style={styles.section}>
              <p style={styles.sectionTitle}>{repeatedStrategyTitle}</p>
              <div style={styles.optionGrid}>
                {repeatedInstanceOptions.map((option) => {
                  const selected = repeatedInstanceStrategy === option.value;

                  return (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => {
                        setRepeatedInstanceStrategy(option.value);
                        setLocalError("");
                        setSuccessMessage("");
                        setResponseData(null);
                      }}
                      style={{
                        ...styles.optionCard,
                        ...(selected ? styles.optionCardActive : {}),
                      }}
                    >
                      <span style={styles.optionTitle}>{option.label}</span>
                      <span style={styles.optionDescription}>
                        {option.description}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : null}

          <div style={styles.actions}>
            <button
              type="button"
              onClick={() => navigate(`/problems/${id}`)}
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

          {localError ? <p style={styles.error}>{localError}</p> : null}
          {successMessage ? <p style={styles.success}>{successMessage}</p> : null}
        </div>

        {responseData ? (
          <div style={styles.resultsSection}>
            <div style={styles.resultsHeader}>
              <h2 style={styles.resultsTitle}>Algoritmos recomendados</h2>
              <span style={styles.resultsBadge}>
                {recommendedAlgorithms.length} encontrados
              </span>
            </div>

            {recommendedAlgorithms.length > 0 ? (
              <div style={styles.algorithmList}>
                {recommendedAlgorithms.map((algorithm, index) => {
                  const parameters = formatAlgorithmParameters(
                    algorithm.parameters || algorithm.configuration
                  );

                  return (
                    <div
                      key={`${algorithm.name || "algorithm"}-${index}`}
                      style={styles.algorithmCard}
                    >
                      <div style={styles.algorithmHeader}>
                        <div>
                          <h3 style={styles.algorithmName}>
                            {algorithm.name || algorithm.algorithm_name || "Sem nome"}
                          </h3>
                          <p style={styles.algorithmFamily}>
                            {algorithm.family ||
                              algorithm.algorithm_family ||
                              "Família não indicada"}
                          </p>
                        </div>

                        <span style={styles.rankBadge}>#{index + 1}</span>
                      </div>

                      <p style={styles.algorithmReason}>
                        {algorithm.reason ||
                          algorithm.justification ||
                          algorithm.algorithm_reason ||
                          "Sem justificação disponível."}
                      </p>

                      {parameters.length > 0 ? (
                        <div style={styles.parametersBlock}>
                          <p style={styles.parametersTitle}>Parâmetros sugeridos</p>
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
          </div>
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
  card: {
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    padding: "24px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.06)",
    marginBottom: "24px",
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
    marginTop: "8px",
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
  resultsSection: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  resultsHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "12px",
    flexWrap: "wrap",
  },
  resultsTitle: {
    margin: 0,
    fontSize: "28px",
    color: "#101828",
  },
  resultsBadge: {
    display: "inline-flex",
    alignItems: "center",
    padding: "8px 12px",
    borderRadius: "999px",
    backgroundColor: "#eef2ff",
    color: "#4338ca",
    fontSize: "13px",
    fontWeight: 700,
  },
  algorithmList: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  algorithmCard: {
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    padding: "20px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
  },
  algorithmHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    flexWrap: "wrap",
    marginBottom: "12px",
  },
  algorithmName: {
    margin: 0,
    fontSize: "20px",
    fontWeight: 700,
    color: "#101828",
  },
  algorithmFamily: {
    margin: 0,
    marginTop: "4px",
    fontSize: "14px",
    color: "#667085",
  },
  rankBadge: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    minWidth: "36px",
    height: "36px",
    padding: "0 12px",
    borderRadius: "999px",
    backgroundColor: "#dbeafe",
    color: "#1d4ed8",
    fontSize: "14px",
    fontWeight: 700,
  },
  algorithmReason: {
    margin: 0,
    fontSize: "15px",
    lineHeight: 1.7,
    color: "#475467",
  },
  parametersBlock: {
    marginTop: "16px",
    paddingTop: "16px",
    borderTop: "1px solid #eaecf0",
  },
  parametersTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#101828",
  },
  parametersGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
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
};