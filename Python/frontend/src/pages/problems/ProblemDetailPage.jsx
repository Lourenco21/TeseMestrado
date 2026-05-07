import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { sendProblemToJava } from "../../services/problemsApi";

export default function ProblemSendToJavaPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [sending, setSending] = useState(false);
  const [localError, setLocalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [responseData, setResponseData] = useState(null);

  async function handleSend() {
    try {
      setSending(true);
      setLocalError("");
      setSuccessMessage("");
      setResponseData(null);

      const result = await sendProblemToJava(id);

      setSuccessMessage("Problema enviado com sucesso para o backend Java.");
      setResponseData(result);
    } catch (err) {
      console.error("Erro ao enviar para Java:", err);
      setLocalError(err.message || "Não foi possível enviar o problema para Java.");
    } finally {
      setSending(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Execução</p>
        <h1 style={styles.title}>Enviar problema para Java</h1>
        <p style={styles.description}>
          Usa este botão para enviar o problema atual para o backend Java em formato JSON.
        </p>

        <div style={styles.card}>
          <div style={styles.infoBox}>
            <p style={styles.infoTitle}>Payload esperado</p>
            <pre style={styles.pre}>
{`{
  "problem_id": 12,
  "name": "timetable test",
  "problem_type": "scheduling",
  "problem_subtype": "course_timetabling",
  "schedule_file_id": 31,
  "rooms_file_id": 3,
  "mapping_data": {},
  "rooms_mapping_data": {},
  "objectives": [],
  "constraints": []
}`}
            </pre>
          </div>

          {localError ? <p style={styles.error}>{localError}</p> : null}
          {successMessage ? <p style={styles.success}>{successMessage}</p> : null}

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
              {sending ? "A enviar..." : "Enviar para Java"}
            </button>
          </div>

          {responseData ? (
            <div style={styles.responseBox}>
              <p style={styles.responseTitle}>Resposta</p>
              <pre style={styles.responsePre}>
                {JSON.stringify(responseData, null, 2)}
              </pre>
            </div>
          ) : null}
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
    maxWidth: "920px",
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
    maxWidth: "720px",
  },
  card: {
    backgroundColor: "#ffffff",
    border: "1px solid #eaecf0",
    borderRadius: "16px",
    padding: "24px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.06)",
  },
  infoBox: {
    marginBottom: "20px",
    padding: "16px",
    borderRadius: "12px",
    backgroundColor: "#f9fafb",
    border: "1px solid #eaecf0",
  },
  infoTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "15px",
    fontWeight: 700,
    color: "#101828",
  },
  pre: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.6,
    color: "#344054",
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
  },
  error: {
    marginBottom: "16px",
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#fef3f2",
    border: "1px solid #fecdca",
    color: "#b42318",
    fontSize: "14px",
  },
  success: {
    marginBottom: "16px",
    padding: "12px 14px",
    borderRadius: "12px",
    backgroundColor: "#ecfdf3",
    border: "1px solid #abefc6",
    color: "#067647",
    fontSize: "14px",
  },
  actions: {
    display: "flex",
    gap: "12px",
    justifyContent: "space-between",
    flexWrap: "wrap",
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
  responseBox: {
    marginTop: "24px",
    padding: "16px",
    borderRadius: "12px",
    backgroundColor: "#0f172a",
    color: "#e2e8f0",
  },
  responseTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#ffffff",
  },
  responsePre: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.6,
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
  },
};