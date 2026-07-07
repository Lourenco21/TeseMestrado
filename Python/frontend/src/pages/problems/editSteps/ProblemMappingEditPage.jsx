import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getProblemDraft,
  getProblemMappingSuggestions,
  updateProblemDraft,
} from "../../../services/problemsApi";

const DRAFT_LOAD_TIMEOUT_MS = 10000;

export default function ProblemMappingStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const didLoadRef = useRef(false);

  const [pageLoading, setPageLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [retryKey, setRetryKey] = useState(0);
  const [localError, setLocalError] = useState("");

  const [problemDraft, setProblemDraft] = useState(null);
  const [schema, setSchema] = useState(null);
  const [matches, setMatches] = useState([]);
  const [selectedMappings, setSelectedMappings] = useState({});
  const [manualChanges, setManualChanges] = useState({});

  useEffect(() => {
    didLoadRef.current = false;
  }, [id, retryKey]);

  useEffect(() => {
    if (!id || didLoadRef.current) return;
    didLoadRef.current = true;

    let cancelled = false;

    async function loadData() {
      try {
        setPageLoading(true);
        setLocalError("");

        if (Number.isNaN(Number(id))) {
          throw new Error("ID do problema inválido.");
        }

        const draft = await Promise.race([
          getProblemDraft(id),
          new Promise((_, reject) =>
            setTimeout(
              () => reject(new Error("Timeout a carregar o draft.")),
              DRAFT_LOAD_TIMEOUT_MS
            )
          ),
        ]);

        if (cancelled) return;

        setProblemDraft(draft || null);

        const result = await getProblemMappingSuggestions(id);

        if (cancelled) return;

        setSchema(result?.schema || null);
        setMatches(result?.matches || []);

        const savedMapping = draft?.mapping_data?.mapping;
        const savedManualChanges = draft?.mapping_data?.manual_changes;

        if (savedMapping && Object.keys(savedMapping).length > 0) {
          setSelectedMappings(savedMapping);
          setManualChanges(savedManualChanges || {});
        } else {
          setSelectedMappings(result?.selected_mappings || {});
          setManualChanges({});
        }
      } catch (err) {
        if (cancelled) return;

        console.error("Erro ao carregar mapping:", err);
        setProblemDraft(null);
        setSchema(null);
        setMatches([]);
        setSelectedMappings({});
        setManualChanges({});
        setLocalError(err?.message || "Não foi possível carregar o mapping.");
      } finally {
        if (!cancelled) {
          setPageLoading(false);
        }
      }
    }

    loadData();

    return () => {
      cancelled = true;
    };
  }, [id, retryKey]);

  function handleRetry() {
    setRetryKey((prev) => prev + 1);
  }

  const sourceColumnUsage = useMemo(() => {
    const usage = {};

    Object.entries(selectedMappings).forEach(([targetField, sourceColumn]) => {
      if (!sourceColumn) return;
      if (!usage[sourceColumn]) usage[sourceColumn] = [];
      usage[sourceColumn].push(targetField);
    });

    return usage;
  }, [selectedMappings]);

  const isMappingSaved = useMemo(() => {
    const mapping = problemDraft?.mapping_data?.mapping;
    return !!mapping && Object.keys(mapping).length > 0;
  }, [problemDraft]);

  const requiredFields = useMemo(() => {
    return (schema?.fields || []).filter((field) => field.required);
  }, [schema]);

  const missingRequiredFields = useMemo(() => {
    return requiredFields.filter((field) => !selectedMappings[field.key]);
  }, [requiredFields, selectedMappings]);

  const duplicatedFields = useMemo(() => {
    return Object.entries(sourceColumnUsage)
      .filter(([, targetFields]) => targetFields.length > 1)
      .map(([sourceColumn]) => sourceColumn);
  }, [sourceColumnUsage]);

  function handleMappingChange(targetField, selectedColumn) {
    setSelectedMappings((prev) => ({
      ...prev,
      [targetField]: selectedColumn,
    }));

    setManualChanges((prev) => ({
      ...prev,
      [targetField]: true,
    }));
  }

  function isFieldDuplicate(targetField) {
    const sourceColumn = selectedMappings[targetField];
    if (!sourceColumn) return false;
    return (sourceColumnUsage[sourceColumn] || []).length > 1;
  }

  function getMatchByField(targetField) {
    return matches.find((match) => match.target_field === targetField);
  }

  function getConfidenceClass(confidence) {
    if (confidence >= 0.8) return styles.confidenceStrong;
    if (confidence >= 0.65) return styles.confidenceWeak;
    return styles.confidenceLow;
  }

  async function handleContinue() {
    if (missingRequiredFields.length > 0 || duplicatedFields.length > 0) {
      setLocalError("Corrige os campos obrigatórios e as duplicações antes de avançar.");
      return;
    }

    try {
      setSaving(true);
      setLocalError("");

      const payload = {
        mapping_data: {
          ...(problemDraft?.mapping_data || {}),
          mapping: selectedMappings,
        },
        room_feature_resolution: {},
      };

      const updatedDraft = await updateProblemDraft(id, payload);

      setProblemDraft(updatedDraft || problemDraft);
      navigate(`/problems/${id}/detail`);
    } catch (err) {
      console.error("Erro ao guardar mapping:", err);
      setLocalError(err?.message || "Não foi possível guardar o mapping.");
    } finally {
      setSaving(false);
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/detail`);
  }

  if (pageLoading) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A carregar problema...</p>
        </div>
      </div>
    );
  }

  if (localError && !problemDraft) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.step}>Passo 4</p>
          <h1 style={styles.title}>Confirmar mapping do ficheiro</h1>
          <p style={styles.error}>{localError}</p>

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
              onClick={handleRetry}
              style={styles.primaryButton}
            >
              Tentar novamente
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Passo 4 de 8</p>
        <h1 style={styles.title}>Confirmar mapping do ficheiro</h1>
        <p style={styles.description}>
          Associe cada variável do schema à coluna correspondente no ficheiro carregado.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {localError ? <p style={styles.error}>{localError}</p> : null}

        {pageLoading ? (
          <p style={styles.message}>A carregar sugestões de mapping...</p>
        ) : (
          <>
            <div style={styles.summaryBar}>
              <span style={styles.summaryItem}>
                Obrigatórios em falta: {missingRequiredFields.length}
              </span>
              <span style={styles.summaryItem}>
                Colunas repetidas: {duplicatedFields.length}
              </span>
            </div>

            <div style={styles.tableCard}>
              <div style={styles.tableHeader}>
                <div style={styles.headerCell}>Variável</div>
                <div style={styles.headerCell}>Coluna do ficheiro</div>
                <div style={styles.headerCell}>
                  {isMappingSaved ? "Estado" : "Confiança"}
                </div>
              </div>

              {(schema?.fields || []).map((field) => {
                const match = getMatchByField(field.key);
                const confidence = match?.confidence || 0;
                const isRequired = !!field.required;
                const isDuplicate = isFieldDuplicate(field.key);
                const isManual = !!manualChanges[field.key];

                return (
                  <div
                    key={field.key}
                    style={{
                      ...styles.row,
                      ...(isRequired && !selectedMappings[field.key]
                        ? styles.rowMissing
                        : {}),
                      ...(isDuplicate ? styles.rowDuplicate : {}),
                    }}
                  >
                    <div style={styles.targetCell}>
                      <div style={styles.fieldLabelRow}>
                        <span>{field.label}</span>
                        {isRequired ? (
                          <span style={styles.requiredBadge}>Obrigatório</span>
                        ) : null}
                      </div>

                      {field.description ? (
                        <p style={styles.fieldDescription}>{field.description}</p>
                      ) : null}
                    </div>

                    <div style={styles.selectCell}>
                      <select
                        value={selectedMappings[field.key] || ""}
                        onChange={(e) =>
                          handleMappingChange(field.key, e.target.value)
                        }
                        style={{
                          ...styles.select,
                          ...(isDuplicate ? styles.selectDuplicate : {}),
                        }}
                      >
                        <option value="">-- Selecionar coluna --</option>
                        {match?.available_source_columns?.map((column) => (
                          <option key={column} value={column}>
                            {column}
                          </option>
                        ))}
                      </select>

                      {isDuplicate ? (
                        <p style={styles.inlineError}>
                          Esta coluna está a ser usada em mais do que um campo.
                        </p>
                      ) : null}
                    </div>

                    <div style={styles.confidenceCell}>
                      {isMappingSaved ? (
                        selectedMappings[field.key] ? (
                          <span style={styles.manualLabel}>Revisto pelo utilizador</span>
                        ) : (
                          <span style={styles.manualLabel}>Nenhuma coluna selecionada</span>
                        )
                      ) : isManual ? (
                        <span style={styles.manualLabel}>Revisto pelo utilizador</span>
                      ) : (
                        <>
                          <span
                            style={{
                              ...styles.confidenceValue,
                              ...getConfidenceClass(confidence),
                            }}
                          >
                            {(confidence * 100).toFixed(0)}%
                          </span>

                          <span style={styles.matchType}>
                            {match?.match_type === "strong"
                              ? "Sugestão forte"
                              : match?.match_type === "medium"
                              ? "Sugestão mediana"
                              : match?.match_type === "weak"
                              ? "Sugestão fraca"
                              : "Sem sugestão"}
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </>
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
            disabled={
              pageLoading ||
              saving ||
              missingRequiredFields.length > 0 ||
              duplicatedFields.length > 0
            }
            style={{
              ...styles.primaryButton,
              ...(pageLoading ||
              saving ||
              missingRequiredFields.length > 0 ||
              duplicatedFields.length > 0
                ? styles.primaryButtonDisabled
                : {}),
            }}
          >
            {saving ? "A guardar..." : "Guardar"}
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
  tableCard: {
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    border: "1px solid #eaecf0",
    overflow: "hidden",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
  },
  tableHeader: {
    display: "grid",
    gridTemplateColumns: "1.2fr 1.4fr 0.9fr",
    backgroundColor: "#f9fafb",
    borderBottom: "1px solid #eaecf0",
  },
  headerCell: {
    padding: "16px 20px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#344054",
  },
  row: {
    display: "grid",
    gridTemplateColumns: "1.2fr 1.4fr 0.9fr",
    borderBottom: "1px solid #f2f4f7",
    alignItems: "stretch",
  },
  rowMissing: {
    backgroundColor: "#fffbeb",
  },
  rowDuplicate: {
    backgroundColor: "#fef3f2",
  },
  targetCell: {
    padding: "18px 20px",
  },
  fieldLabelRow: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    marginBottom: "6px",
    fontSize: "15px",
    fontWeight: 600,
    color: "#101828",
    flexWrap: "wrap",
  },
  requiredBadge: {
    padding: "3px 8px",
    borderRadius: "999px",
    backgroundColor: "#fee4e2",
    color: "#b42318",
    fontSize: "12px",
    fontWeight: 700,
  },
  fieldDescription: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.5,
    color: "#667085",
  },
  selectCell: {
    padding: "12px 20px",
  },
  select: {
    width: "100%",
    padding: "12px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    fontSize: "15px",
    backgroundColor: "#ffffff",
    color: "#101828",
  },
  selectDuplicate: {
    borderColor: "#f04438",
  },
  inlineError: {
    margin: "8px 0 0",
    fontSize: "13px",
    color: "#b42318",
  },
  confidenceCell: {
    padding: "18px 20px",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    gap: "6px",
  },
  confidenceValue: {
    fontSize: "18px",
    fontWeight: 800,
  },
  confidenceStrong: {
    color: "#067647",
  },
  confidenceWeak: {
    color: "#a38824",
  },
  confidenceLow: {
    color: "#b42318",
  },
  matchType: {
    fontSize: "13px",
    color: "#667085",
  },
  manualLabel: {
    fontSize: "13px",
    fontWeight: 700,
    color: "#175cd3",
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
    opacity: 0.6,
    cursor: "not-allowed",
  },
};