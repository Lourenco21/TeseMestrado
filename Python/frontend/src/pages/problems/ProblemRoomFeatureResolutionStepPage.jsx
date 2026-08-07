import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext";
import { getProblemRoomFeatureResolutionAnalysis } from "../../services/problemsApi";

function MultiCheckboxDropdown({
  label,
  options,
  selectedValues,
  onChange,
  placeholder = "Selecionar opções",
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
              ? `${selectedValues.length} opção(ões) selecionada(s)`
              : placeholder}
          </span>
          <span style={styles.dropdownArrow}>{open ? "▲" : "▼"}</span>
        </button>

        {open ? (
          <div style={styles.dropdownMenu}>
            {options.length === 0 ? (
              <div style={styles.dropdownEmpty}>Sem opções disponíveis</div>
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

function normalizeResolutionItems(items) {
  return (items || []).map((item) => ({
    source_value: item.source_value || "",
    source_value_normalized: item.source_value_normalized || "",
    resolution_type: item.resolution_type || "unresolved",
    target_values: item.target_values || [],
    suggested_target_values: item.suggested_target_values || [],
  }));
}

export default function ProblemRoomFeatureResolutionStepPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { problemDraft, saveDraft, loadDraft, loading, error } = useProblemWizard();

  const [requestedValues, setRequestedValues] = useState([]);
  const [availableRoomFeatures, setAvailableRoomFeatures] = useState([]);
  const [pageLoading, setPageLoading] = useState(true);
  const [localSaving, setLocalSaving] = useState(false);
  const [localError, setLocalError] = useState("");

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
        const result = await getProblemRoomFeatureResolutionAnalysis(id);

        if (cancelled) return;

        const existingResolution =
          draft?.room_feature_resolution?.requested_values || [];
        const existingBySource = Object.fromEntries(
          existingResolution.map((item) => [item.source_value, item])
        );

        const mergedRequestedValues = normalizeResolutionItems(
          (result.requested_values || []).map((item) => {
            const existing = existingBySource[item.source_value];
            if (!existing) return item;

            return {
              ...item,
              resolution_type:
                existing.resolution_type || item.resolution_type || "unresolved",
              target_values: existing.target_values || item.target_values || [],
            };
          })
        );

        setRequestedValues(mergedRequestedValues);
        setAvailableRoomFeatures(result.available_room_features || []);
      } catch (err) {
        if (!cancelled) {
          console.error("Erro ao carregar resolução de características:", err);
          setLocalError(
            err.message ||
              "Não foi possível carregar a resolução de características."
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

  const summary = useMemo(() => {
    const total = requestedValues.length;
    const resolved = requestedValues.filter(
      (item) =>
        item.resolution_type === "map_to_room_feature" &&
        (item.target_values || []).length > 0
    ).length;
    const noneRequired = requestedValues.filter(
      (item) => item.resolution_type === "none_required"
    ).length;
    const noMatch = requestedValues.filter(
      (item) => item.resolution_type === "no_match"
    ).length;
    const unresolved = requestedValues.filter((item) => {
      if (item.resolution_type === "none_required") return false;
      if (item.resolution_type === "no_match") return false;
      if (
        item.resolution_type === "map_to_room_feature" &&
        (item.target_values || []).length > 0
      ) {
        return false;
      }
      return true;
    }).length;

    return {
      total,
      resolved,
      noneRequired,
      noMatch,
      unresolved,
    };
  }, [requestedValues]);

  function updateRequestedValue(index, changes) {
    setRequestedValues((prev) =>
      prev.map((item, currentIndex) =>
        currentIndex === index ? { ...item, ...changes } : item
      )
    );
  }

  function handleResolutionTypeChange(index, resolutionType) {
    const currentItem = requestedValues[index];

    if (resolutionType === "map_to_room_feature") {
      updateRequestedValue(index, {
        resolution_type: resolutionType,
        target_values:
          currentItem.target_values?.length > 0
            ? currentItem.target_values
            : currentItem.suggested_target_values || [],
      });
      return;
    }

    if (resolutionType === "none_required") {
      updateRequestedValue(index, {
        resolution_type: resolutionType,
        target_values: [],
      });
      return;
    }

    if (resolutionType === "no_match") {
      updateRequestedValue(index, {
        resolution_type: resolutionType,
        target_values: [],
      });
      return;
    }

    updateRequestedValue(index, {
      resolution_type: "unresolved",
      target_values: [],
    });
  }

  function handleTargetValuesChange(index, values) {
    updateRequestedValue(index, {
      target_values: values,
      resolution_type: values.length > 0 ? "map_to_room_feature" : "unresolved",
    });
  }

  function applySuggestions() {
    setRequestedValues((prev) =>
      prev.map((item) => {
        if (
          item.resolution_type === "map_to_room_feature" &&
          item.target_values?.length > 0
        ) {
          return item;
        }

        if (item.suggested_target_values?.length > 0) {
          return {
            ...item,
            resolution_type: "map_to_room_feature",
            target_values: item.suggested_target_values,
          };
        }

        return item;
      })
    );
  }

  async function handleContinue() {
    const invalidItems = requestedValues.filter((item) => {
      if (item.resolution_type === "none_required") return false;
      if (item.resolution_type === "no_match") return false;
      if (
        item.resolution_type === "map_to_room_feature" &&
        (item.target_values || []).length > 0
      ) {
        return false;
      }
      return true;
    });

    if (invalidItems.length > 0) {
      setLocalError(
        "Resolve todas as características antes de continuar. Pode mapear para características da sala, indicar que não é necessária ou marcar como sem correspondência."
      );
      return;
    }

    try {
      setLocalSaving(true);
      setLocalError("");

      await saveDraft({
        room_feature_resolution: {
          requested_values: requestedValues.map((item) => ({
            source_value: item.source_value,
            resolution_type: item.resolution_type,
            target_values: item.target_values || [],
          })),
        },
        current_step: 6,
      });

      navigate(`/problems/${id}/constraints`);
    } catch (err) {
      console.error("Erro ao guardar resolução de características:", err);
      setLocalError(
        err.message ||
          "Não foi possível guardar a resolução das características."
      );
    } finally {
      setLocalSaving(false);
    }
  }

  function handleBack() {
    navigate(`/problems/${id}/rooms-mapping`);
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
        <p style={styles.step}>Passo 6 de 7</p>
        <h1 style={styles.title}>Resolução de características pedidas</h1>
        <p style={styles.description}>
          Associe cada característica pedida nas aulas, que não existe no ficheiro de salas, às características
          disponíveis no ficheiro de salas. Uma característica pedida pode
          corresponder a várias opções de sala.
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
          <p style={styles.message}>A carregar análise das características...</p>
        ) : (
          <>
            <div style={styles.summaryBar}>
              <span style={styles.summaryItem}>
                Pedidos totais: {summary.total}
              </span>
              <span style={styles.summaryItem}>
                Resolvidas: {summary.resolved}
              </span>
              <span style={styles.summaryItem}>
                Sem requisito: {summary.noneRequired}
              </span>
              <span style={styles.summaryItem}>
                Sem correspondência: {summary.noMatch}
              </span>
              <span style={styles.summaryItem}>
                Por resolver: {summary.unresolved}
              </span>
            </div>

            <div style={styles.sectionCard}>
              <div style={styles.sectionHeaderRow}>
                <div>
                  <h2 style={styles.sectionTitle}>Características pedidas</h2>
                  <p style={styles.sectionDescription}>
                    Para cada valor pedido no horário, escolhe como deve ser
                    interpretado no contexto das características das salas.
                  </p>
                </div>


              </div>

              {requestedValues.length === 0 ? (
                <div style={styles.infoBox}>
                  <p style={styles.infoTitle}>Sem características pedidas</p>
                  <p style={styles.infoText}>
                    Não foram encontradas características pedidas no ficheiro de
                    horário, ou a coluna correspondente ainda não foi mapeada.
                  </p>
                </div>
              ) : (
                <div style={styles.rowsWrapper}>
                  {requestedValues.map((item, index) => {
                    const isResolved =
                      item.resolution_type === "none_required" ||
                      item.resolution_type === "no_match" ||
                      (item.resolution_type === "map_to_room_feature" &&
                        (item.target_values || []).length > 0);

                    return (
                      <div
                        key={`${item.source_value}-${index}`}
                        style={{
                          ...styles.mappingRowCard,
                          ...(isResolved
                            ? styles.mappingRowResolved
                            : styles.mappingRowPending),
                        }}
                      >
                        <div style={styles.mappingRowTop}>
                          <div style={styles.mappingRowHeader}>
                            <div style={styles.mappingValueRow}>
                              <span style={styles.mappingValueLabel}>
                                {item.source_value}
                              </span>

                              {isResolved ? (
                                <span style={styles.successBadge}>Resolvido</span>
                              ) : (
                                <span style={styles.warningBadge}>
                                  Por resolver
                                </span>
                              )}
                            </div>

                            {item.suggested_target_values?.length > 0 ? (
                              <p style={styles.fieldDescription}>
                                Sugestões automáticas:{" "}
                                {item.suggested_target_values.join(", ")}
                              </p>
                            ) : (
                              <p style={styles.fieldDescription}>
                                Não foram encontradas sugestões automáticas para
                                este valor.
                              </p>
                            )}
                          </div>
                        </div>

                        <div style={styles.inlineControlsColumn}>
                          <div style={styles.resolutionTypeRow}>
                            <label style={styles.radioOption}>
                              <input
                                type="radio"
                                name={`resolution-${index}`}
                                checked={
                                  item.resolution_type === "map_to_room_feature"
                                }
                                onChange={() =>
                                  handleResolutionTypeChange(
                                    index,
                                    "map_to_room_feature"
                                  )
                                }
                              />
                              <span>Corresponde a características da sala</span>
                            </label>

                            <label style={styles.radioOption}>
                              <input
                                type="radio"
                                name={`resolution-${index}`}
                                checked={
                                  item.resolution_type === "none_required"
                                }
                                onChange={() =>
                                  handleResolutionTypeChange(
                                    index,
                                    "none_required"
                                  )
                                }
                              />
                              <span>Não atribuir sala</span>
                            </label>

                            <label style={styles.radioOption}>
                              <input
                                type="radio"
                                name={`resolution-${index}`}
                                checked={item.resolution_type === "no_match"}
                                onChange={() =>
                                  handleResolutionTypeChange(index, "no_match")
                                }
                              />
                              <span>Sem correspondência</span>
                            </label>
                          </div>

                          {item.resolution_type === "map_to_room_feature" ? (
                            <MultiCheckboxDropdown
                              label="Características de sala compatíveis"
                              options={availableRoomFeatures}
                              selectedValues={item.target_values || []}
                              onChange={(values) =>
                                handleTargetValuesChange(index, values)
                              }
                              placeholder="Selecionar características"
                            />
                          ) : null}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
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
            disabled={pageLoading || localSaving}
            style={{
              ...styles.primaryButton,
              ...(pageLoading || localSaving
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
    maxWidth: "820px",
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
  sectionHeaderRow: {
    display: "flex",
    justifyContent: "space-between",
    gap: "16px",
    alignItems: "flex-start",
    marginBottom: "18px",
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
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#667085",
  },
  rowsWrapper: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  mappingRowCard: {
    border: "1px solid #eaecf0",
    borderRadius: "14px",
    padding: "18px",
  },
  mappingRowResolved: {
    backgroundColor: "#f9fefb",
    borderColor: "#ccebd8",
  },
  mappingRowPending: {
    backgroundColor: "#fffdf7",
    borderColor: "#f4e0a6",
  },
  mappingRowTop: {
    marginBottom: "14px",
  },
  mappingRowHeader: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
  },
  mappingValueRow: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    flexWrap: "wrap",
  },
  mappingValueLabel: {
    fontSize: "16px",
    fontWeight: 700,
    color: "#101828",
  },
  fieldDescription: {
    margin: 0,
    fontSize: "13px",
    lineHeight: 1.5,
    color: "#667085",
  },
  successBadge: {
    padding: "4px 9px",
    borderRadius: "999px",
    backgroundColor: "#dcfae6",
    color: "#067647",
    fontSize: "12px",
    fontWeight: 700,
  },
  warningBadge: {
    padding: "4px 9px",
    borderRadius: "999px",
    backgroundColor: "#fef0c7",
    color: "#b54708",
    fontSize: "12px",
    fontWeight: 700,
  },
  inlineControlsColumn: {
    display: "flex",
    flexDirection: "column",
    gap: "14px",
  },
  resolutionTypeRow: {
    display: "flex",
    gap: "20px",
    flexWrap: "wrap",
  },
  radioOption: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    fontSize: "14px",
    color: "#344054",
  },
  field: {
    display: "flex",
    flexDirection: "column",
    gap: "8px",
  },
  label: {
    fontSize: "14px",
    fontWeight: 600,
    color: "#101828",
  },
  dropdownContainer: {
    position: "relative",
  },
  dropdownButtonCompact: {
    width: "100%",
    minHeight: "44px",
    padding: "10px 12px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "12px",
  },
  dropdownValue: {
    color: "#101828",
    fontSize: "14px",
  },
  dropdownPlaceholder: {
    color: "#667085",
    fontSize: "14px",
  },
  dropdownArrow: {
    color: "#667085",
    fontSize: "12px",
  },
  dropdownMenu: {
    position: "absolute",
    top: "calc(100% + 6px)",
    left: 0,
    right: 0,
    zIndex: 20,
    backgroundColor: "#ffffff",
    border: "1px solid #d0d5dd",
    borderRadius: "12px",
    boxShadow: "0 12px 24px rgba(16, 24, 40, 0.12)",
    maxHeight: "260px",
    overflowY: "auto",
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
    padding: "8px 10px",
    borderRadius: "8px",
    fontSize: "14px",
    color: "#344054",
  },
  chipsRowCompact: {
    display: "flex",
    flexWrap: "wrap",
    gap: "8px",
    marginTop: "10px",
  },
  inlineChipsRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "10px",
  },
  chip: {
    display: "inline-flex",
    alignItems: "center",
    gap: "8px",
    padding: "7px 10px",
    borderRadius: "999px",
    backgroundColor: "#eff6ff",
    border: "1px solid #bfdbfe",
    color: "#1d4ed8",
    fontSize: "13px",
    fontWeight: 600,
  },
  neutralChip: {
    display: "inline-flex",
    alignItems: "center",
    padding: "8px 12px",
    borderRadius: "999px",
    backgroundColor: "#f2f4f7",
    color: "#344054",
    fontSize: "13px",
    fontWeight: 600,
  },
  chipRemoveButton: {
    border: "none",
    background: "transparent",
    color: "#1d4ed8",
    cursor: "pointer",
    fontSize: "16px",
    lineHeight: 1,
    padding: 0,
  },
  infoBox: {
    borderRadius: "12px",
    border: "1px solid #dbeafe",
    backgroundColor: "#eff6ff",
    padding: "16px",
  },
  infoTitle: {
    margin: 0,
    marginBottom: "6px",
    fontSize: "14px",
    fontWeight: 700,
    color: "#1d4ed8",
  },
  infoText: {
    margin: 0,
    fontSize: "14px",
    lineHeight: 1.6,
    color: "#1e40af",
  },
  actions: {
    display: "flex",
    justifyContent: "space-between",
    gap: "12px",
    marginTop: "8px",
  },
  secondaryButton: {
    padding: "12px 18px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#344054",
    fontSize: "15px",
    fontWeight: 600,
    cursor: "pointer",
  },
  secondaryButtonSmall: {
    padding: "10px 14px",
    borderRadius: "10px",
    border: "1px solid #d0d5dd",
    backgroundColor: "#ffffff",
    color: "#344054",
    fontSize: "14px",
    fontWeight: 600,
    cursor: "pointer",
    whiteSpace: "nowrap",
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
    opacity: 0.6,
    cursor: "not-allowed",
  },
};