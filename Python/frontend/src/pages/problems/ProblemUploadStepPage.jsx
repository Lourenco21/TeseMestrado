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
  const [existingSchedule, setExistingSchedule] = useState(null);
  const [isReplacingFile, setIsReplacingFile] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    async function fetchDraft() {
      try {
        const draft = await loadDraft(id);

        if (draft?.name) {
          setScheduleName(draft.name);
        }

        if (draft?.uploaded_schedule) {
          setExistingSchedule(draft.uploaded_schedule);
        } else {
          setExistingSchedule(null);
        }
      } catch (err) {
        console.error("Erro ao carregar o problem draft:", err);
      }
    }

    fetchDraft();
  }, [id]);

  async function handleContinue() {
    if (existingSchedule && !isReplacingFile) {
      try {
        setLocalError("");

        await saveDraft({
          status: "file_uploaded",
          current_step: 4,
        });

        navigate(`/problems/${id}/mapping`);
      } catch (err) {
        console.error("Erro ao avançar para o mapping:", err);
        setLocalError(err.message || "Não foi possível continuar.");
      }

      return;
    }

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

      const updatedDraft = await saveDraft({
        uploaded_schedule: uploadedSchedule.id,
        status: "file_uploaded",
        current_step: 4,
        mapping_data: {},
      });

      setExistingSchedule(uploadedSchedule);
      setIsReplacingFile(false);
      setSelectedFile(null);

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

  function handleStartReplacing() {
    setIsReplacingFile(true);
    setSelectedFile(null);
    setLocalError("");
  }

  function handleCancelReplacing() {
    setIsReplacingFile(false);
    setSelectedFile(null);
    setLocalError("");
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Passo 3 de 7</p>
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
              disabled={!!existingSchedule && !isReplacingFile}
            />
          </div>

          {existingSchedule && !isReplacingFile ? (
            <div style={styles.existingFileBox}>
              <div style={styles.existingFileHeader}>
                <div>
                  <p style={styles.existingFileTitle}>Ficheiro já carregado</p>
                  <p style={styles.existingFileName}>
                    {existingSchedule.name || "Ficheiro sem nome"}
                  </p>
                </div>

                <span style={styles.badgeSuccess}>Associado ao problema</span>
              </div>

              <p style={styles.helperText}>
                Este problema já tem um ficheiro carregado. Podes continuar para
                o mapping ou substituir o ficheiro atual.
              </p>

              <div style={styles.replaceActions}>
                <button
                  type="button"
                  onClick={handleStartReplacing}
                  style={styles.replaceButton}
                >
                  Substituir ficheiro
                </button>
              </div>
            </div>
          ) : (
            <>
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

              {existingSchedule && isReplacingFile ? (
                <div style={styles.warningBox}>
                  <p style={styles.warningTitle}>Modo de substituição ativo</p>
                  <p style={styles.warningText}>
                    Vais substituir o ficheiro atualmente associado a este problema.
                  </p>

                  <button
                    type="button"
                    onClick={handleCancelReplacing}
                    style={styles.cancelReplaceButton}
                  >
                    Cancelar substituição
                  </button>
                </div>
              ) : null}
            </>
          )}
        </div>

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
              saving ||
              uploading ||
              (!existingSchedule && !selectedFile) ||
              (isReplacingFile && !selectedFile)
            }
            style={styles.primaryButton}
          >
            {uploading || saving
              ? "A guardar..."
              : existingSchedule && !isReplacingFile
              ? "Continuar para mapping"
              : "Guardar e continuar"}
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
  existingFileBox: {
    padding: "20px",
    borderRadius: "14px",
    backgroundColor: "#f0fdf4",
    border: "1px solid #bbf7d0",
  },
  existingFileHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "16px",
    flexWrap: "wrap",
    marginBottom: "12px",
  },
  existingFileTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#166534",
    textTransform: "uppercase",
  },
  existingFileName: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#101828",
  },
  badgeSuccess: {
    display: "inline-flex",
    alignItems: "center",
    padding: "6px 10px",
    borderRadius: "999px",
    backgroundColor: "#dcfce7",
    color: "#166534",
    fontSize: "12px",
    fontWeight: 700,
  },
  replaceActions: {
    marginTop: "16px",
  },
  replaceButton: {
    padding: "10px 16px",
    borderRadius: "10px",
    border: "1px solid #86efac",
    backgroundColor: "#ffffff",
    color: "#166534",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
  },
  warningBox: {
    marginTop: "20px",
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
    marginBottom: "12px",
    fontSize: "14px",
    color: "#92400e",
  },
  cancelReplaceButton: {
    padding: "8px 14px",
    borderRadius: "10px",
    border: "1px solid #fcd34d",
    backgroundColor: "#ffffff",
    color: "#92400e",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
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