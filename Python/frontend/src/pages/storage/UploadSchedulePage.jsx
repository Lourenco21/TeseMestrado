/**import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { uploadScheduleFile } from "../../services/schedulesApi";
import "../../components/schedule/ScheduleUploadReview.css";

export default function UploadSchedulePage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage("");
    setError("");

    if (!file) {
      setError("Seleciona um ficheiro.");
      return;
    }

    try {
      setLoading(true);

      const result = await uploadScheduleFile({
        name: name || file.name,
        file,
      });

      setMessage(`Upload concluído: ${result.name}`);
      setName("");
      setFile(null);
      navigate(`/horario/upload/${result.id}/review`);
      e.target.reset();
    } catch (err) {
      setError(err.message || "Erro ao fazer upload.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="schedule-page">
      <div className="upload-page">
        <h1>Upload de horário</h1>
        <p>Carrega um ficheiro real para testar a importação.</p>

        <form className="upload-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="schedule-name">Nome</label>
            <input
              id="schedule-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ex.: Horário Engenharia Informática"
            />
          </div>

          <div className="form-group">
            <label htmlFor="schedule-file">Ficheiro</label>
            <input
              id="schedule-file"
              type="file"
              accept=".csv,.xlsx,.xls,.pdf"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "A enviar..." : "Fazer upload"}
          </button>
        </form>

        {message && <p className="upload-success">{message}</p>}
        {error && <p className="upload-error">{error}</p>}
      </div>
    </div>
  );
}**/