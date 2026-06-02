import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import {
  getProblemRoomsMappingSuggestions,
  saveProblemRoomsMapping,
} from "../../services/roomsApi";

function createEmptyCharacteristics(format = "") {
  return {
    format,
    config: {
      source_column: "",
      separator: "",
      selected_columns: [],
      selected_values: [],
      start_column: "",
      end_column: "",
    },
  };
}

function MultiCheckboxDropdown({
  label,
  options,
  selectedValues,
  onChange,
  placeholder = "Selecionar colunas",
}) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (!containerRef.current?.contains(event.target)) {
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  function toggleValue(value) {
    const exists = selectedValues.includes(value);

    if (exists) {
      onChange(selectedValues.filter((item) => item !== value));
    } else {
      onChange([...selectedValues, value]);
    }
  }

  return (
    <div style={styles.field}>
      {label ? <label style={styles.label}>{label}</label> : null}

      <div style={styles.dropdownContainer} ref={containerRef}>
        <button
          type="button"
          onClick={() => setOpen((prev) => !prev)}
          style={styles.dropdownButtonCompact}
        >
          <span
            style={
              selectedValues.length
                ? styles.dropdownValue
                : styles.dropdownPlaceholder
            }
          >
            {selectedValues.length > 0
              ? `${selectedValues.length} coluna(s) selecionada(s)`
              : placeholder}
          </span>
          <span style={styles.dropdownArrow}>{open ? "▲" : "▼"}</span>
        </button>

        {open ? (
          <div style={styles.dropdownMenu}>
            {options.length === 0 ? (
              <div style={styles.dropdownEmpty}>Sem colunas disponíveis</div>
            ) : (
              options.map((option) => {
                const checked = selectedValues.includes(option);

                return (
                  <label key={option} style={styles.checkboxRow}>
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => toggleValue(option)}
                    />
                    <span>{option}</span>
                  </label>
                );
              })
            )}
          </div>
        ) : null}
      </div>

      {selectedValues.length > 0 ? (
        <div style={styles.chipsRowCompact}>
          {selectedValues.map((item) => (
            <span key={item} style={styles.chip}>
              <span>{item}</span>
              <button
                type="button"
                onClick={() =>
                  onChange(selectedValues.filter((value) => value !== item))
                }
                style={styles.chipRemoveButton}
              >
                ×
              </button>
            </span>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function MultiValueInlineInput({
  label,
  values,
  inputValue,
  setInputValue,
  onAdd,
  onRemove,
  placeholder,
  helperText,
}) {
  function handleKeyDown(event) {
    if (event.key === "Enter") {
      event.preventDefault();
      onAdd();
    }
  }

  return (
    <div style={styles.field}>
      <label style={styles.label}>{label}</label>

      <div style={styles.inlineValuesRow}>
        <div style={styles.inlineInputRowCompact}>
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            style={styles.inputCompact}
          />
          <button type="button" onClick={onAdd} style={styles.addButton}>
            Adicionar
          </button>
        </div>

        {values.length > 0 ? (
          <div style={styles.inlineChipsRow}>
            {values.map((item) => (
              <span key={item} style={styles.chip}>
                <span>{item}</span>
                <button
                  type="button"
                  onClick={() => onRemove(item)}
                  style={styles.chipRemoveButton}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        ) : null}
      </div>

      {helperText ? <p style={styles.helperText}>{helperText}</p> : null}
    </div>
  );
}

export default function ProblemRoomsMappingStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, saveDraft, loadDraft, loading, error } = useProblemWizard();

  const [schema, setSchema] = useState(null);
  const [matches, setMatches] = useState([]);
  const [selectedFieldMappings, setSelectedFieldMappings] = useState({});
  const [manualChanges, setManualChanges] = useState({});
  const [scheduleSourceColumns, setScheduleSourceColumns] = useState([]);
  const [roomsSourceColumns, setRoomsSourceColumns] = useState([]);
  const [linking, setLinking] = useState({
    schedule_room_column: "",
    rooms_file_room_column: "",
  });
  const [characteristics, setCharacteristics] = useState(
    createEmptyCharacteristics()
  );
  const [selectedValueInput, setSelectedValueInput] = useState("");
  const [pageLoading, setPageLoading] = useState(true);
  const [localError, setLocalError] = useState("");
  const [localSaving, setLocalSaving] = useState(false);

  const initializedRef = useRef(false);

  useEffect(() => {
    initializedRef.current = false;
  }, [id]);

  useEffect(() => {
    if (initializedRef.current) return;
    initializedRef.current = true;

    let cancelled = false;

    async function loadData() {
      try {
        setPageLoading(true);
        setLocalError("");

        const draft = await loadDraft(id);
        const result = await getProblemRoomsMappingSuggestions(id);

        if (cancelled) return;

        setSchema(result.schema || null);
        setMatches(result.matches || []);
        setRoomsSourceColumns(result.rooms_source_columns || []);
        setScheduleSourceColumns(result.schedule_source_columns || []);

        const saved = result.saved_rooms_mapping_data || draft?.rooms_mapping_data || {};

        if (saved.field_mappings && Object.keys(saved.field_mappings).length > 0) {
          setSelectedFieldMappings(saved.field_mappings);
          setManualChanges(saved.manual_changes || {});
        } else {
          setSelectedFieldMappings(result.selected_field_mappings || {});
          setManualChanges({});
        }

        setLinking(
          saved.linking || {
            schedule_room_column: result.suggested_schedule_room_column || "",
            rooms_file_room_column:
              saved.field_mappings?.room_name ||
              result.selected_field_mappings?.room_name ||
              "",
          }
        );

        setCharacteristics(
          saved.characteristics
            ? {
                ...createEmptyCharacteristics(saved.characteristics.format || ""),
                ...saved.characteristics,
                config: {
                  ...createEmptyCharacteristics(
                    saved.characteristics.format || ""
                  ).config,
                  ...(saved.characteristics.config || {}),
                },
              }
            : createEmptyCharacteristics()
        );
      } catch (err) {
        if (!cancelled) {
          console.error("Erro ao carregar mapping de salas:", err);
          setLocalError(
            err.message || "Não foi possível carregar o mapping das salas."
          );
        }
      } finally {
        if (!cancelled) {
          setPageLoading(false);
        }
      }
    }

    loadData();

    return () => {
      cancelled = true;
    };

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const sourceColumnUsage = useMemo(() => {
    const usage = {};

    Object.entries(selectedFieldMappings).forEach(([targetField, sourceColumn]) => {
      if (!sourceColumn) return;
      if (!usage[sourceColumn]) usage[sourceColumn] = [];
      usage[sourceColumn].push(targetField);
    });

    return usage;
  }, [selectedFieldMappings]);

  const isMappingSaved = useMemo(() => {
    const mapping =
      problemDraft?.rooms_mapping_data?.field_mappings ||
      problemDraft?.rooms_mapping_data?.mapping;
    return mapping && Object.keys(mapping).length > 0;
  }, [problemDraft]);

  const requiredFields = useMemo(() => {
    return (schema?.fields || []).filter((field) => field.required);
  }, [schema]);

  const missingRequiredFields = useMemo(() => {
    return requiredFields.filter((field) => !selectedFieldMappings[field.key]);
  }, [requiredFields, selectedFieldMappings]);

  const duplicatedFields = useMemo(() => {
    return Object.entries(sourceColumnUsage)
      .filter(([, targetFields]) => targetFields.length > 1)
      .map(([sourceColumn]) => sourceColumn);
  }, [sourceColumnUsage]);

  const linkingErrors = useMemo(() => {
    const errors = [];

    if (!linking.schedule_room_column) {
      errors.push("Falta selecionar a coluna do horário que identifica a sala.");
    }

    if (!linking.rooms_file_room_column) {
      errors.push(
        "Falta selecionar a coluna do ficheiro de salas que identifica a sala."
      );
    }

    return errors;
  }, [linking]);

  const characteristicsErrors = useMemo(() => {
    const errors = [];

    if (!characteristics?.format) {
      errors.push("Seleciona a forma como as características estão representadas.");
      return errors;
    }

    if (characteristics.format === "single_column_list") {
      if (!characteristics.config?.source_column) {
        errors.push("Seleciona a coluna onde estão as características.");
      }
      if (!characteristics.config?.separator?.trim()) {
        errors.push("Indica o separador usado na coluna de características.");
      }
    }

    if (characteristics.format === "multiple_columns") {
      if (!characteristics.config?.selected_columns?.length) {
        errors.push("Seleciona pelo menos uma coluna de características.");
      }
      if (!characteristics.config?.selected_values?.length) {
        errors.push(
          "Adiciona pelo menos um valor que indique que a característica está selecionada."
        );
      }
    }

    if (characteristics.format === "range_columns") {
      if (!characteristics.config?.start_column) {
        errors.push("Seleciona a coluna inicial do intervalo.");
      }
      if (!characteristics.config?.end_column) {
        errors.push("Seleciona a coluna final do intervalo.");
      }
      if (!characteristics.config?.selected_values?.length) {
        errors.push(
          "Adiciona pelo menos um valor que indique que a característica está selecionada."
        );
      }
    }

    return errors;
  }, [characteristics]);

  function handleFieldMappingChange(targetField, selectedColumn) {
    setSelectedFieldMappings((prev) => ({
      ...prev,
      [targetField]: selectedColumn,
    }));

    setManualChanges((prev) => ({
      ...prev,
      [targetField]: true,
    }));

    if (targetField === "room_name") {
      setLinking((prev) => ({
        ...prev,
        rooms_file_room_column: selectedColumn,
      }));
    }
  }

  function handleLinkingChange(field, value) {
    setLinking((prev) => ({
      ...prev,
      [field]: value,
    }));
  }

  function handleCharacteristicsFormatChange(format) {
    setCharacteristics(createEmptyCharacteristics(format));
    setSelectedValueInput("");
  }

  function handleCharacteristicsConfigChange(field, value) {
    setCharacteristics((prev) => ({
      ...prev,
      config: {
        ...prev.config,
        [field]: value,
      },
    }));
  }

  function addSelectedValues() {
    const rawValues = selectedValueInput
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);

    if (rawValues.length === 0) return;

    setCharacteristics((prev) => {
      const current = prev.config?.selected_values || [];
      const merged = Array.from(new Set([...current, ...rawValues]));
      return {
        ...prev,
        config: {
          ...prev.config,
          selected_values: merged,
        },
      };
    });

    setSelectedValueInput("");
  }

  function removeSelectedValue(valueToRemove) {
    setCharacteristics((prev) => ({
      ...prev,
      config: {
        ...prev.config,
        selected_values: (prev.config?.selected_values || []).filter(
          (item) => item !== valueToRemove
        ),
      },
    }));
  }

  function isFieldDuplicate(targetField) {
    const sourceColumn = selectedFieldMappings[targetField];
    if (!sourceColumn) return false;
    return (sourceColumnUsage[sourceColumn] || []).length > 1;
  }

  function getMatchByField(targetField) {
    return matches.find((match) => match.target_field === targetField);
  }

  function getConfidenceClass(confidence) {
    if (confidence >= 0.8) return styles.confidenceStrong;
    if (confidence >= 0.65) return styles.confidenceWeak;
    return styles.confidenceLow;
  }

  async function handleContinue() {
    if (
      missingRequiredFields.length > 0 ||
      duplicatedFields.length > 0 ||
      linkingErrors.length > 0 ||
      characteristicsErrors.length > 0
    ) {
      setLocalError(
        "Corrige os campos obrigatórios, duplicações e configuração das características antes de avançar."
      );
      return;
    }

    try {
      setLocalSaving(true);
      setLocalError("");

      const normalizedCharacteristics = {
        ...characteristics,
        config: {
          ...characteristics.config,
        },
      };

      const payload = {
        field_mappings: selectedFieldMappings,
        linking,
        characteristics: normalizedCharacteristics,
      };

      await saveDraft({
        current_step: 9,
        last_completed_step: 8,
      });

      await saveProblemRoomsMapping(id, payload);
      navigate(`/problems/${id}/detail`);
    } catch (err) {
      console.error("Erro ao guardar mapping das salas:", err);
      setLocalError(err.message || "Não foi possível guardar o mapping.");
    } finally {
      setLocalSaving(false);
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/rooms-upload`);
  }

  if (loading && !problemDraft) {
    return (
      <div style={styles.page}>
        <div style={styles.container}>
          <p style={styles.message}>A carregar problema...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <p style={styles.step}>Passo 7 de 7</p>
        <h1 style={styles.title}>Mapping do ficheiro de salas</h1>
        <p style={styles.description}>
          Associa as colunas do ficheiro de salas aos campos necessários e define
          como as características estão representadas.
        </p>

        {problemDraft?.name ? (
          <div style={styles.problemInfo}>
            <span style={styles.problemLabel}>Problema atual:</span>
            <span style={styles.problemName}>{problemDraft.name}</span>
          </div>
        ) : null}

        {error ? <p style={styles.error}>{error}</p> : null}
        {localError ? <p style={styles.error}>{localError}</p> : null}

        {pageLoading ? (
          <p style={styles.message}>A carregar sugestões de mapping...</p>
        ) : (
          <>
            <div style={styles.summaryBar}>
              <span style={styles.summaryItem}>
                Campos obrigatórios em falta: {missingRequiredFields.length}
              </span>
              <span style={styles.summaryItem}>
                Colunas repetidas: {duplicatedFields.length}
              </span>
              <span style={styles.summaryItem}>
                Erros de ligação: {linkingErrors.length}
              </span>
              <span style={styles.summaryItem}>
                Erros nas características: {characteristicsErrors.length}
              </span>
            </div>

            <div style={styles.sectionCard}>
              <h2 style={styles.sectionTitle}>Campos base da sala</h2>

              <div style={styles.tableCard}>
                <div style={styles.tableHeader}>
                  <div style={styles.headerCell}>Variável</div>
                  <div style={styles.headerCell}>Coluna do ficheiro</div>
                  {isMappingSaved ? (
                    <div style={styles.headerCell}>Estado</div>
                  ) : (
                    <div style={styles.headerCell}>Confiança</div>
                  )}
                </div>

                {(schema?.fields || []).map((field) => {
                  const match = getMatchByField(field.key);
                  const confidence = match?.confidence || 0;
                  const isRequired = !!field.required;
                  const isDuplicate = isFieldDuplicate(field.key);
                  const isManual = !!manualChanges[field.key];

                  return (
                    <div
                      key={field.key}
                      style={{
                        ...styles.row,
                        ...(isRequired && !selectedFieldMappings[field.key]
                          ? styles.rowMissing
                          : {}),
                        ...(isDuplicate ? styles.rowDuplicate : {}),
                      }}
                    >
                      <div style={styles.targetCell}>
                        <div style={styles.fieldLabelRow}>
                          <span>{field.label}</span>
                          {isRequired ? (
                            <span style={styles.requiredBadge}>Obrigatório</span>
                          ) : null}
                        </div>

                        {field.description ? (
                          <p style={styles.fieldDescription}>{field.description}</p>
                        ) : null}
                      </div>

                      <div style={styles.selectCell}>
                        <select
                          value={selectedFieldMappings[field.key] || ""}
                          onChange={(e) =>
                            handleFieldMappingChange(field.key, e.target.value)
                          }
                          style={{
                            ...styles.select,
                            ...(isDuplicate ? styles.selectDuplicate : {}),
                          }}
                        >
                          <option value="">-- Selecionar coluna --</option>
                          {match?.available_source_columns?.map((column) => (
                            <option key={column} value={column}>
                              {column}
                            </option>
                          ))}
                        </select>

                        {isDuplicate ? (
                          <p style={styles.inlineError}>
                            Esta coluna está a ser usada em mais do que um campo.
                          </p>
                        ) : null}
                      </div>

                      <div style={styles.confidenceCell}>
                        {isMappingSaved ? (
                          selectedFieldMappings[field.key] ? (
                            <span style={styles.manualLabel}>
                              Revisto pelo utilizador
                            </span>
                          ) : (
                            <span style={styles.manualLabel}>
                              Nenhuma coluna selecionada
                            </span>
                          )
                        ) : (
                          <>
                            {isManual ? (
                              <span style={styles.manualLabel}>
                                Revisto pelo utilizador
                              </span>
                            ) : (
                              <>
                                <span
                                  style={{
                                    ...styles.confidenceValue,
                                    ...getConfidenceClass(confidence),
                                  }}
                                >
                                  {(confidence * 100).toFixed(0)}%
                                </span>
                                <span style={styles.matchType}>
                                  {match?.match_type === "strong"
                                    ? "Sugestão forte"
                                    : match?.match_type === "medium"
                                    ? "Sugestão mediana"
                                    : match?.match_type === "weak"
                                    ? "Sugestão fraca"
                                    : "Sem sugestão"}
                                </span>
                              </>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div style={styles.sectionCard}>
              <h2 style={styles.sectionTitle}>Ligação ao horário</h2>
              <p style={styles.sectionDescription}>
                Indique as colunas que permitem identificar a sala tanto no ficheiro
                de salas como no horário, para que seja possível ligar as
                informações de ambos os ficheiros.
              </p>

              <div style={styles.linkGrid}>
                <div style={styles.field}>
                  <label style={styles.label}>
                    Coluna do horário que identifica a sala
                  </label>
                  <select
                    value={linking.schedule_room_column}
                    onChange={(e) =>
                      handleLinkingChange("schedule_room_column", e.target.value)
                    }
                    style={styles.select}
                  >
                    <option value="">-- Selecionar coluna --</option>
                    {scheduleSourceColumns.map((column) => (
                      <option key={column} value={column}>
                        {column}
                      </option>
                    ))}
                  </select>
                </div>

                <div style={styles.field}>
                  <label style={styles.label}>
                    Coluna do ficheiro de salas que identifica a sala
                  </label>
                  <select
                    value={linking.rooms_file_room_column}
                    onChange={(e) =>
                      handleLinkingChange("rooms_file_room_column", e.target.value)
                    }
                    style={styles.select}
                  >
                    <option value="">-- Selecionar coluna --</option>
                    {roomsSourceColumns.map((column) => (
                      <option key={column} value={column}>
                        {column}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {linkingErrors.length > 0 ? (
                <div style={styles.warningBox}>
                  <p style={styles.warningTitle}>Ligação incompleta</p>
                  {linkingErrors.map((item) => (
                    <p key={item} style={styles.warningText}>
                      {item}
                    </p>
                  ))}
                </div>
              ) : null}
            </div>

            <div style={styles.sectionCard}>
              <h2 style={styles.sectionTitle}>Características da sala</h2>
              <p style={styles.sectionDescription}>
                Escolha apenas uma forma de interpretar as características no ficheiro.
              </p>

              <div style={styles.formatCards}>
                {[
                  {
                    key: "single_column_list",
                    title: "Lista numa coluna",
                    desc: "Todas as características vêm numa única coluna e estão separadas por um delimitador.",
                  },
                  {
                    key: "multiple_columns",
                    title: "Várias colunas",
                    desc: "Cada coluna representa uma característica.",
                  },
                  {
                    key: "range_columns",
                    title: "Intervalo de colunas",
                    desc: "As características ocupam um intervalo contínuo de colunas.",
                  },
                ].map((item) => (
                  <button
                    key={item.key}
                    type="button"
                    onClick={() => handleCharacteristicsFormatChange(item.key)}
                    style={{
                      ...styles.formatCard,
                      ...(characteristics.format === item.key
                        ? styles.formatCardActive
                        : {}),
                    }}
                  >
                    <div style={styles.formatCardTitle}>{item.title}</div>
                    <div style={styles.formatCardDesc}>{item.desc}</div>
                  </button>
                ))}
              </div>

              {!characteristics.format ? (
                <div style={styles.infoBox}>
                  <p style={styles.infoTitle}>Nenhuma forma selecionada</p>
                  <p style={styles.infoText}>
                    Seleciona uma das opções acima para configurar a interpretação
                    das características.
                  </p>
                </div>
              ) : null}

              {characteristics.format ? (
                <div style={styles.conditionalBox}>
                  {characteristics.format === "single_column_list" ? (
                    <div style={styles.linkGrid}>
                      <div style={styles.field}>
                        <label style={styles.label}>Coluna das características</label>
                        <select
                          value={characteristics.config.source_column || ""}
                          onChange={(e) =>
                            handleCharacteristicsConfigChange(
                              "source_column",
                              e.target.value
                            )
                          }
                          style={styles.select}
                        >
                          <option value="">-- Selecionar coluna --</option>
                          {roomsSourceColumns.map((column) => (
                            <option key={column} value={column}>
                              {column}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div style={styles.field}>
                        <label style={styles.label}>Separador</label>
                        <input
                          type="text"
                          value={characteristics.config.separator || ""}
                          onChange={(e) =>
                            handleCharacteristicsConfigChange(
                              "separator",
                              e.target.value
                            )
                          }
                          placeholder="Ex.: ;  ou  ,  ou  |"
                          style={styles.input}
                        />
                      </div>
                    </div>
                  ) : null}

                  {characteristics.format === "multiple_columns" ? (
                    <>
                      <MultiCheckboxDropdown
                        label="Colunas das características"
                        options={roomsSourceColumns}
                        selectedValues={characteristics.config.selected_columns || []}
                        onChange={(values) =>
                          handleCharacteristicsConfigChange("selected_columns", values)
                        }
                        placeholder="Selecionar colunas"
                      />

                      <MultiValueInlineInput
                        label="Valores que indicam que está selecionada"
                        values={characteristics.config.selected_values || []}
                        inputValue={selectedValueInput}
                        setInputValue={setSelectedValueInput}
                        onAdd={addSelectedValues}
                        onRemove={removeSelectedValue}
                        placeholder="Ex.: X ou 1 ou Sim"
                        helperText="Pode adicionar vários valores e removê-los individualmente."
                      />
                    </>
                  ) : null}

                  {characteristics.format === "range_columns" ? (
                    <>
                      <div style={styles.linkGrid}>
                        <div style={styles.field}>
                          <label style={styles.label}>Coluna inicial</label>
                          <select
                            value={characteristics.config.start_column || ""}
                            onChange={(e) =>
                              handleCharacteristicsConfigChange(
                                "start_column",
                                e.target.value
                              )
                            }
                            style={styles.select}
                          >
                            <option value="">-- Selecionar coluna --</option>
                            {roomsSourceColumns.map((column) => (
                              <option key={column} value={column}>
                                {column}
                              </option>
                            ))}
                          </select>
                        </div>

                        <div style={styles.field}>
                          <label style={styles.label}>Coluna final</label>
                          <select
                            value={characteristics.config.end_column || ""}
                            onChange={(e) =>
                              handleCharacteristicsConfigChange(
                                "end_column",
                                e.target.value
                              )
                            }
                            style={styles.select}
                          >
                            <option value="">-- Selecionar coluna --</option>
                            {roomsSourceColumns.map((column) => (
                              <option key={column} value={column}>
                                {column}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>

                      <MultiValueInlineInput
                        label="Valores que indicam que está selecionada"
                        values={characteristics.config.selected_values || []}
                        inputValue={selectedValueInput}
                        setInputValue={setSelectedValueInput}
                        onAdd={addSelectedValues}
                        onRemove={removeSelectedValue}
                        placeholder="Ex.: X ou 1 ou Sim"
                        helperText="Pode adicionar vários valores e removê-los individualmente."
                      />
                    </>
                  ) : null}
                </div>
              ) : null}

              {characteristicsErrors.length > 0 ? (
                <div style={styles.warningBox}>
                  <p style={styles.warningTitle}>Configuração incompleta</p>
                  {characteristicsErrors.map((item) => (
                    <p key={item} style={styles.warningText}>
                      {item}
                    </p>
                  ))}
                </div>
              ) : null}
            </div>
          </>
        )}

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
              pageLoading ||
              localSaving ||
              missingRequiredFields.length > 0 ||
              duplicatedFields.length > 0 ||
              linkingErrors.length > 0 ||
              characteristicsErrors.length > 0
            }
            style={{
              ...styles.primaryButton,
              ...(pageLoading ||
              localSaving ||
              missingRequiredFields.length > 0 ||
              duplicatedFields.length > 0 ||
              linkingErrors.length > 0 ||
              characteristicsErrors.length > 0
                ? styles.primaryButtonDisabled
                : {}),
            }}
          >
            {localSaving ? "A guardar..." : "Continuar"}
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
    maxWidth: "1200px",
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
    maxWidth: "780px",
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
  message: {
    fontSize: "16px",
    color: "#475467",
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
  summaryBar: {
    display: "flex",
    gap: "16px",
    flexWrap: "wrap",
    marginBottom: "20px",
  },
  summaryItem: {
    padding: "10px 14px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "14px",
    fontWeight: 600,
  },
  sectionCard: {
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    border: "1px solid #eaecf0",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
    padding: "24px",
    marginBottom: "24px",
  },
  sectionTitle: {
    margin: 0,
    marginBottom: "12px",
    fontSize: "22px",
    fontWeight: 700,
    color: "#101828",
  },
  sectionDescription: {
    margin: 0,
    marginBottom: "18px",
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  tableCard: {
    backgroundColor: "#ffffff",
    borderRadius: "16px",
    border: "1px solid #eaecf0",
    overflow: "hidden",
    boxShadow: "0 4px 16px rgba(16, 24, 40, 0.05)",
  },
  tableHeader: {
    display: "grid",
    gridTemplateColumns: "1.2fr 1.4fr 0.9fr",
    backgroundColor: "#f9fafb",
    borderBottom: "1px solid #eaecf0",
  },
  headerCell: {
    padding: "16px 20px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#344054",
  },
  row: {
    display: "grid",
    gridTemplateColumns: "1.2fr 1.4fr 0.9fr",
    borderBottom: "1px solid #f2f4f7",
    alignItems: "stretch",
  },
  rowMissing: {
    backgroundColor: "#fffbeb",
  },
  rowDuplicate: {
    backgroundColor: "#fef3f2",
  },
  targetCell: {
    padding: "18px 20px",
  },
  fieldLabelRow: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    marginBottom: "6px",
    fontSize: "15px",
    fontWeight: 600,
    color: "#101828",
    flexWrap: "wrap",
  },
  requiredBadge: {
    padding: "3px 8px",
    borderRadius: "999px",
    backgroundColor: "#fee4e2",
    color: "#b42318",
    fontSize: "12px",
    fontWeight: 700,
  },
  fieldDescription: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.5,
    color: "#667085",
  },
  selectCell: {
    padding: "12px 20px",
  },
  select: {
    width: "100%",
    padding: "12px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    fontSize: "15px",
    backgroundColor: "#ffffff",
    color: "#101828",
  },
  input: {
    width: "100%",
    padding: "10px 12px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    fontSize: "14px",
    backgroundColor: "#ffffff",
    color: "#101828",
  },
  inputCompact: {
    width: "250px",
    minWidth: "120px",
    padding: "10px 12px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    fontSize: "14px",
    backgroundColor: "#ffffff",
    color: "#101828",
  },
  selectDuplicate: {
    borderColor: "#f04438",
  },
  inlineError: {
    margin: "8px 0 0",
    fontSize: "13px",
    color: "#b42318",
  },
  confidenceCell: {
    padding: "18px 20px",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    gap: "6px",
  },
  confidenceValue: {
    fontSize: "18px",
    fontWeight: 800,
  },
  confidenceStrong: {
    color: "#067647",
  },
  confidenceWeak: {
    color: "#a38824",
  },
  confidenceLow: {
    color: "#b42318",
  },
  matchType: {
    fontSize: "13px",
    color: "#667085",
  },
  manualLabel: {
    fontSize: "13px",
    fontWeight: 700,
    color: "#175cd3",
  },
  linkGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(2, minmax(0, 1fr))",
    gap: "20px",
  },
  field: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
    marginBottom: "14px",
  },
  label: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#101828",
  },
  helperText: {
    margin: "6px 0 0",
    fontSize: "13px",
    lineHeight: 1.5,
    color: "#667085",
  },
  formatCards: {
    display: "grid",
    gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
    gap: "16px",
    marginBottom: "20px",
  },
  formatCard: {
    textAlign: "left",
    padding: "18px",
    borderRadius: "14px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    cursor: "pointer",
  },
  formatCardActive: {
    border: "1px solid #175cd3",
    backgroundColor: "#eff8ff",
    boxShadow: "0 0 0 3px rgba(23, 92, 211, 0.08)",
  },
  formatCardTitle: {
    marginBottom: "8px",
    fontSize: "16px",
    fontWeight: 700,
    color: "#101828",
  },
  formatCardDesc: {
    fontSize: "14px",
    lineHeight: 1.5,
    color: "#667085",
  },
  conditionalBox: {
    padding: "20px",
    borderRadius: "14px",
    backgroundColor: "#f9fafb",
    border: "1px solid #eaecf0",
  },
  inlineValuesRow: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
    flexWrap: "wrap",
  },
  inlineInputRowCompact: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexShrink: 0,
  },
  inlineChipsRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
    alignItems: "center",
  },
  addButton: {
    padding: "10px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#101828",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  chipsRowCompact: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
    marginTop: "6px",
  },
  chip: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    padding: "6px 10px",
    borderRadius: "999px",
    backgroundColor: "#eef4ff",
    border: "1px solid #c7d7fe",
    color: "#3538cd",
    fontSize: "13px",
    fontWeight: 600,
  },
  chipRemoveButton: {
    border: "none",
    background: "transparent",
    color: "#3538cd",
    fontSize: "16px",
    lineHeight: 1,
    cursor: "pointer",
    padding: 0,
  },
  infoBox: {
    marginTop: "14px",
    padding: "14px 16px",
    borderRadius: "12px",
    backgroundColor: "#eff8ff",
    border: "1px solid #b2ddff",
  },
  infoTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#175cd3",
  },
  infoText: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#1849a9",
  },
  warningBox: {
    marginTop: "14px",
    padding: "14px 16px",
    borderRadius: "12px",
    backgroundColor: "#fffaeb",
    border: "1px solid #fedf89",
  },
  warningTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#b54708",
  },
  warningText: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#93370d",
  },
  dropdownContainer: {
    position: "relative",
  },
  dropdownButtonCompact: {
    width: "100%",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "12px",
    padding: "10px 12px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    cursor: "pointer",
    fontSize: "14px",
    color: "#101828",
  },
  dropdownValue: {
    color: "#101828",
  },
  dropdownPlaceholder: {
    color: "#667085",
  },
  dropdownArrow: {
    fontSize: "12px",
    color: "#667085",
  },
  dropdownMenu: {
    position: "absolute",
    zIndex: 20,
    top: "calc(100% + 8px)",
    left: 0,
    right: 0,
    maxHeight: "240px",
    overflowY: "auto",
    backgroundColor: "#ffffff",
    border: "1px solid #d0d5dd",
    borderRadius: "12px",
    boxShadow: "0 12px 24px rgba(16, 24, 40, 0.12)",
    padding: "8px",
  },
  dropdownEmpty: {
    padding: "10px 12px",
    fontSize: "14px",
    color: "#667085",
  },
  checkboxRow: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    padding: "10px 12px",
    borderRadius: "8px",
    cursor: "pointer",
    fontSize: "14px",
    color: "#101828",
  },
  actions: {
    display: "flex",
    justifyContent: "space-between",
    gap: "16px",
    flexWrap: "wrap",
    marginTop: "32px",
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
    opacity: 0.7,
    cursor: "not-allowed",
  },
};