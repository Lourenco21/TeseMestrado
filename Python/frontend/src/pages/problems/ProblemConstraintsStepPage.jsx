import { useEffect, useMemo, useState, useCallback, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useProblemWizard } from "../../contexts/ProblemWizardContext.jsx";
import { getProblemCatalog } from "../../services/problemsApi.js";

const IMPORTANCE_OPTIONS = [
    { value: "low", label: "Baixa" },
    { value: "medium", label: "Média" },
    { value: "high", label: "Alta" },
];

const GOAL_OPTIONS = [
    { value: "hard", label: "Obrigatória" },
    { value: "soft", label: "Preferencial" },
];

// Restrição que deve estar sempre selecionada e fixa como obrigatória (hard).
const FORCED_HARD_CONSTRAINT_ID = "room_exclusivity";

// Restrição que, quando selecionada, só pode assumir o valor preferencial (soft).
const FORCED_SOFT_CONSTRAINT_ID = "capacity_waste";

export default function ProblemConstraintsStepPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { problemDraft, loadDraft, saveDraft, loading, saving, error } =
        useProblemWizard();

    const [constraintLibrary, setConstraintLibrary] = useState({});
    const [selectedConstraints, setSelectedConstraints] = useState([]);
    const [localError, setLocalError] = useState("");
    const [catalogLoaded, setCatalogLoaded] = useState(false);
    const hasSyncedRef = useRef(false);

    const loadCatalog = useCallback(async () => {
        if (catalogLoaded) return;

        try {
            const catalog = await getProblemCatalog();
            setConstraintLibrary(catalog.constraint_library || {});
            setCatalogLoaded(true);
        } catch (err) {
            console.error("Erro ao carregar catálogo:", err);
            setLocalError("Não foi possível carregar o catálogo de restrições.");
        }
    }, [catalogLoaded]);

    const loadDraftOnce = useCallback(async () => {
        if (problemDraft) return;

        try {
            await loadDraft(id);
        } catch (err) {
            console.error("Erro ao carregar draft:", err);
            setLocalError("Não foi possível carregar o problema.");
        }
    }, [id, problemDraft, loadDraft]);

    useEffect(() => {
        hasSyncedRef.current = false;
    }, [problemDraft?.id]);

    useEffect(() => {
        if (!problemDraft || hasSyncedRef.current) return;

        const draftConstraints = problemDraft.selected_constraints || [];
        const normalizedConstraints = draftConstraints.map((item) => {
            let goal = item.goal || null;

            if (item.id === FORCED_HARD_CONSTRAINT_ID) {
                goal = "hard";
            } else if (item.id === FORCED_SOFT_CONSTRAINT_ID) {
                goal = "soft";
            }

            return {
                ...item,
                goal,
                importance: item.importance || null,
                enabled:
                    item.id === FORCED_HARD_CONSTRAINT_ID
                        ? true
                        : item.enabled !== false,
            };
        });

        setSelectedConstraints(normalizedConstraints);
        hasSyncedRef.current = true;
    }, [problemDraft]);

    useEffect(() => {
        let cancelled = false;

        async function init() {
            try {
                setLocalError("");
                await loadDraftOnce();

                if (!cancelled) {
                    await loadCatalog();
                }
            } catch (err) {
                if (!cancelled) {
                    setLocalError(err.message || "Erro ao inicializar página.");
                }
            }
        }

        init();

        return () => {
            cancelled = true;
        };
    }, [loadDraftOnce, loadCatalog]);

    const availableConstraints = useMemo(() => {
        const family = problemDraft?.problem_family;
        if (!family || !catalogLoaded) return [];
        return constraintLibrary[family] || [];
    }, [constraintLibrary, problemDraft?.problem_family, catalogLoaded]);

    // Garante que, sempre que a restrição room_exclusivity estiver disponível
    // para a família do problema, fica sempre selecionada e fixa como "hard".
    useEffect(() => {
        const hasForcedConstraint = availableConstraints.some(
            (constraint) => constraint.id === FORCED_HARD_CONSTRAINT_ID
        );

        if (!hasForcedConstraint) return;

        setSelectedConstraints((prev) => {
            const existing = prev.find(
                (item) => item.id === FORCED_HARD_CONSTRAINT_ID
            );

            if (existing) {
                if (existing.enabled && existing.goal === "hard") {
                    return prev;
                }

                return prev.map((item) =>
                    item.id === FORCED_HARD_CONSTRAINT_ID
                        ? { ...item, enabled: true, goal: "hard" }
                        : item
                );
            }

            return [
                ...prev,
                {
                    id: FORCED_HARD_CONSTRAINT_ID,
                    enabled: true,
                    goal: "hard",
                    importance: null,
                },
            ];
        });
    }, [availableConstraints]);

    const enabledConstraints = useMemo(() => {
        return selectedConstraints.filter((item) => item.enabled);
    }, [selectedConstraints]);

    const hardConstraintsCount = useMemo(() => {
        return enabledConstraints.filter((item) => item.goal === "hard").length;
    }, [enabledConstraints]);

    const softConstraintsCount = useMemo(() => {
        return enabledConstraints.filter((item) => item.goal === "soft").length;
    }, [enabledConstraints]);

    const missingSelectionsCount = useMemo(() => {
        return enabledConstraints.reduce((total, item) => {
            let missing = 0;

            if (!item.goal) missing += 1;
            if (item.goal === "soft" && !item.importance) missing += 1;

            return total + missing;
        }, 0);
    }, [enabledConstraints]);

    const toggleConstraint = useCallback((constraintId) => {
        if (constraintId === FORCED_HARD_CONSTRAINT_ID) return;

        setSelectedConstraints((prev) => {
            const exists = prev.some((item) => item.id === constraintId);

            if (exists) {
                return prev.filter((item) => item.id !== constraintId);
            }

            return [
                ...prev,
                {
                    id: constraintId,
                    enabled: true,
                    goal: constraintId === FORCED_SOFT_CONSTRAINT_ID ? "soft" : null,
                    importance: null,
                },
            ];
        });
    }, []);

    const updateGoal = useCallback((constraintId, goal) => {
        if (constraintId === FORCED_HARD_CONSTRAINT_ID) return;
        if (constraintId === FORCED_SOFT_CONSTRAINT_ID) return;

        setSelectedConstraints((prev) =>
            prev.map((item) =>
                item.id === constraintId ? { ...item, goal } : item
            )
        );
    }, []);

    const updateImportance = useCallback((constraintId, importance) => {
        setSelectedConstraints((prev) =>
            prev.map((item) =>
                item.id === constraintId ? { ...item, importance } : item
            )
        );
    }, []);

    const getImportanceHelpText = useCallback((goal) => {
        if (goal === "hard") {
            return "A importância é comparada com as restantes restrições obrigatórias.";
        }

        if (goal === "soft") {
            return "A importância é comparada com as restantes restrições preferenciais.";
        }

        return "Selecione primeiro o tipo da constraint.";
    }, []);

    const handleContinue = useCallback(async () => {
        const validConstraints = selectedConstraints.filter((item) => item.enabled);

        if (validConstraints.length === 0) {
            setLocalError("Selecione pelo menos uma restrição antes de continuar.");
            return;
        }

        const incompleteConstraints = validConstraints.filter(
            (item) => !item.goal || (item.goal === "soft" && !item.importance)
        );

        if (incompleteConstraints.length > 0) {
            setLocalError(
                "Preencha o tipo e a importância de todas as restrições selecionadas antes de continuar."
            );
            return;
        }

        const enrichedConstraints = validConstraints.map((item) => {
            const catalogConstraint = availableConstraints.find(
                (constraint) => constraint.id === item.id
            );

            return {
                ...item,
                label: catalogConstraint?.label || item.id,
            };
        });

        try {
            setLocalError("");

            await saveDraft({
                status: "constraints_selected",
                selected_constraints: enrichedConstraints,
                current_step: 7,
            });

            navigate(`/problems/${id}/detail`);
        } catch (err) {
            console.error("Erro ao guardar restrições:", err);
            setLocalError(err.message || "Não foi possível guardar as restrições.");
        }
    }, [selectedConstraints, availableConstraints, id, saveDraft, navigate]);

    const handleBack = useCallback(() => {
        navigate(`/problems/${id}/room-features`);
    }, [id, navigate]);

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
                <div style={styles.header}>
                    <p style={styles.step}>Passo 7 de 7</p>
                    <h1 style={styles.title}>Selecionar restrições</h1>
                    <p style={styles.description}>
                        Escolha as regras a aplicar ao problema, defina se cada uma é obrigatória
                        ou preferencial, e indique a sua importância relativa dentro desse tipo.
                    </p>
                </div>

                {problemDraft?.name ? (
                    <div style={styles.problemInfo}>
                        <span style={styles.problemLabel}>Problema atual:</span>
                        <span style={styles.problemName}>{problemDraft.name}</span>
                    </div>
                ) : null}

                <div style={styles.infoBox}>
                    <p style={styles.infoTitle}>Como interpretar esta configuração</p>
                    <p style={styles.infoText}>
                        <strong>Tipo</strong> indica se a restrição é obrigatória ou preferencial.
                        Restrições obrigatórias representam regras que devem ser satisfeitas,
                        enquanto restrições preferenciais representam regras desejáveis, mas podem
                        ser violadas em troca de uma solução global melhor.
                    </p>
                    <p style={styles.infoText}>
                        <strong>Importância relativa</strong> serve para comparar a prioridade de
                        uma restrição com outras do <strong>mesmo tipo</strong>. Uma restrição preferencial com
                        importância alta continua a ser preferencial.
                    </p>
                    <p style={styles.infoText}>
                        Apenas a restrição "Sala não pode ter duas aulas ao mesmo tempo" é sempre selecionada.
                        Quando selecionada, a restrição "Minimizar desperdício de capacidade" é obrigatoriamente definida como preferencial.
                        Para continuar, tem de completar o tipo
                        e a importância de todas as restrições que escolher.
                    </p>
                </div>

                {error ? <p style={styles.error}>{error}</p> : null}
                {localError ? <p style={styles.error}>{localError}</p> : null}

                {availableConstraints.length > 0 ? (
                    <>
                        <div style={styles.summaryBar}>
                            <span style={styles.summaryItem}>
                                Restrições selecionadas: {enabledConstraints.length}
                            </span>
                            <span style={styles.summaryItem}>
                                Obrigatórias: {hardConstraintsCount}
                            </span>
                            <span style={styles.summaryItem}>
                                Preferenciais: {softConstraintsCount}
                            </span>

                            {missingSelectionsCount > 0 ? (
                                <span style={styles.warningSummaryItem}>
                                    Opções em falta: {missingSelectionsCount}
                                </span>
                            ) : null}
                        </div>

                        <div style={styles.list}>
                            {availableConstraints.map((constraint) => {
                                const isForcedHard =
                                    constraint.id === FORCED_HARD_CONSTRAINT_ID;

                                const isForcedSoft =
                                    constraint.id === FORCED_SOFT_CONSTRAINT_ID;

                                const selected = selectedConstraints.some(
                                    (item) => item.id === constraint.id && item.enabled
                                );

                                const current = selectedConstraints.find(
                                    (item) => item.id === constraint.id
                                );

                                const isIncomplete =
                                    selected &&
                                    (!current?.goal ||
                                        (current?.goal === "soft" && !current?.importance));

                                return (
                                    <div
                                        key={constraint.id}
                                        style={{
                                            ...styles.listItem,
                                            ...(selected ? styles.listItemActive : {}),
                                            ...(isIncomplete ? styles.listItemWarning : {}),
                                        }}
                                    >
                                        <div style={styles.constraintHeader}>
                                            <label style={styles.checkboxLabel}>
                                                <input
                                                    type="checkbox"
                                                    checked={selected}
                                                    disabled={isForcedHard}
                                                    onChange={() => toggleConstraint(constraint.id)}
                                                    style={styles.checkbox}
                                                />

                                                <div style={styles.constraintContent}>
                                                    <div style={styles.constraintTitleRow}>
                                                        <h2 style={styles.constraintTitle}>
                                                            {constraint.label}
                                                        </h2>

                                                        <span
                                                            style={{
                                                                ...styles.statusBadge,
                                                                ...(selected
                                                                    ? styles.statusBadgeActive
                                                                    : styles.statusBadgeInactive),
                                                            }}
                                                        >
                                                            {selected ? "Selecionada" : "Não selecionada"}
                                                        </span>

                                                        {isIncomplete ? (
                                                            <span style={styles.inlineWarningBadge}>
                                                                Configuração incompleta
                                                            </span>
                                                        ) : null}
                                                    </div>

                                                    <p style={styles.constraintDescription}>
                                                        {constraint.description || "Sem descrição disponível."}
                                                    </p>
                                                </div>
                                            </label>
                                        </div>

                                        {selected ? (
                                            <>
                                                <div style={styles.controlsRow}>
                                                    <div style={styles.controlsGroup}>
                                                        <span style={styles.compactLabel}>Tipo</span>
                                                        <div style={styles.segmentedControl}>
                                                            {GOAL_OPTIONS.map((option) => {
                                                                const active =
                                                                    current?.goal === option.value;

                                                                return (
                                                                    <button
                                                                        key={option.value}
                                                                        type="button"
                                                                        disabled={isForcedHard || isForcedSoft}
                                                                        onClick={() =>
                                                                            updateGoal(
                                                                                constraint.id,
                                                                                option.value
                                                                            )
                                                                        }
                                                                        style={{
                                                                            ...styles.segmentButton,
                                                                            ...(active
                                                                                ? styles.segmentButtonActive
                                                                                : {}),
                                                                        }}
                                                                    >
                                                                        {option.label}
                                                                    </button>
                                                                );
                                                            })}
                                                        </div>
                                                    </div>

                                                    {current?.goal === "soft" ? (
                                                        <div style={styles.controlsGroup}>
                                                            <span style={styles.compactLabel}>Importância</span>
                                                            <div style={styles.segmentedControl}>
                                                                {IMPORTANCE_OPTIONS.map((option) => {
                                                                    const active =
                                                                        current?.importance === option.value;

                                                                    return (
                                                                        <button
                                                                            key={option.value}
                                                                            type="button"
                                                                            onClick={() =>
                                                                                updateImportance(
                                                                                    constraint.id,
                                                                                    option.value
                                                                                )
                                                                            }
                                                                            style={{
                                                                                ...styles.segmentButton,
                                                                                ...(active
                                                                                    ? styles.segmentButtonActive
                                                                                    : {}),
                                                                            }}
                                                                        >
                                                                            {option.label}
                                                                        </button>
                                                                    );
                                                                })}
                                                            </div>
                                                        </div>
                                                    ) : null}
                                                    {current?.goal === "soft" ? (
                                                        <div style={styles.helperBlock}>
                                                            <span style={styles.helperBadge}>Nota</span>
                                                            <p style={styles.compactHelperText}>
                                                                {getImportanceHelpText(current?.goal)}
                                                            </p>
                                                        </div>
                                                    ) : null}
                                                </div>

                                                {isIncomplete ? (
                                                    <p style={styles.validationHint}>
                                                        Faltam selecionar{" "}
                                                        {(!current?.goal &&
                                                            current?.goal !== "soft")
                                                            ? "2 opções"
                                                            : "1 opção"}{" "}
                                                        nesta restrição.
                                                    </p>
                                                ) : null}
                                            </>
                                        ) : null}
                                    </div>
                                );
                            })}
                        </div>
                    </>
                ) : (
                    <div style={styles.emptyState}>
                        <p style={styles.message}>
                            {!catalogLoaded
                                ? "A carregar catálogo..."
                                : "Não existem restrições disponíveis para esta família."}
                        </p>
                    </div>
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
                        disabled={saving || loading}
                        style={{
                            ...styles.primaryButton,
                            ...(saving || loading ? styles.primaryButtonDisabled : {}),
                        }}
                    >
                        {saving ? "A guardar..." : "Continuar"}
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
        maxWidth: "1120px",
        margin: "0 auto",
    },
    header: {
        marginBottom: "24px",
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
        marginBottom: "20px",
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
    infoBox: {
        marginBottom: "20px",
        padding: "16px 18px",
        borderRadius: "14px",
        border: "1px solid #dbeafe",
        backgroundColor: "#eff6ff",
    },
    infoTitle: {
        margin: 0,
        marginBottom: "8px",
        fontSize: "15px",
        fontWeight: 700,
        color: "#1d4ed8",
    },
    infoText: {
        margin: 0,
        marginTop: "8px",
        fontSize: "14px",
        lineHeight: 1.6,
        color: "#1e3a8a",
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
    summaryBar: {
        display: "flex",
        gap: "16px",
        flexWrap: "wrap",
        marginBottom: "16px",
    },
    summaryItem: {
        padding: "10px 14px",
        borderRadius: "999px",
        backgroundColor: "#f2f4f7",
        color: "#344054",
        fontSize: "14px",
        fontWeight: 600,
    },
    warningSummaryItem: {
        padding: "10px 14px",
        borderRadius: "999px",
        backgroundColor: "#fff4e5",
        color: "#b54708",
        fontSize: "14px",
        fontWeight: 700,
        border: "1px solid #fecdca",
    },
    list: {
        display: "flex",
        flexDirection: "column",
        gap: "14px",
        marginTop: "20px",
    },
    listItem: {
        border: "1px solid #eaecf0",
        borderRadius: "16px",
        backgroundColor: "#ffffff",
        padding: "16px 18px",
        boxShadow: "0 4px 16px rgba(16, 24, 40, 0.04)",
    },
    listItemActive: {
        borderColor: "#175cd3",
        backgroundColor: "#f8fbff",
    },
    listItemWarning: {
        borderColor: "#f59e0b",
    },
    constraintHeader: {
        width: "100%",
    },
    checkboxLabel: {
        display: "flex",
        alignItems: "flex-start",
        gap: "12px",
        cursor: "pointer",
    },
    checkbox: {
        marginTop: "4px",
        width: "16px",
        height: "16px",
        accentColor: "#175cd3",
        cursor: "pointer",
    },
    constraintContent: {
        display: "flex",
        flexDirection: "column",
        gap: "6px",
        flex: 1,
        minWidth: 0,
    },
    constraintTitleRow: {
        display: "flex",
        alignItems: "center",
        gap: "10px",
        flexWrap: "wrap",
    },
    constraintTitle: {
        margin: 0,
        fontSize: "18px",
        fontWeight: 700,
        color: "#101828",
    },
    constraintDescription: {
        margin: 0,
        fontSize: "14px",
        lineHeight: 1.6,
        color: "#667085",
        maxWidth: "100%",
    },
    statusBadge: {
        display: "inline-flex",
        alignItems: "center",
        padding: "4px 10px",
        borderRadius: "999px",
        fontSize: "12px",
        fontWeight: 700,
    },
    statusBadgeActive: {
        backgroundColor: "#dbeafe",
        color: "#1d4ed8",
    },
    statusBadgeInactive: {
        backgroundColor: "#f2f4f7",
        color: "#667085",
    },
    inlineWarningBadge: {
        display: "inline-flex",
        alignItems: "center",
        padding: "4px 10px",
        borderRadius: "999px",
        backgroundColor: "#fff4e5",
        color: "#b54708",
        fontSize: "12px",
        fontWeight: 700,
        border: "1px solid #fecdca",
    },
    controlsRow: {
        display: "flex",
        alignItems: "flex-end",
        gap: "25px",
        flexWrap: "wrap",
        marginTop: "14px",
        marginLeft: "28px",
        paddingTop: "14px",
        borderTop: "1px solid #e4e7ec",
    },
    controlsGroup: {
        display: "flex",
        flexDirection: "column",
        gap: "6px",
    },
    compactLabel: {
        fontSize: "12px",
        fontWeight: 700,
        textTransform: "uppercase",
        letterSpacing: "0.04em",
        color: "#475467",
    },
    segmentedControl: {
        display: "flex",
        flexWrap: "wrap",
        gap: "8px",
    },
    segmentButton: {
        padding: "8px 12px",
        borderRadius: "999px",
        borderWidth: "1px",
        borderStyle: "solid",
        borderColor: "#d0d5dd",
        backgroundColor: "#ffffff",
        color: "#344054",
        fontSize: "13px",
        fontWeight: 600,
        cursor: "pointer",
    },
    segmentButtonActive: {
        borderColor: "#175cd3",
        backgroundColor: "#dbeafe",
        color: "#1d4ed8",
    },
    helperBlock: {
        display: "flex",
        alignItems: "center",
        gap: "8px",
        minHeight: "38px",
        paddingBottom: "2px",
    },
    helperBadge: {
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "4px 8px",
        borderRadius: "999px",
        backgroundColor: "#eef2ff",
        color: "#4338ca",
        fontSize: "12px",
        fontWeight: 700,
    },
    compactHelperText: {
        margin: 0,
        fontSize: "13px",
        lineHeight: 1.5,
        color: "#667085",
        maxWidth: "400px",
    },
    validationHint: {
        margin: 0,
        marginTop: "10px",
        marginLeft: "28px",
        fontSize: "13px",
        fontWeight: 600,
        color: "#b42318",
    },
    emptyState: {
        padding: "24px",
        border: "1px dashed #d0d5dd",
        borderRadius: "16px",
        backgroundColor: "#ffffff",
        marginTop: "20px",
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