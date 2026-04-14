import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemCatalog } from "../../services/problemsApi";

export default function ProblemTypeStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [problemFamilies, setProblemFamilies] = useState([]);
  const [selectedFamily, setSelectedFamily] = useState("");
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    async function fetchData() {
      try {
        setLocalError("");

        const [draft, catalog] = await Promise.all([
          loadDraft(id),
          getProblemCatalog(),
        ]);

        setSelectedFamily(draft.problem_family || "");
        setProblemFamilies(catalog.problem_families || []);
      } catch (err) {
        console.error("Erro ao carregar dados da página de tipo:", err);
        setLocalError(err.message || "Não foi possível carregar os tipos de problema.");
      }
    }

    fetchData();
  }, [id]);

  async function handleContinue() {
    if (!selectedFamily) {
      setLocalError("Seleciona uma família antes de continuar.");
      return;
    }

    try {
      setLocalError("");

      await saveDraft({
        problem_family: selectedFamily,
        problem_subtype: "",
        status: "problem_family_selected",
        current_step: 2,
        last_completed_step: 1,
      });

      navigate(`/problems/${id}/subtype`);
    } catch (err) {
      console.error("Erro ao guardar família do problema:", err);
      setLocalError(err.message || "Não foi possível guardar a família.");
    }
  }

  function handleBack() {
    navigate("/problems");
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
        <p style={styles.step}>Step 1 de 7</p>
        <h1 style={styles.title}>Selecionar tipo de problema</h1>
        <p style={styles.description}>
          Escolhe a família que melhor representa o problema que queres modelar.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
        {localError ? <p style={styles.error}>{localError}</p> : null}

        {problemFamilies.length > 0 ? (
          <div style={styles.grid}>
            {problemFamilies.map((family) => {
              const isSelected = selectedFamily === family.id;

              return (
                <button
                  key={family.id}
                  type="button"
                  onClick={() => setSelectedFamily(family.id)}
                  style={{
                    ...styles.card,
                    ...(isSelected ? styles.cardSelected : {}),
                  }}
                >
                  <h2 style={styles.cardTitle}>{family.label}</h2>
                  <p style={styles.cardDescription}>
                    {family.description || "Sem descrição disponível."}
                  </p>
                </button>
              );
            })}
          </div>
        ) : (
          <p style={styles.message}>Não existem tipos de problema disponíveis.</p>
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
            disabled={!selectedFamily || saving}
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
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
    gap: "20px",
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