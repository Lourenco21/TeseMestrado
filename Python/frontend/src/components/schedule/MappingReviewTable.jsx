import "./ScheduleUploadReview.css";

function getConfidenceMeta(score) {
  if (score >= 85) {
    return {
      label: "Alta",
      className: "confidence-badge confidence-high",
    };
  }

  if (score >= 70) {
    return {
      label: "Média",
      className: "confidence-badge confidence-medium",
    };
  }

  return {
    label: "Baixa",
    className: "confidence-badge confidence-low",
  };
}

export default function MappingReviewTable({
  matches,
  sourceColumns,
  selectedMappings,
  manualOverrides,
  duplicatedTargetFields,
  onMappingChange,
}) {
  return (
    <div className="mapping-review-table-wrapper">
      <table className="mapping-review-table">
        <thead>
          <tr>
            <th>Variável</th>
            <th>Coluna do ficheiro</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          {matches.map((match) => {
            const confidence = getConfidenceMeta(match.score);
            const isReviewedByUser = !!manualOverrides[match.target_field];
            const hasDuplicate = !!duplicatedTargetFields[match.target_field];

            return (
              <tr
                key={match.target_field}
                className={hasDuplicate ? "mapping-row-duplicate" : ""}
              >
                <td>
                  <div className="mapping-variable-cell">
                    <span className="mapping-variable-name">
                      {match.target_label}
                    </span>

                    {match.required && (
                      <span
                        className="required-indicator"
                        title="Campo obrigatório"
                        aria-label="Campo obrigatório"
                      >
                        *
                      </span>
                    )}
                  </div>
                </td>

                <td>
                  <div className="mapping-select-cell">
                    <select
                      className={hasDuplicate ? "mapping-select-error" : ""}
                      value={selectedMappings[match.target_field] || ""}
                      onChange={(e) =>
                        onMappingChange(match.target_field, e.target.value)
                      }
                    >
                      <option value="">-- Não mapear --</option>
                      {sourceColumns.map((column) => (
                        <option key={column} value={column}>
                          {column}
                        </option>
                      ))}
                    </select>

                    {hasDuplicate && (
                      <span className="mapping-inline-error">
                        Coluna repetida
                      </span>
                    )}
                  </div>
                </td>

                <td>
                  {isReviewedByUser ? (
                    <span
                      className="reviewed-badge"
                      aria-label="Revisto pelo utilizador"
                    >
                      <span className="reviewed-badge-icon" aria-hidden="true">
                        ✓
                      </span>
                      <span>Revisto pelo utilizador</span>
                    </span>
                  ) : (
                    <span
                      className={confidence.className}
                      aria-label={`Confiança ${confidence.label} com score ${Math.round(match.score)}`}
                    >
                      <span className="confidence-dot" aria-hidden="true" />
                      <span>{confidence.label}</span>
                      <span className="confidence-score">
                        {Math.round(match.score)}
                      </span>
                    </span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <p className="mapping-legend">
        <span className="required-indicator" aria-hidden="true">*</span> campo obrigatório
      </p>
    </div>
  );
}