/**import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { getScheduleMappingSuggestions, saveScheduleMapping  } from "../../services/schedulesApi";
import MappingReviewTable from "../../components/schedule/MappingReviewTable";

export default function ReviewScheduleMappingPage() {
  const { scheduleId } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [data, setData] = useState(null);
  const [selectedMappings, setSelectedMappings] = useState({});
  const [manualOverrides, setManualOverrides] = useState({});

  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const result = await getScheduleMappingSuggestions(scheduleId);
        setData(result);
        setSelectedMappings(result.selected_mappings || {});
        setManualOverrides({});
      } catch (err) {
        setError(err.message || "Erro ao carregar mapping.");
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [scheduleId]);

  function handleMappingChange(targetField, value) {
    setSelectedMappings((prev) => ({
      ...prev,
      [targetField]: value,
    }));

    setManualOverrides((prev) => ({
      ...prev,
      [targetField]: true,
    }));
  }

  const requiredMissing = useMemo(() => {
    if (!data) return [];

    return data.matches
      .filter((match) => match.required && !selectedMappings[match.target_field])
      .map((match) => match.target_label);
  }, [data, selectedMappings]);

  const duplicateColumns = useMemo(() => {
    const counts = {};

    Object.values(selectedMappings)
      .filter(Boolean)
      .forEach((value) => {
        counts[value] = (counts[value] || 0) + 1;
      });

    return Object.keys(counts).filter((key) => counts[key] > 1);
  }, [selectedMappings]);

  const duplicatedTargetFields = useMemo(() => {
    const duplicated = {};

    Object.entries(selectedMappings).forEach(([targetField, selectedColumn]) => {
      if (selectedColumn && duplicateColumns.includes(selectedColumn)) {
        duplicated[targetField] = true;
      }
    });

    return duplicated;
  }, [selectedMappings, duplicateColumns]);

  const hasDuplicates = duplicateColumns.length > 0;
  const hasValidationErrors = requiredMissing.length > 0 || hasDuplicates;

  if (loading) {
    return <div className="schedule-page">A carregar sugestões...</div>;
  }

  if (error) {
    return <div className="schedule-page">Erro: {error}</div>;
  }

  return (
    <div className="schedule-page">
      <div className="upload-page">
        <h1>Rever mapping de colunas</h1>
        <p>
          Ficheiro: <strong>{data.schedule_name}</strong>
          {data.mode === "saved" && " — mapping guardado carregado"}
          {data.mode === "suggested" && " — sugestões automáticas carregadas"}
        </p>

        {requiredMissing.length > 0 && (
          <p className="upload-error">
            Faltam campos obrigatórios: {requiredMissing.join(", ")}
          </p>
        )}

        {hasDuplicates && (
          <p className="upload-error">
            Existem colunas atribuídas a mais do que uma variável: {duplicateColumns.join(", ")}
          </p>
        )}

        <MappingReviewTable
          matches={data.matches}
          sourceColumns={data.source_columns}
          selectedMappings={selectedMappings}
          manualOverrides={manualOverrides}
          duplicatedTargetFields={duplicatedTargetFields}
          onMappingChange={handleMappingChange}
        />

        <button
          type="button"
          className="primary-button"
          disabled={hasValidationErrors}
          onClick={async () => {
            try {
              const result = await saveScheduleMapping(scheduleId, selectedMappings);
              console.log("Mapping guardado:", result);
            } catch (err) {
              setError(err.message || "Erro ao guardar mapping.");
            }
          }}
        >
          Confirmar mapping
        </button>
      </div>
    </div>
  );
}**/