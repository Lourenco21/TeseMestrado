import { useEffect, useMemo, useRef, useState } from "react";

export default function SearchableSelect({
  label,
  value,
  onChange,
  options,
  placeholder = "Pesquisar...",
  noResultsText = "Sem resultados",
  style = {},
  inputId,
}) {
  const wrapperRef = useRef(null);
  const listboxId = `${inputId || "searchable-select"}-listbox`;

  const [query, setQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);

  const selectedOption = useMemo(() => {
    return options.find((option) => option.id === value) || null;
  }, [options, value]);

  useEffect(() => {
    if (!isOpen) {
      setQuery(selectedOption?.label || "");
    }
  }, [selectedOption, isOpen]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setIsOpen(false);
        setQuery(selectedOption?.label || "");
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [selectedOption]);

  const filteredOptions = useMemo(() => {
    const normalizedQuery = normalizeText(query);

    if (!normalizedQuery) {
      return options;
    }

    return options.filter((option) =>
      normalizeText(option.searchText || option.label).includes(normalizedQuery)
    );
  }, [options, query]);

  return (
    <div style={{ ...searchableStyles.wrapper, ...style }} ref={wrapperRef}>
      {label ? (
        <label htmlFor={inputId} style={searchableStyles.label}>
          {label}
        </label>
      ) : null}

      <input
        id={inputId}
        type="text"
        value={query}
        placeholder={placeholder}
        autoComplete="off"
        onFocus={() => setIsOpen(true)}
        onClick={() => setIsOpen(true)}
        onChange={(e) => {
          setQuery(e.target.value);
          setIsOpen(true);
        }}
        style={searchableStyles.input}
        role="combobox"
        aria-expanded={isOpen}
        aria-controls={listboxId}
        aria-autocomplete="list"
      />

      {isOpen ? (
        <div style={searchableStyles.dropdown} role="listbox" id={listboxId}>
          {filteredOptions.length > 0 ? (
            filteredOptions.map((option) => (
              <button
                key={option.id}
                type="button"
                role="option"
                aria-selected={value === option.id}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(option.id);
                  setQuery(option.label);
                  setIsOpen(false);
                }}
                style={{
                  ...searchableStyles.option,
                  ...(value === option.id ? searchableStyles.optionActive : {}),
                }}
              >
                {option.label}
              </button>
            ))
          ) : (
            <div style={searchableStyles.empty}>{noResultsText}</div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();
}

const searchableStyles = {
  wrapper: {
    position: "relative",
    minWidth: "260px",
  },
  label: {
    display: "block",
    marginBottom: "8px",
    fontSize: "14px",
    fontWeight: 600,
    color: "#475467",
  },
  input: {
    width: "100%",
    height: "42px",
    padding: "0 12px",
    border: "1px solid #cbd5e1",
    borderRadius: "8px",
    background: "#ffffff",
    color: "#111827",
    fontSize: "14px",
    outline: "none",
    boxSizing: "border-box",
  },
  dropdown: {
    position: "absolute",
    top: "calc(100% + 6px)",
    left: 0,
    right: 0,
    maxHeight: "240px",
    overflowY: "auto",
    background: "#ffffff",
    border: "1px solid #d0d5dd",
    borderRadius: "10px",
    boxShadow: "0 10px 24px rgba(15, 23, 42, 0.10)",
    zIndex: 100,
    padding: "6px",
  },
  option: {
    display: "block",
    width: "100%",
    textAlign: "left",
    padding: "10px 12px",
    border: "none",
    background: "transparent",
    borderRadius: "8px",
    fontSize: "14px",
    color: "#111827",
    cursor: "pointer",
  },
  optionActive: {
    background: "#f3f4f6",
    fontWeight: 600,
  },
  empty: {
    padding: "10px 12px",
    fontSize: "14px",
    color: "#6b7280",
  },
};