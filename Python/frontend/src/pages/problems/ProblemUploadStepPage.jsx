import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { uploadSchedule } from "../../services/schedulesApi";

export default function ProblemUploadStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
    useProblemWizard();

  const [scheduleName, setScheduleName] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    async function fetchDraft() {
      try {
        const draft = await loadDraft(id);

        if (draft?.name) {
          setScheduleName(draft.name);
        }
      } catch (err) {
        console.error("Erro ao carregar o problem draft:", err);
      }
    }

    fetchDraft();
  }, [id]);

  async function handleContinue() {
    if (!selectedFile) {
      setLocalError("Seleciona um ficheiro antes de continuar.");
      return;
    }

    try {
      setUploading(true);
      setLocalError("");

      const uploadedSchedule = await uploadSchedule({
        name: scheduleName.trim() || selectedFile.name,
        file: selectedFile,
      });

      await saveDraft({
        uploaded_schedule: uploadedSchedule.id,
        status: "file_uploaded",
        current_step: 4,
      });

      navigate(`/problems/${id}/mapping`);
    } catch (err) {
      console.error("Erro ao fazer upload do ficheiro:", err);
      setLocalError(err.message || "Não foi possível fazer upload do ficheiro.");
    } finally {
      setUploading(false);
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/subtype`);
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Step 3 de 7</p>
        <h1 style={styles.title}>Carregar ficheiro de dados</h1>
        <p style={styles.description}>
          Faz upload do ficheiro que vai servir de base para este problema de
          otimização.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
        {localError ? <p style={styles.error}>{localError}</p> : null}

        <div style={styles.formCard}>
          <div style={styles.field}>
            <label htmlFor="scheduleName" style={styles.label}>
              Nome do ficheiro / conjunto de dados
            </label>
            <input
              id="scheduleName"
              type="text"
              value={scheduleName}
              onChange={(e) => setScheduleName(e.target.value)}
              placeholder="Ex.: Horário LEI 2025/2026"
              style={styles.input}
            />
          </div>

          <div style={styles.field}>
            <label htmlFor="scheduleFile" style={styles.label}>
              Ficheiro
            </label>
            <input
              id="scheduleFile"
              type="file"
              accept=".csv,.xlsx,.xls"
              onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              style={styles.inputFile}
            />
            <p style={styles.helperText}>
              Formatos suportados: CSV, XLSX e XLS.
            </p>
          </div>

          {selectedFile ? (
            <div style={styles.fileInfo}>
              <p style={styles.fileName}>{selectedFile.name}</p>
              <p style={styles.fileMeta}>
                {(selectedFile.size / 1024).toFixed(1)} KB
              </p>
            </div>
          ) : null}
        </div>

        <div style={styles.actions}>
          <button type="button" onClick={handleBack} style={styles.secondaryButton}>
            Voltar
          </button>

          <button
            type="button"
            onClick={handleContinue}
            disabled={!selectedFile || saving || uploading}
            style={styles.primaryButton}
          >
            {uploading || saving ? "A guardar..." : "Continuar"}
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
    maxWidth: "900px",
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
  formCard: {
    padding: "24px",
    borderRadius: "16px",
    border: "1px solid #eaecf0",
    backgroundColor: "#ffffff",
    marginBottom: "32px",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
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
    color: "#101828",
  },
  input: {
    padding: "12px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    fontSize: "16px",
  },
  inputFile: {
    padding: "12px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    fontSize: "14px",
  },
  helperText: {
    margin: 0,
    fontSize: "13px",
    color: "#667085",
  },
  fileInfo: {
    marginTop: "8px",
    padding: "14px",
    borderRadius: "12px",
    backgroundColor: "#f9fafb",
    border: "1px solid #eaecf0",
  },
  fileName: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "15px",
    fontWeight: 600,
    color: "#101828",
  },
  fileMeta: {
    margin: 0,
    fontSize: "14px",
    color: "#667085",
  },
  error: {
    marginBottom: "16px",
    fontSize: "16px",
    color: "#b42318",
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