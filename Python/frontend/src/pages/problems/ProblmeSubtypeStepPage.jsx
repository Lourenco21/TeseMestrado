import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemCatalog } from "../../services/problemsApi";

export default function ProblemSubtypeStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [problemFamilies, setProblemFamilies] = useState([]);
  const [selectedSubtype, setSelectedSubtype] = useState("");
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    async function fetchData() {
      try {
        setLocalError("");

        const [draft, catalog] = await Promise.all([
          loadDraft(id),
          getProblemCatalog(),
        ]);

        setProblemFamilies(catalog.problem_families || []);
        setSelectedSubtype(draft.problem_subtype || "");
      } catch (err) {
        console.error("Erro ao carregar dados da página de subtipo:", err);
        setLocalError(err.message || "Não foi possível carregar os subtipos.");
      }
    }

    fetchData();
  }, [id]);

  const selectedFamilyData = useMemo(() => {
    return problemFamilies.find(
      (family) => family.id === problemDraft?.problem_family
    );
  }, [problemFamilies, problemDraft?.problem_family]);

  const availableSubtypes = selectedFamilyData?.subtypes || [];

  async function handleContinue() {
    if (!selectedSubtype) {
      setLocalError("Seleciona um subtipo antes de continuar.");
      return;
    }

    try {
      setLocalError("");

      await saveDraft({
        problem_subtype: selectedSubtype,
        status: "problem_subtype_selected",
        current_step: 3,
      });

      navigate(`/problems/${id}/upload`);
    } catch (err) {
      console.error("Erro ao guardar subtipo do problema:", err);
      setLocalError(err.message || "Não foi possível guardar o subtipo.");
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/type`);
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
        <p style={styles.step}>Step 2 de 7</p>
        <h1 style={styles.title}>Selecionar subtipo de problema</h1>
        <p style={styles.description}>
          Escolhe o subtipo que melhor representa o caso concreto que queres modelar.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {problemDraft?.problem_family ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Família selecionada:</span>
            <span style={styles.problemName}>
              {selectedFamilyData?.label || problemDraft.problem_family}
            </span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
        {localError ? <p style={styles.error}>{localError}</p> : null}

        {!problemDraft?.problem_family ? (
          <div style={styles.warningBox}>
            <p style={styles.warningTitle}>Família em falta</p>
            <p style={styles.warningText}>
              Primeiro tens de selecionar a família do problema antes de escolher o subtipo.
            </p>
          </div>
        ) : null}

        {problemDraft?.problem_family ? (
          availableSubtypes.length > 0 ? (
            <div style={styles.grid}>
              {availableSubtypes.map((subtype) => {
                const isSelected = selectedSubtype === subtype.id;

                return (
                  <button
                    key={subtype.id}
                    type="button"
                    onClick={() => setSelectedSubtype(subtype.id)}
                    style={{
                      ...styles.card,
                      ...(isSelected ? styles.cardSelected : {}),
                    }}
                  >
                    <h2 style={styles.cardTitle}>{subtype.label}</h2>
                    {subtype.description ? (
                      <p style={styles.cardDescription}>{subtype.description}</p>
                    ) : (
                      <p style={styles.cardDescriptionMuted}>
                        Sem descrição disponível.
                      </p>
                    )}
                  </button>
                );
              })}
            </div>
          ) : (
            <p style={styles.message}>
              Não existem subtipos disponíveis para esta família.
            </p>
          )
        ) : null}

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
            disabled={!selectedSubtype || saving || !problemDraft?.problem_family}
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
    maxWidth: "1100px",
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
    maxWidth: "720px",
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
  },
  problemInfo: {
    display: "inline-flex",
    gap: "8px",
    alignItems: "center",
    marginBottom: "16px",
    marginRight: "12px",
    padding: "10px 14px",
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "10px",
    flexWrap: "wrap",
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
  warningBox: {
    marginBottom: "24px",
    padding: "16px",
    borderRadius: "12px",
    backgroundColor: "#fffbeb",
    border: "1px solid #fde68a",
  },
  warningTitle: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#92400e",
  },
  warningText: {
    margin: 0,
    fontSize: "14px",
    color: "#92400e",
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
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
    gap: "20px",
    marginTop: "24px",
    marginBottom: "32px",
  },
  card: {
    textAlign: "left",
    padding: "24px",
    borderRadius: "16px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    cursor: "pointer",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
  },
  cardSelected: {
    border: "2px solid #0f62fe",
    boxShadow: "0 8px 20px rgba(15, 98, 254, 0.12)",
  },
  cardTitle: {
    margin: 0,
    marginBottom: "10px",
    fontSize: "22px",
    color: "#101828",
  },
  cardDescription: {
    margin: 0,
    fontSize: "15px",
    lineHeight: 1.6,
    color: "#475467",
  },
  cardDescriptionMuted: {
    margin: 0,
    fontSize: "15px",
    lineHeight: 1.6,
    color: "#98a2b3",
    fontStyle: "italic",
  },
  actions: {
    display: "flex",
    justifyContent: "space-between",
    gap: "16px",
    flexWrap: "wrap",
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