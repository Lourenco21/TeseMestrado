import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";

export default function ProblemWizardStartPage() {
  const navigate = useNavigate();
  const { createDraft, loading, error } = useProblemWizard();

  const [problemName, setProblemName] = useState("");

  async function handleStartWizard() {
    try {
      const draft = await createDraft({
        name: problemName.trim(),
        status: "created",
        current_step: 1,
        last_completed_step: 0,
        wizard_data: {},
      });

      navigate(`/problems/${draft.id}/type`);
    } catch (err) {
      console.error("Erro ao criar problem draft:", err);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>
        <p style={styles.step}>Novo problema</p>
        <h1 style={styles.title}>Começar modelação do problema</h1>
        <p style={styles.description}>
          Este wizard vai ajudar-te a descrever o problema de otimização passo a
          passo, de forma guiada.
        </p>

        <div style={styles.field}>
          <label htmlFor="problemName" style={styles.label}>
            Nome do problema
          </label>
          <input
            id="problemName"
            type="text"
            value={problemName}
            onChange={(e) => setProblemName(e.target.value)}
            placeholder="Ex.: Horário da Licenciatura em Engenharia Informática"
            style={styles.input}
          />
        </div>

        {error ? <p style={styles.error}>{error}</p> : null}

        <button
          type="button"
          onClick={handleStartWizard}
          disabled={loading}
          style={styles.button}
        >
          {loading ? "A criar..." : "Começar"}
        </button>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "32px",
    backgroundColor: "#f7f7f7",
  },
  card: {
    width: "100%",
    maxWidth: "640px",
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    padding: "32px",
    boxShadow: "0 10px 30px rgba(0,0,0,0.08)",
  },
  step: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 600,
    color: "#666",
  },
  title: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "32px",
    color: "#1f1f1f",
  },
  description: {
    margin: 0,
    marginBottom: "24px",
    fontSize: "16px",
    color: "#4f4f4f",
    lineHeight: 1.5,
  },
  field: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    marginBottom: "20px",
  },
  label: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#1f1f1f",
  },
  input: {
    padding: "12px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d0d0",
    fontSize: "16px",
  },
  error: {
    marginBottom: "16px",
    color: "#b42318",
    fontSize: "14px",
  },
  button: {
    padding: "12px 18px",
    border: "none",
    borderRadius: "10px",
    backgroundColor: "#0f62fe",
    color: "#fff",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
  },
};