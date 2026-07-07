import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getProblemDraft,
  updateProblemDraft,
} from "../../../services/problemsApi";
import { uploadRoomsFile } from "../../../services/roomsApi";

const DRAFT_LOAD_TIMEOUT_MS = 10000;

export default function ProblemRoomsUploadStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const didLoadRef = useRef(false);

  const [pageLoading, setPageLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [retryKey, setRetryKey] = useState(0);
  const [localError, setLocalError] = useState("");

  const [problemDraft, setProblemDraft] = useState(null);
  const [fileName, setFileName] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [existingRoomsFile, setExistingRoomsFile] = useState(null);
  const [isReplacingFile, setIsReplacingFile] = useState(false);

  useEffect(() => {
    didLoadRef.current = false;
  }, [id, retryKey]);

  useEffect(() => {
    if (!id || didLoadRef.current) return;
    didLoadRef.current = true;

    let cancelled = false;

    async function fetchDraft() {
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

        if (draft?.uploaded_rooms_file) {
          setExistingRoomsFile({
            id: draft.uploaded_rooms_file,
            name: draft.uploaded_rooms_file_name || "Ficheiro sem nome",
            file: draft.uploaded_rooms_file_file || null,
          });
          setFileName(draft.uploaded_rooms_file_name || draft.name || "");
        } else {
          setExistingRoomsFile(null);
          setFileName(draft?.name || "");
        }
      } catch (err) {
        if (cancelled) return;

        console.error("Erro ao carregar o problem draft:", err);
        setProblemDraft(null);
        setExistingRoomsFile(null);
        setLocalError(err?.message || "Não foi possível carregar o draft.");
      } finally {
        if (!cancelled) {
          setPageLoading(false);
        }
      }
    }

    fetchDraft();

    return () => {
      cancelled = true;
    };
  }, [id, retryKey]);

  function handleRetry() {
    setRetryKey((prev) => prev + 1);
  }

  function handleBack() {
    navigate(`/problems/${id}/detail`);
  }

  function handleStartReplacing() {
    setIsReplacingFile(true);
    setSelectedFile(null);
    setLocalError("");
    setFileName(problemDraft?.name || "");
  }

  function handleCancelReplacing() {
    setIsReplacingFile(false);
    setSelectedFile(null);
    setLocalError("");
    setFileName(existingRoomsFile?.name || problemDraft?.name || "");
  }

  function handleFileChange(event) {
    const file = event.target.files?.[0] || null;
    setSelectedFile(file);
  }

  async function handleContinue() {
    if (existingRoomsFile && !isReplacingFile) {
      try {
        setSaving(true);
        setLocalError("");

        const updatedDraft = await updateProblemDraft(id, {
        });

        setProblemDraft(updatedDraft || problemDraft);
        navigate(`/problems/${id}/detail`);
      } catch (err) {
        console.error("Erro ao avançar para o mapping das salas:", err);
        setLocalError(err?.message || "Não foi possível continuar.");
      } finally {
        setSaving(false);
      }

      return;
    }

    if (!selectedFile) {
      setLocalError("Seleciona um ficheiro antes de continuar.");
      return;
    }

    try {
      setUploading(true);
      setSaving(true);
      setLocalError("");

      const uploadedRoomsFile = await uploadRoomsFile({
        name: selectedFile.name,
        file: selectedFile,
      });

      if (!uploadedRoomsFile?.id) {
        throw new Error("Não foi possível obter o ID do ficheiro carregado.");
      }

      const payload = {
        uploaded_rooms_file: uploadedRoomsFile.id,
        rooms_mapping_data: {},
        room_feature_resolution: {},
      };

      const updatedDraft = await updateProblemDraft(id, payload);

      setProblemDraft(updatedDraft || null);
      setExistingRoomsFile({
        id: uploadedRoomsFile.id,
        name: uploadedRoomsFile.name || selectedFile.name,
        file: uploadedRoomsFile.file || null,
      });
      setIsReplacingFile(false);
      setSelectedFile(null);
      setFileName(uploadedRoomsFile.name || selectedFile.name);

      navigate(`/problems/${id}/detail`);
    } catch (err) {
      console.error("Erro ao fazer upload do ficheiro de salas:", err);
      setLocalError(
        err?.message || "Não foi possível fazer upload do ficheiro de salas."
      );
    } finally {
      setUploading(false);
      setSaving(false);
    }
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
          <p style={styles.step}>Passo 6</p>
          <h1 style={styles.title}>Carregar ficheiro adicional de salas</h1>
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
        <p style={styles.step}>Passo 6 de 8</p>
        <h1 style={styles.title}>Carregar ficheiro adicional de salas</h1>
        <p style={styles.description}>
          Faça upload do ficheiro com as características das salas que vais ligar ao horário.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {localError ? <p style={styles.error}>{localError}</p> : null}

        <div style={styles.formCard}>
          <div style={styles.field}>
            <label htmlFor="fileName" style={styles.label}>
              Nome do ficheiro
            </label>
            <input
              id="fileName"
              type="text"
              value={
                existingRoomsFile && !isReplacingFile
                  ? existingRoomsFile.name || "Ficheiro sem nome"
                  : fileName
              }
              onChange={(e) => setFileName(e.target.value)}
              placeholder="Ex.: Características das salas 2025/2026"
              style={styles.input}
              disabled={!!existingRoomsFile && !isReplacingFile}
            />
          </div>

          {existingRoomsFile && !isReplacingFile ? (
            <div style={styles.existingFileBox}>
              <div style={styles.existingFileHeader}>
                <div>
                  <p style={styles.existingFileTitle}>Ficheiro já carregado</p>
                  <p style={styles.existingFileName}>
                    {existingRoomsFile?.name || "Ficheiro sem nome"}
                  </p>
                </div>

                <span style={styles.badgeSuccess}>Associado ao problema</span>
              </div>

              <p style={styles.helperText}>
                Este problema já tem um ficheiro adicional associado. Podes continuar para
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
                <label htmlFor="roomsFile" style={styles.label}>
                  Ficheiro
                </label>
                <input
                  id="roomsFile"
                  type="file"
                  accept=".csv,.xlsx,.xls"
                  onChange={handleFileChange}
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

              {existingRoomsFile && isReplacingFile ? (
                <div style={styles.warningBox}>
                  <p style={styles.warningTitle}>Modo de substituição ativo</p>
                  <p style={styles.warningText}>
                    Vais substituir o ficheiro atualmente associado a este problema.
                    O mapping das salas e a resolução de características serão apagados e terão de ser refeitos.
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
              (!existingRoomsFile && !selectedFile) ||
              (isReplacingFile && !selectedFile)
            }
            style={{
              ...styles.primaryButton,
              ...(saving || uploading
                ? styles.primaryButtonDisabled
                : {}),
            }}
          >
            {uploading || saving
              ? "A guardar..."
              : "Guardar"}
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
    padding: "12px 14px",
    borderRadius: "12px",
    border: "1px solid #fecaca",
    backgroundColor: "#fef2f2",
    fontSize: "15px",
    color: "#b42318",
  },
  message: {
    fontSize: "16px",
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
  primaryButtonDisabled: {
    opacity: 0.6,
    cursor: "not-allowed",
  },
};