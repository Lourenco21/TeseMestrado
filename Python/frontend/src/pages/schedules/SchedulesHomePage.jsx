import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSchedules } from "../../services/schedulesApi";

function formatDate(value) {
  if (!value) return "-";

  return new Date(value).toLocaleString("pt-PT", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export default function SchedulesHomePage() {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadSchedules() {
      try {
        setLoading(true);
        const result = await getSchedules();
        setSchedules(result);
      } catch (err) {
        setError(err.message || "Erro ao carregar horários.");
      } finally {
        setLoading(false);
      }
    }

    loadSchedules();
  }, []);

  if (loading) {
    return (
      <div className="page-shell">
        <div className="page-container">A carregar horários...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-shell">
        <div className="page-container">Erro: {error}</div>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1>Horários carregados</h1>
            <p>Lista de ficheiros importados e respetivo estado de mapping.</p>
          </div>

          <Link to="/horario/upload" className="primary-link-button">
            Novo upload
          </Link>
        </div>

        {schedules.length === 0 ? (
          <div className="surface-card panel empty-state">
            <h2>Ainda não existem horários</h2>
            <p>Faz o primeiro upload para começar.</p>
          </div>
        ) : (
          <div className="surface-card panel">
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Ficheiro</th>
                    <th>Upload</th>
                    <th>Atualizado</th>
                    <th>Estado</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {schedules.map((schedule) => (
                    <tr key={schedule.id}>
                      <td>{schedule.name}</td>
                      <td>{schedule.file_name}</td>
                      <td>{formatDate(schedule.uploaded_at)}</td>
                      <td>{formatDate(schedule.updated_at)}</td>
                      <td>
                        {schedule.has_mapping ? (
                          <span className="badge badge-success">
                            <span className="badge-dot" aria-hidden="true" />
                            <span>Mapping guardado</span>
                          </span>
                        ) : (
                          <span className="badge badge-warning">
                            <span className="badge-dot" aria-hidden="true" />
                            <span>Por mapear</span>
                          </span>
                        )}
                      </td>
                      <td>
                        <Link
                          to={`/horario/upload/${schedule.id}/review`}
                          className="schedule-table-link"
                        >
                          Rever mapping
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}