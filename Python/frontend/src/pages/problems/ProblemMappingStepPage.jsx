import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemMappingSuggestions } from "../../services/problemsApi";

export default function ProblemMappingStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [schema, setSchema] = useState(null);
  const [matches, setMatches] = useState([]);
  const [selectedMappings, setSelectedMappings] = useState({});
  const [manualChanges, setManualChanges] = useState({});
  const [pageLoading, setPageLoading] = useState(true);
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    async function loadData() {
      try {
        setPageLoading(true);
        setLocalError("");

        const draft = await loadDraft(id);
        const result = await getProblemMappingSuggestions(id);

        setSchema(result.schema);
        setMatches(result.matches || []);
        setSelectedMappings(result.selected_mappings || {});
        setManualChanges({});
      } catch (err) {
        console.error("Erro ao carregar mapping:", err);
        setLocalError(err.message || "Não foi possível carregar o mapping.");
      } finally {
        setPageLoading(false);
      }
    }

    loadData();
  }, [id]);

  const sourceColumnUsage = useMemo(() => {
    const usage = {};

    Object.entries(selectedMappings).forEach(([targetField, sourceColumn]) => {
      if (!sourceColumn) return;
      if (!usage[sourceColumn]) usage[sourceColumn] = [];
      usage[sourceColumn].push(targetField);
    });

    return usage;
  }, [selectedMappings]);

  const requiredFields = useMemo(() => {
    return (schema?.fields || []).filter((field) => field.required);
  }, [schema]);

  const missingRequiredFields = useMemo(() => {
    return requiredFields.filter(
      (field) => !selectedMappings[field.key]
    );
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
    if (confidence >= 0.55) return styles.confidenceWeak;
    return styles.confidenceLow;
  }

  async function handleContinue() {
    if (missingRequiredFields.length > 0 || duplicatedFields.length > 0) {
      setLocalError("Corrige os campos obrigatórios e as duplicações antes de avançar.");
      return;
    }

    try {
      await saveDraft({
        status: "mapping_completed",
        current_step: 5,
        wizard_data: {
          ...(problemDraft?.wizard_data || {}),
          mapping: selectedMappings,
        },
      });

      navigate(`/problems/${id}/objectives`);
    } catch (err) {
      console.error("Erro ao guardar mapping:", err);
      setLocalError(err.message || "Não foi possível guardar o mapping.");
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/upload`);
  }

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
        <p style={styles.step}>Step 4 de 7</p>
        <h1 style={styles.title}>Confirmar mapping do ficheiro</h1>
        <p style={styles.description}>
          Associa cada variável do schema à coluna correspondente no ficheiro carregado.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
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
                <div style={styles.headerCell}>Confiança</div>
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
                      ...(isRequired && !selectedMappings[field.key] ? styles.rowMissing : {}),
                      ...(isDuplicate ? styles.rowDuplicate : {}),
                    }}
                  >
                    <div style={styles.targetCell}>
                      <div style={styles.fieldLabelRow}>
                        <span>{field.label}</span>
                        {isRequired ? <span style={styles.requiredBadge}>Obrigatório</span> : null}
                      </div>
                      {field.description ? (
                        <p style={styles.fieldDescription}>{field.description}</p>
                      ) : null}
                    </div>

                    <div style={styles.selectCell}>
                      <select
                        value={selectedMappings[field.key] || ""}
                        onChange={(e) => handleMappingChange(field.key, e.target.value)}
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
                      {isManual ? (
                        <span style={styles.manualLabel}>Revisto pelo utilizador</span>
                      ) : (
                        <>
                          <span style={{ ...styles.confidenceValue, ...getConfidenceClass(confidence) }}>
                            {(confidence * 100).toFixed(0)}%
                          </span>
                          <span style={styles.matchType}>
                            {match?.match_type === "strong"
                              ? "Sugestão forte"
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
          <button type="button" onClick={handleBack} style={styles.secondaryButton}>
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
            style={styles.primaryButton}
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
    fontSize: "16px",
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
    color: "#b54708",
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
};