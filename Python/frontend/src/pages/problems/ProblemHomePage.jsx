import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { listProblemDrafts } from "../../services/problemsApi";

export default function ProblemHomePage() {
  const navigate = useNavigate();

  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadProblems() {
      try {
        setLoading(true);
        setError("");
        const data = await listProblemDrafts();
        setProblems(data);
      } catch (err) {
        setError(err.message || "Não foi possível carregar os problemas.");
      } finally {
        setLoading(false);
      }
    }

    loadProblems();
  }, []);

  function handleCreateProblem() {
    navigate("/problems/new");
  }

  function handleOpenProblem(problemId) {
    navigate(`/problems/${problemId}/type`);
  }

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <div>
          <p style={styles.eyebrow}>Problemas</p>
          <h1 style={styles.title}>Os meus problemas de otimização</h1>
          <p style={styles.description}>
            Aqui podes consultar problemas já criados e continuar a modelação de
            um novo problema através do wizard.
          </p>
        </div>

        <button type="button" onClick={handleCreateProblem} style={styles.button}>
          Novo problema
        </button>
      </div>

      {loading ? <p style={styles.message}>A carregar problemas...</p> : null}

      {error ? <p style={styles.error}>{error}</p> : null}

      {!loading && !error && problems.length === 0 ? (
        <div style={styles.emptyState}>
          <h2 style={styles.emptyTitle}>Ainda não tens problemas criados</h2>
          <p style={styles.emptyText}>
            Começa por criar um novo problema para iniciar o wizard de modelação.
          </p>
          <button
            type="button"
            onClick={handleCreateProblem}
            style={styles.button}
          >
            Criar primeiro problema
          </button>
        </div>
      ) : null}

      {!loading && !error && problems.length > 0 ? (
        <div style={styles.grid}>
          {problems.map((problem) => (
            <div key={problem.id} style={styles.card}>
              <p style={styles.cardStatus}>{problem.status || "created"}</p>

              <h2 style={styles.cardTitle}>
                {problem.name || `Problema #${problem.id}`}
              </h2>

              <p style={styles.cardText}>
                Família: {problem.problem_family || "Ainda não definida"}
              </p>

              <p style={styles.cardText}>
                Subtipo: {problem.problem_subtype || "Ainda não definido"}
              </p>

              <p style={styles.cardText}>
                Step atual: {problem.current_step ?? "-"}
              </p>

              <button
                type="button"
                onClick={() => handleOpenProblem(problem.id)}
                style={styles.secondaryButton}
              >
                Abrir problema
              </button>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}

const styles = {
  page: {
    padding: "32px",
    maxWidth: "1200px",
    margin: "0 auto",
  },
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "24px",
    marginBottom: "32px",
    flexWrap: "wrap",
  },
  eyebrow: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 600,
    color: "#667085",
  },
  title: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "36px",
    color: "#101828",
  },
  description: {
    margin: 0,
    maxWidth: "720px",
    fontSize: "16px",
    lineHeight: 1.6,
    color: "#475467",
  },
  button: {
    border: "none",
    borderRadius: "10px",
    padding: "12px 18px",
    backgroundColor: "#0f62fe",
    color: "#ffffff",
    fontSize: "16px",
    fontWeight: 600,
    cursor: "pointer",
  },
  secondaryButton: {
    marginTop: "16px",
    border: "1px solid #d0d5dd",
    borderRadius: "10px",
    padding: "10px 16px",
    backgroundColor: "#ffffff",
    color: "#101828",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
  },
  message: {
    fontSize: "16px",
    color: "#475467",
  },
  error: {
    fontSize: "16px",
    color: "#b42318",
  },
  emptyState: {
    marginTop: "40px",
    padding: "32px",
    borderRadius: "16px",
    backgroundColor: "#f9fafb",
    border: "1px solid #eaecf0",
  },
  emptyTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "24px",
    color: "#101828",
  },
  emptyText: {
    margin: 0,
    marginBottom: "20px",
    fontSize: "16px",
    color: "#475467",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    gap: "20px",
  },
  card: {
    padding: "24px",
    borderRadius: "16px",
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.06)",
  },
  cardStatus: {
    margin: 0,
    marginBottom: "10px",
    fontSize: "13px",
    fontWeight: 700,
    color: "#0f62fe",
    textTransform: "uppercase",
  },
  cardTitle: {
    margin: 0,
    marginBottom: "14px",
    fontSize: "22px",
    color: "#101828",
  },
  cardText: {
    margin: 0,
    marginBottom: "8px",
    fontSize: "15px",
    color: "#475467",
  },
};